package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.SettingEntity
import com.example.data.model.AiModel
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ActiveScreen {
    CHAT, MODELS, SETTINGS
}

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    // Active Screen state
    private val _currentScreen = MutableStateFlow(ActiveScreen.CHAT)
    val currentScreen: StateFlow<ActiveScreen> = _currentScreen.asStateFlow()

    // Key Validation flows (Empty, Validating..., Valid, or Error Message)
    private val _nvidiaValidationStatus = MutableStateFlow("")
    val nvidiaValidationStatus: StateFlow<String> = _nvidiaValidationStatus.asStateFlow()

    private val _openRouterValidationStatus = MutableStateFlow("")
    val openRouterValidationStatus: StateFlow<String> = _openRouterValidationStatus.asStateFlow()

    private val _geminiValidationStatus = MutableStateFlow("")
    val geminiValidationStatus: StateFlow<String> = _geminiValidationStatus.asStateFlow()

    // Query filters for Models List screen
    private val _modelsSearchQuery = MutableStateFlow("")
    val modelsSearchQuery: StateFlow<String> = _modelsSearchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    // Local Chat state
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Session and settings streams
    val sessions: StateFlow<List<ChatSessionEntity>> = repository.sessionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<SettingEntity> = repository.settingsFlow
        .map { it ?: SettingEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingEntity()
        )

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(emptyList())
            } else {
                repository.getMessagesFlow(id)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Exposed dynamic list of filtered Models
    val filteredModels: StateFlow<List<AiModel>> = MutableStateFlow(AiModel.FREE_MODELS)
        .flatMapLatest { models ->
            modelsSearchQuery.flatMapLatest { query ->
                selectedCategoryFilter.map { category ->
                    models.filter { model ->
                        val matchesSearch = model.name.contains(query, ignoreCase = true) ||
                                model.id.contains(query, ignoreCase = true) ||
                                model.description.contains(query, ignoreCase = true)
                        
                        val matchesCategory = category == "All" || model.computedCategory.equals(category, ignoreCase = true)
                        
                        matchesSearch && matchesCategory
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AiModel.FREE_MODELS
        )

    init {
        // Create an initial session if none exists or select the first session from DB
        viewModelScope.launch {
            val currentSettings = repository.getSettings()
            // Just verify table is seeded
        }
    }

    fun setScreen(screen: ActiveScreen) {
        _currentScreen.value = screen
    }

    fun setModelsSearchQuery(query: String) {
        _modelsSearchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun selectSession(sessionId: Long?) {
        _currentSessionId.value = if (sessionId == 0L || sessionId == null) null else sessionId
    }

    fun createSession(title: String) {
        viewModelScope.launch {
            val parsedTitle = title.trim().ifBlank { "New Intelligence thread" }
            val currentModel = settings.value.currentModelId
            val newId = repository.createNewSession(parsedTitle, currentModel)
            _currentSessionId.value = newId
            _currentScreen.value = ActiveScreen.CHAT
        }
    }

    fun createSessionForModel(title: String, modelId: String) {
        viewModelScope.launch {
            val parsedTitle = title.trim().ifBlank { "New Intelligence thread" }
            val newId = repository.createNewSession(parsedTitle, modelId)
            _currentSessionId.value = newId
            _currentScreen.value = ActiveScreen.CHAT
        }
    }

    fun deleteSession(session: ChatSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (_currentSessionId.value == session.id) {
                _currentSessionId.value = sessions.value.firstOrNull { it.id != session.id }?.id
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.clearAllSessions()
            _currentSessionId.value = null
        }
    }

    fun clearSessionMessages(sessionId: Long) {
        viewModelScope.launch {
            repository.clearSessionMessages(sessionId)
        }
    }

    fun selectModel(model: AiModel) {
        viewModelScope.launch {
            val current = settings.value
            val updated = current.copy(currentModelId = model.id)
            repository.saveSettings(updated)
        }
    }

    fun saveApiKeys(nvidia: String, openRouter: String, gemini: String) {
        viewModelScope.launch {
            val current = settings.value
            val updated = current.copy(
                nvidiaKey = nvidia.trim(),
                openRouterKey = openRouter.trim(),
                geminiKey = gemini.trim()
            )
            repository.saveSettings(updated)
        }
    }

    fun validateAndSaveNvidiaKey(key: String) {
        val trimmedKey = key.trim()
        if (trimmedKey.isBlank()) {
            _nvidiaValidationStatus.value = "Failed: Key string cannot be blank."
            return
        }
        _nvidiaValidationStatus.value = "Checking..."
        viewModelScope.launch {
            val res = repository.validateApiKey("NVIDIA", trimmedKey)
            if (res.isSuccess) {
                _nvidiaValidationStatus.value = "✔ Verified & Saved Locally"
                val current = repository.getSettings()
                val updated = current.copy(nvidiaKey = trimmedKey)
                repository.saveSettings(updated)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Validation failed"
                _nvidiaValidationStatus.value = "❌ $err"
            }
        }
    }

    fun validateAndSaveOpenRouterKey(key: String) {
        val trimmedKey = key.trim()
        if (trimmedKey.isBlank()) {
            _openRouterValidationStatus.value = "Failed: Key string cannot be blank."
            return
        }
        _openRouterValidationStatus.value = "Checking..."
        viewModelScope.launch {
            val res = repository.validateApiKey("OpenRouter", trimmedKey)
            if (res.isSuccess) {
                _openRouterValidationStatus.value = "✔ Verified & Saved Locally"
                val current = repository.getSettings()
                val updated = current.copy(openRouterKey = trimmedKey)
                repository.saveSettings(updated)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Validation failed"
                _openRouterValidationStatus.value = "❌ $err"
            }
        }
    }

    fun validateAndSaveGeminiKey(key: String) {
        val trimmedKey = key.trim()
        if (trimmedKey.isBlank()) {
            _geminiValidationStatus.value = "Failed: Key string cannot be blank."
            return
        }
        _geminiValidationStatus.value = "Checking..."
        viewModelScope.launch {
            val res = repository.validateApiKey("Google", trimmedKey)
            if (res.isSuccess) {
                _geminiValidationStatus.value = "✔ Verified & Saved Locally"
                val current = repository.getSettings()
                val updated = current.copy(geminiKey = trimmedKey)
                repository.saveSettings(updated)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Validation failed"
                _geminiValidationStatus.value = "❌ $err"
            }
        }
    }

    fun clearNvidiaKey() {
        _nvidiaValidationStatus.value = ""
        viewModelScope.launch {
            val current = repository.getSettings()
            val updated = current.copy(nvidiaKey = "")
            repository.saveSettings(updated)
        }
    }

    fun clearOpenRouterKey() {
        _openRouterValidationStatus.value = ""
        viewModelScope.launch {
            val current = repository.getSettings()
            val updated = current.copy(openRouterKey = "")
            repository.saveSettings(updated)
        }
    }

    fun clearGeminiKey() {
        _geminiValidationStatus.value = ""
        viewModelScope.launch {
            val current = repository.getSettings()
            val updated = current.copy(geminiKey = "")
            repository.saveSettings(updated)
        }
    }

    fun saveSystemPrompt(prompt: String) {
        viewModelScope.launch {
            val current = settings.value
            val updated = current.copy(customSystemPrompt = prompt.trim())
            repository.saveSettings(updated)
        }
    }

    fun togglePlatformPreference(platform: String) {
        if (platform == "NVIDIA" || platform == "OpenRouter") {
            viewModelScope.launch {
                val current = settings.value
                val updated = current.copy(currentPlatform = platform)
                repository.saveSettings(updated)
            }
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun sendMessage() {
        val prompt = _inputText.value.trim()
        if (prompt.isBlank() || _isGenerating.value) return

        val activeSessionId = _currentSessionId.value
        val modelList = AiModel.FREE_MODELS
        val activeModelId = settings.value.currentModelId
        val activeModel = modelList.find { it.id == activeModelId } ?: modelList.first()

        viewModelScope.launch {
            var targetSessionId = activeSessionId
            if (targetSessionId == null) {
                val shortTitle = if (prompt.length > 25) prompt.take(25) + "..." else prompt
                targetSessionId = repository.createNewSession(shortTitle, activeModelId)
                _currentSessionId.value = targetSessionId
            }

            // Save user message
            repository.insertUserMessage(targetSessionId, prompt)
            setInputText("")
            _isGenerating.value = true

            // Send to endpoint
            repository.sendChatMessage(
                sessionId = targetSessionId,
                model = activeModel,
                settings = settings.value
            )

            _isGenerating.value = false
        }
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

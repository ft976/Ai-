package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.ApiClient
import com.example.data.api.ApiMessage
import com.example.data.api.ChatCompletionRequest
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.SettingEntity
import com.example.data.local.SettingsDao
import com.example.data.local.EncryptionUtils
import com.example.data.model.AiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

class ChatRepository(
    private val chatDao: ChatDao,
    private val settingsDao: SettingsDao
) {
    val sessionsFlow: Flow<List<ChatSessionEntity>> = chatDao.getSessionsFlow()
    
    val settingsFlow: Flow<SettingEntity?> = settingsDao.getSettingsFlow().map { entity ->
        entity?.let {
            it.copy(
                nvidiaKey = EncryptionUtils.decrypt(it.nvidiaKey),
                openRouterKey = EncryptionUtils.decrypt(it.openRouterKey),
                geminiKey = EncryptionUtils.decrypt(it.geminiKey)
            )
        }
    }

    fun getMessagesFlow(sessionId: Long): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForSessionFlow(sessionId)

    suspend fun getSettings(): SettingEntity {
        val dbSettings = settingsDao.getSettings() ?: SettingEntity().also {
            settingsDao.insertSettings(it)
        }
        return dbSettings.copy(
            nvidiaKey = EncryptionUtils.decrypt(dbSettings.nvidiaKey),
            openRouterKey = EncryptionUtils.decrypt(dbSettings.openRouterKey),
            geminiKey = EncryptionUtils.decrypt(dbSettings.geminiKey)
        )
    }

    suspend fun saveSettings(settings: SettingEntity) {
        val encrypted = settings.copy(
            nvidiaKey = EncryptionUtils.encrypt(settings.nvidiaKey),
            openRouterKey = EncryptionUtils.encrypt(settings.openRouterKey),
            geminiKey = EncryptionUtils.encrypt(settings.geminiKey)
        )
        settingsDao.insertSettings(encrypted)
    }

    suspend fun createNewSession(title: String, modelId: String): Long {
        return chatDao.insertSession(ChatSessionEntity(title = title, modelId = modelId))
    }

    suspend fun getSessionById(sessionId: Long): ChatSessionEntity? {
        return chatDao.getSessionById(sessionId)
    }

    suspend fun deleteSession(session: ChatSessionEntity) {
        chatDao.deleteSessionMessages(session.id)
        chatDao.deleteSession(session)
    }

    suspend fun clearSessionMessages(sessionId: Long) {
        chatDao.deleteSessionMessages(sessionId)
    }

    suspend fun clearAllSessions() {
        chatDao.deleteAllMessages()
        chatDao.deleteAllSessions()
    }

    suspend fun getEffectiveKeys(settings: SettingEntity): Triple<String, String, String> {
        val envNvidia = try { BuildConfig.NVIDIA_API_KEY } catch (e: Exception) { "" }
        val envOpenRouter = try { BuildConfig.OPENROUTER_API_KEY } catch (e: Exception) { "" }
        val envGemini = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        val nvidiaKey = if (settings.nvidiaKey.isNotBlank()) settings.nvidiaKey else envNvidia
        val openRouterKey = if (settings.openRouterKey.isNotBlank()) settings.openRouterKey else envOpenRouter
        val geminiKey = if (settings.geminiKey.isNotBlank()) settings.geminiKey else envGemini

        return Triple(
            if (isKeyValid(nvidiaKey, "MY_NVIDIA_API_KEY")) nvidiaKey else "",
            if (isKeyValid(openRouterKey, "MY_OPENROUTER_API_KEY")) openRouterKey else "",
            if (isKeyValid(geminiKey, "MY_GEMINI_API_KEY")) geminiKey else ""
        )
    }

    private fun isKeyValid(key: String, placeholder: String): Boolean {
        return key.isNotBlank() && 
               key != placeholder && 
               !key.startsWith("YOUR_") && 
               !key.startsWith("MY_")
    }

    suspend fun insertUserMessage(sessionId: Long, content: String) {
        chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = content
            )
        )
    }

    suspend fun deleteMessage(messageId: Long) {
        chatDao.deleteMessage(messageId)
    }

    suspend fun validateApiKey(platform: String, key: String): Result<Boolean> {
        val trimmedKey = key.trim()
        if (trimmedKey.isBlank() || trimmedKey.startsWith("YOUR_") || trimmedKey.startsWith("MY_")) {
            return Result.failure(Exception("API Key cannot be blank or contain placeholder strings."))
        }
        
        // Enforce prefix conventions
        if (platform == "NVIDIA") {
            if (!trimmedKey.startsWith("nvapi-")) {
                return Result.failure(Exception("NVIDIA api keys must start with the 'nvapi-' prefix."))
            }
            if (trimmedKey.length < 20) {
                return Result.failure(Exception("Invalid key length: NVIDIA api key is too short."))
            }
        } else if (platform == "OpenRouter") {
            if (!trimmedKey.startsWith("sk-or-")) {
                return Result.failure(Exception("OpenRouter keys must start with the 'sk-or-' prefix."))
            }
            if (trimmedKey.length < 20) {
                return Result.failure(Exception("Invalid key length: OpenRouter api key is too short."))
            }
        } else if (platform == "Google") {
            // Google Gemini Keys often start with AIza
            if (trimmedKey.length < 20) {
                return Result.failure(Exception("Invalid key length: Google API key is too short."))
            }
        }

        val url = if (platform == "NVIDIA") {
            "https://integrate.api.nvidia.com/v1/chat/completions"
        } else if (platform == "Google") {
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
        } else {
            "https://openrouter.ai/api/v1/chat/completions"
        }
        
        val testModel = if (platform == "NVIDIA") {
            "nvidia/nemotron-mini-4b-instruct"
        } else if (platform == "Google") {
            "gemini-3.5-flash"
        } else {
            "google/gemini-2.5-flash:free"
        }
        
        val authHeader = "Bearer $trimmedKey"
        val request = ChatCompletionRequest(
            model = testModel,
            messages = listOf(ApiMessage(role = "user", content = "ping")),
            max_tokens = 1,
            temperature = 0.0
        )
        
        return try {
            val response = ApiClient.service.getChatCompletion(url, authHeader, request)
            // If it succeeds and returns without throwing, the key is active and working!
            Result.success(true)
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            Log.e("ChatRepository", "Validation HTTP error $code: $errorBody")
            if (code == 401) {
                Result.failure(Exception("Unauthorized: The provided $platform API key is incorrect or expired."))
            } else if (code == 403) {
                Result.failure(Exception("Forbidden: Access denied. Check account billing/permissions for $platform."))
            } else if (code == 400 && errorBody.contains("invalid", ignoreCase = true)) {
                Result.failure(Exception("Invalid Key: Request rejected by $platform server."))
            } else {
                Result.failure(Exception("Invalid key status reported by $platform (HTTP $code)."))
            }
        } catch (e: java.io.IOException) {
            // Network connection errors
            Log.e("ChatRepository", "Validation network error: ${e.message}")
            Result.failure(Exception("Connection failed: Please check your active internet connection."))
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Unknown key status verification issue."
            if (msg.contains("401") || msg.contains("Unauthorized")) {
                Result.failure(Exception("Unauthorized: The provided $platform API key is incorrect or expired."))
            } else {
                Result.failure(Exception("Rejected: $msg"))
            }
        }
    }

    suspend fun sendChatMessage(
        sessionId: Long,
        model: AiModel,
        settings: SettingEntity
    ): Result<String> {
        val session = chatDao.getSessionById(sessionId)
        val effectiveModel = if (session != null) {
            AiModel.FREE_MODELS.find { it.id == session.modelId } ?: model
        } else {
            model
        }
        val messages = chatDao.getMessagesForSession(sessionId)
        if (messages.isEmpty()) {
            return Result.failure(Exception("No messages to send"))
        }

        // Map messages to com.example.data.api.ApiMessage (excluding error messages)
        val contextMessages = messages.filter { !it.isError }.map { msg ->
            com.example.data.api.ApiMessage(role = msg.role, content = msg.content)
        }

        // Retrieve effective decrypted/env key for NVIDIA NIM, OpenRouter, Gemini
        val (nvidiaKey, openRouterKey, geminiKey) = getEffectiveKeys(settings)

        // Delegate to Unified AI Gateway Service
        val gatewayResult = ApiClient.gatewayService.routeAndExecute(
            messages = contextMessages,
            systemPrompt = null, // Global system directive removed
            selectedModel = effectiveModel,
            nvidiaKey = nvidiaKey,
            openRouterKey = openRouterKey,
            geminiKey = geminiKey,
            preferredPlatform = settings.currentPlatform
        )

        return when (gatewayResult) {
            is com.example.data.api.GatewayResult.Success -> {
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = gatewayResult.text,
                        modelUsed = gatewayResult.mappedModelId,
                        platformUsed = gatewayResult.platform
                    )
                )
                Result.success(gatewayResult.text)
            }
            is com.example.data.api.GatewayResult.Error -> {
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = "Gateway error: ${gatewayResult.message}",
                        isError = true,
                        modelUsed = effectiveModel.id,
                        platformUsed = gatewayResult.platform ?: "Unknown"
                    )
                )
                Result.failure(Exception(gatewayResult.message))
            }
        }
    }
}

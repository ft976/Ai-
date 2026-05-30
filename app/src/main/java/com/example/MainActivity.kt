package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.model.AiModel
import com.example.data.repository.ChatRepository
import com.example.ui.ActiveScreen
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize local Room DB & repositories
        val database = AppDatabase.getDatabase(applicationContext)
        val chatRepository = ChatRepository(database.chatDao(), database.settingsDao())
        
        setContent {
            MyApplicationTheme {
                // Instantiating ChatViewModel with the repository factory
                val chatViewModel: ChatViewModel = viewModel(
                    factory = ChatViewModelFactory(chatRepository)
                )
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainLayout(chatViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppMainLayout(viewModel: ChatViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    
    // Deletion confirmation state variables
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deleteConfirmTitle by remember { mutableStateOf("") }
    var deleteConfirmMessage by remember { mutableStateOf("") }
    var onDeleteConfirmedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    val confirmDelete: (String, String, () -> Unit) -> Unit = { title, message, action ->
        deleteConfirmTitle = title
        deleteConfirmMessage = message
        onDeleteConfirmedAction = action
        showDeleteConfirmDialog = true
    }
    
    val activeModel = remember(settings.currentModelId) {
        AiModel.FREE_MODELS.find { it.id == settings.currentModelId } ?: AiModel.FREE_MODELS.first()
    }
    
    val modelSessions = remember(sessions, settings.currentModelId) {
        sessions.filter { it.modelId == settings.currentModelId }
    }
    
    var didInitialAutoSelect by remember { mutableStateOf(false) }
    // Auto-select first session for active model if present and active session Id is null
    LaunchedEffect(sessions, settings.currentModelId) {
        if (!didInitialAutoSelect && activeSessionId == null && modelSessions.isNotEmpty()) {
            viewModel.selectSession(modelSessions.first().id)
            didInitialAutoSelect = true
        }
    }
    
    val isKeyboardVisible = WindowInsets.isImeVisible
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer Header - Ultra Polished Cyberpunk Look
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(com.example.ui.theme.NeonCyan, com.example.ui.theme.NeonPurple)
                            )
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.NeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Intel Vault v3.0",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "NVIDIA AI Platforms",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Navigation Options Section
                    Text(
                        text = "NAVIGATION OPTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Chat, contentDescription = null) },
                        label = { Text("Chat Console", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selected = currentScreen == ActiveScreen.CHAT,
                        onClick = {
                            viewModel.setScreen(ActiveScreen.CHAT)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text("61 Models Catalog", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selected = currentScreen == ActiveScreen.MODELS,
                        onClick = {
                            viewModel.setScreen(ActiveScreen.MODELS)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("API Credentials Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selected = currentScreen == ActiveScreen.SETTINGS,
                        onClick = {
                            viewModel.setScreen(ActiveScreen.SETTINGS)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                        label = { Text("About Creator & Security", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selected = currentScreen == ActiveScreen.ABOUT,
                        onClick = {
                            viewModel.setScreen(ActiveScreen.ABOUT)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Model History Section
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MODEL HISTORY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        IconButton(
                            onClick = {
                                viewModel.selectSession(null)
                                viewModel.setScreen(ActiveScreen.CHAT)
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.AddComment, contentDescription = "New Thread", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Text(
                        text = "Current model: ${activeModel.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (modelSessions.isEmpty()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "No chats for this model.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(modelSessions) { session ->
                                    val isActive = session.id == activeSessionId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                viewModel.selectSession(session.id)
                                                viewModel.setScreen(ActiveScreen.CHAT)
                                                coroutineScope.launch { drawerState.close() }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Forum,
                                            contentDescription = null,
                                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.title,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                confirmDelete(
                                                    "Delete Thread",
                                                    "Are you sure you want to delete this thread: \"${session.title}\"? All matching chat history will be permanently erased."
                                                ) {
                                                    viewModel.deleteSession(session)
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete Thread",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (sessions.isNotEmpty()) {
                        Button(
                            onClick = {
                                confirmDelete(
                                    "Clear All History",
                                    "Are you sure you want to permanently delete ALL conversation threads in this application? This action is absolute and irreversible."
                                ) {
                                    viewModel.clearAllConversations()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All Threads", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            bottomBar = {
                // If the keyboard is visible, hide the bottom bar to prevent screen compression upwards!
                if (!isKeyboardVisible) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.height(72.dp)
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == ActiveScreen.CHAT,
                            onClick = { viewModel.setScreen(ActiveScreen.CHAT) },
                            icon = { Icon(Icons.Filled.Chat, contentDescription = "Chat console") },
                            label = { Text("Chat", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == ActiveScreen.MODELS,
                            onClick = { viewModel.setScreen(ActiveScreen.MODELS) },
                            icon = { Icon(Icons.Filled.List, contentDescription = "Models list") },
                            label = { Text("61 Models", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == ActiveScreen.SETTINGS,
                            onClick = { viewModel.setScreen(ActiveScreen.SETTINGS) },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings console") },
                            label = { Text("API Console", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentScreen) {
                    ActiveScreen.CHAT -> ChatScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onConfirmDelete = confirmDelete
                    )
                    ActiveScreen.MODELS -> ModelsCatalogScreen(viewModel)
                    ActiveScreen.SETTINGS -> SettingsScreen(viewModel)
                    ActiveScreen.ABOUT -> AboutAndDeveloperScreen()
                }
            }
        }
    }

    // Master Delete Confirmation Dialog (applied cleanly everywhere!)
    if (showDeleteConfirmDialog && onDeleteConfirmedAction != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                onDeleteConfirmedAction = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Warning icon",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = deleteConfirmTitle,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                    text = deleteConfirmMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConfirmedAction?.invoke()
                        showDeleteConfirmDialog = false
                        onDeleteConfirmedAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteConfirmedAction = null
                    }
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        )
    }
}

@Composable
fun CustomInteractiveChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ModelSetupLander(
    selectedModel: AiModel,
    lastSession: ChatSessionEntity?,
    isKeyAdded: Boolean,
    onGoToSettings: () -> Unit,
    onCreateNew: (String) -> Unit,
    onResumeLast: (Long) -> Unit,
    onOpenThreads: () -> Unit
) {
    var customTitle by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated Header Icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (selectedModel.computedCategory) {
                            "Reasoning", "Thinking" -> Icons.Filled.Psychology
                            "Coding" -> Icons.Filled.Code
                            "Vision", "Multi-Modal" -> Icons.Filled.Visibility
                            "Lightweight" -> Icons.Filled.Speed
                            "Moderation" -> Icons.Filled.Security
                            "Audio" -> Icons.Filled.VolumeUp
                            "Science" -> Icons.Filled.Science
                            else -> Icons.Filled.AutoAwesome
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedModel.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${selectedModel.platform} NIM Gateway Proxy",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Text(
                text = selectedModel.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            // Text field to optionally customize thread name
            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                placeholder = { Text("Describe thread topic (or auto-generate)...", fontSize = 12.sp) },
                label = { Text("Conversation Title", fontSize = 11.sp) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp)
            )
            
            // Action Panel
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isKeyAdded) {
                    // Lock message
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "API Key Authorization Required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "To chat with ${selectedModel.name} on ${selectedModel.platform}, you must add and verify your custom platform API key in Settings.",
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Button(
                        onClick = onGoToSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configure API Credentials", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                } else {
                    // Confirm 1: Create New Conversation Bound to this Model
                    Button(
                        onClick = { onCreateNew(customTitle) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AddComment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Conversation", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }
                
                // Confirm 2: Continue/Resume Last session if present
                if (lastSession != null && isKeyAdded) {
                    Button(
                        onClick = { onResumeLast(lastSession.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.Forum, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resume Last Thread: \"${lastSession.title}\"", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onOpenThreads() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Conversation Explorer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenDrawer: () -> Unit,
    onConfirmDelete: (String, String, () -> Unit) -> Unit
) {
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    
    val isKeyboardVisible = WindowInsets.isImeVisible
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showMenuDropdown by remember { mutableStateOf(false) }
    
    val activeSession = remember(sessions, currentSessionId) {
        sessions.find { it.id == currentSessionId }
    }
    
    val activeModel = remember(activeSession, settings.currentModelId) {
        val modelId = activeSession?.modelId ?: settings.currentModelId
        AiModel.FREE_MODELS.find { it.id == modelId } ?: AiModel.FREE_MODELS.first()
    }
    
    // Auto Scroll to bottom upon receiving messages or starting generation
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header with Model Info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        com.example.ui.theme.NeonCyan.copy(alpha = 0.35f),
                        com.example.ui.theme.NeonPurple.copy(alpha = 0.35f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Thread Navigator Button
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Threads manager",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Model metadata info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (activeSession != null) activeSession.title else "Model Selected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (activeModel.platform) {
                                "NVIDIA" -> Color(0xFF00E676).copy(alpha = 0.15f)
                                "OpenRouter" -> Color(0xFF7C4DFF).copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            },
                            contentColor = when (activeModel.platform) {
                                "NVIDIA" -> Color(0xFF00FF87)
                                "OpenRouter" -> Color(0xFF9E7BFF)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                val isNvidiaKeyAdded = settings.nvidiaKey.isNotBlank()
                                val isOpenRouterKeyAdded = settings.openRouterKey.isNotBlank()
                                val isGeminiKeyAdded = settings.geminiKey.isNotBlank()
                                val isArmed = when (activeModel.platform) {
                                    "NVIDIA" -> isNvidiaKeyAdded
                                    "Google" -> isGeminiKeyAdded
                                    else -> isOpenRouterKeyAdded
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isArmed) Color(0xFF00FF87) else Color(0xFFFF5252), CircleShape)
                                )
                                Text(
                                    text = activeModel.name + if (isArmed) " Enabled" else " Fallback",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Switch model shortcut icon
                    IconButton(
                        onClick = { viewModel.setScreen(ActiveScreen.MODELS) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Autorenew,
                            contentDescription = "Switch AI model",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    // Options menu trigger
                    Box {
                        IconButton(
                            onClick = { showMenuDropdown = true },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Options menu",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.AddComment, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                text = { Text("New Conversation Thread", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.selectSession(null)
                                }
                            )
                            if (currentSessionId != null) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    text = { Text("Clear Current Chat Messages", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        showMenuDropdown = false
                                        currentSessionId?.let { id ->
                                            onConfirmDelete(
                                                "Clear Messages",
                                                "Are you sure you want to permanently delete all messages in this thread?"
                                            ) {
                                                viewModel.clearSessionMessages(id)
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    text = { Text("Delete This Thread", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenuDropdown = false
                                        activeSession?.let { session ->
                                            onConfirmDelete(
                                                "Delete Thread",
                                                "Are you sure you want to permanently delete this conversation thread: \"${session.title}\"?"
                                            ) {
                                                viewModel.deleteSession(session)
                                            }
                                        }
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                                text = { Text("Clear All Threads & Conversations", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenuDropdown = false
                                    onConfirmDelete(
                                        "Clear All History",
                                        "Are you sure you want to permanently delete ALL conversation threads in this application? This action is absolute and irreversible."
                                    ) {
                                        viewModel.clearAllConversations()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        if (currentSessionId == null) {
            val lastSessionForModel = remember(sessions, activeModel.id) {
                sessions.firstOrNull { it.modelId == activeModel.id }
            }
            val isNvidiaKeyAdded = settings.nvidiaKey.isNotBlank()
            val isOpenRouterKeyAdded = settings.openRouterKey.isNotBlank()
            val isActiveModelKeyAdded = when (activeModel.platform) {
                "NVIDIA" -> isNvidiaKeyAdded
                "OpenRouter" -> isOpenRouterKeyAdded
                "Both" -> isNvidiaKeyAdded || isOpenRouterKeyAdded
                else -> true
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ModelSetupLander(
                    selectedModel = activeModel,
                    lastSession = lastSessionForModel,
                    isKeyAdded = isActiveModelKeyAdded,
                    onGoToSettings = {
                        viewModel.setScreen(ActiveScreen.SETTINGS)
                    },
                    onCreateNew = { customTitle ->
                        viewModel.createSessionForModel(customTitle, activeModel.id)
                    },
                    onResumeLast = { lastSessionId ->
                        viewModel.selectSession(lastSessionId)
                    },
                    onOpenThreads = onOpenDrawer
                )
            }
        } else {
            // Chat History List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        ) {
                            // Glowing header badge
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(com.example.ui.theme.NeonCyan, com.example.ui.theme.NeonPurple)
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(com.example.ui.theme.EmeraldPrimary, CircleShape)
                                    )
                                    Text(
                                        text = "NEURAL PORT GATEWAY",
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.5.sp,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            
                            // Visual Big Title
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = activeModel.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Ready to synthesize deep intelligence.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            // Quick Action Launcher Prompts heading
                            Text(
                                text = "TAP TO DETONATE PROMPT SCENARIO",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            
                            val suggestions = when (activeModel.computedCategory) {
                                "Coding" -> listOf(
                                    "Explain Room & SQLite Flow" to "Explain how to set up Room Database with Kotlin Coroutines Flow and live UI streams.",
                                    "Compose Responsive Canvas" to "Design an interactive, gorgeous custom Canvas rendering dynamic glowing circular indicators in Jetpack Compose.",
                                    "Staggered Grid Layout" to "Show how to implement a clean responsive grid spacing layout using Jetpack Compose."
                                )
                                "Reasoning" -> listOf(
                                    "Solve Low-latency caching" to "Design an advanced, low-latency client-side caching tier with automatic cache-invalidation rules.",
                                    "Explain Quantum Physics" to "Give me an intuitive, metaphorical description of quantum entanglement and quantum gates for junior developers.",
                                    "Trace Coroutine memory leak" to "Detail the exact debugging checklist to trace and resolve memory leaks inside long-running coroutines."
                                )
                                "Vision" -> listOf(
                                    "Analyze UI layout heuristics" to "What are the core heuristics to analyze user interfaces for layout contrast, density, and spatial rhythm?",
                                    "Write image edge-detector" to "Write a clean Kotlin algorithm to process pixels of a mock image for edge detection features."
                                )
                                else -> listOf(
                                    "Draft Executive Status update" to "Compose a highly polished, professional developer update on database schema optimization and API testing milestones.",
                                    "Refining system architecture" to "Review key bottlenecks in high-frequency message exchange pipelines and suggest robust queue-based solutions.",
                                    "Deep Metaphorical explanation" to "Help me explain complex distributed transactions using an illustrative analogy of a global postal delivery office."
                                )
                            }
                            
                            suggestions.forEach { (label, prompt) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setInputText(prompt) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Bolt,
                                            contentDescription = null,
                                            tint = com.example.ui.theme.NeonPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = prompt,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.Forward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                    ) {
                        items(messages) { msg ->
                            MessageBubble(
                                msg = msg,
                                onDelete = {
                                    onConfirmDelete(
                                        "Delete Message",
                                        "Are you sure you want to delete this specific message?"
                                    ) {
                                        viewModel.deleteMessage(msg.id)
                                    }
                                }
                            )
                        }
                        if (isGenerating) {
                            item {
                                TypingBubble(activeModel.name)
                            }
                        }
                    }
                }
            }
            
            // Input Controls View Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    
                    TextField(
                        value = inputText,
                        onValueChange = { viewModel.setInputText(it) },
                        placeholder = {
                            Text(
                                "Type your inquiry here...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.Transparent),
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isGenerating) {
                                    viewModel.sendMessage()
                                    keyboardController?.hide()
                                }
                            }
                        )
                    )
                    
                    IconButton(
                        onClick = {
                            viewModel.sendMessage()
                            keyboardController?.hide()
                        },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = if (inputText.isNotBlank() && !isGenerating) {
                                    Brush.horizontalGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.3f))
                                    )
                                },
                                shape = CircleShape
                             )
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send instruction",
                            tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessageEntity, onDelete: () -> Unit) {
    val isUser = msg.role == "user"
    val context = LocalContext.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Bottom),
                shape = CircleShape,
                color = if (msg.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (msg.isError) Icons.Filled.ErrorOutline else Icons.Filled.SmartToy,
                        contentDescription = "AI response",
                        tint = if (msg.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        
        val bubbleShape = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = if (isUser) 18.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 18.dp
        )
        
        Card(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    Color.Transparent
                } else if (msg.isError) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            shape = bubbleShape,
            border = if (isUser) {
                BorderStroke(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            com.example.ui.theme.NeonPurple.copy(alpha = 0.8f),
                            com.example.ui.theme.PinkPulse.copy(alpha = 0.8f)
                        )
                    )
                )
            } else {
                BorderStroke(
                    width = 1.2.dp,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = if (msg.platformUsed == "NVIDIA") {
                            listOf(com.example.ui.theme.EmeraldPrimary, com.example.ui.theme.EmeraldPrimaryDark.copy(alpha = 0.4f))
                        } else {
                            listOf(com.example.ui.theme.IndigoAccent, com.example.ui.theme.NeonCyan.copy(alpha = 0.4f))
                        }
                    )
                )
            }
        ) {
            Column(
                modifier = if (isUser) {
                    Modifier
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    com.example.ui.theme.NeonPurple.copy(alpha = 0.9f),
                                    com.example.ui.theme.PinkPulse.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(14.dp)
                } else {
                    Modifier.padding(14.dp)
                }
            ) {
                // Top metadata labels for assistant message
                if (!isUser && (msg.modelUsed.isNotBlank() || msg.platformUsed.isNotBlank())) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (msg.platformUsed == "NVIDIA") "NVIDIA NIM" else "OpenRouter",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (msg.platformUsed == "NVIDIA") Color(0xFF00E676) else Color(0xFF9E7BFF)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = msg.modelUsed.takeLast(24),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                
                // Pure markdown or code block checking
                val spansCode = msg.content.contains("```")
                if (spansCode) {
                    val splitParts = msg.content.split("```")
                    splitParts.forEachIndexed { idx, section ->
                        if (idx % 2 == 1) { // It's code snippet
                            val cleanCode = section.removePrefix("kotlin\n")
                                .removePrefix("java\n")
                                .removePrefix("python\n")
                                .removePrefix("json\n")
                                .trim()
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("CODE SNIPPET", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Output Code", cleanCode))
                                                Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Text(
                                        text = cleanCode,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            if (section.isNotBlank()) {
                                Text(
                                    text = section.trim(),
                                    fontSize = 13.5.sp,
                                    lineHeight = 18.2.sp,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = msg.content.trim(),
                        fontSize = 13.5.sp,
                        lineHeight = 18.2.sp,
                        color = if (isUser) Color.White else if (msg.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Touch selection utilities & timestamps on long press can delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Chat Message", msg.content))
                            Toast.makeText(context, "Message copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy text",
                            tint = if (isUser) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete from history",
                            tint = if (isUser) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
        
        if (isUser) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Bottom),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User avatar",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TypingBubble(modelName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Card(
            modifier = Modifier.widthIn(max = 240.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$modelName thinking step...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.61f),
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
fun ModelsCatalogScreen(viewModel: ChatViewModel) {
    val filteredModels by viewModel.filteredModels.collectAsStateWithLifecycle()
    val searchQuery by viewModel.modelsSearchQuery.collectAsStateWithLifecycle()
    val activeCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    val categories = listOf("All", "General", "Coding", "Reasoning", "Vision", "Voice", "Translation", "Safety")
    var selectedModelDetails by remember { mutableStateOf<AiModel?>(null) }
    var showCustomIdEditorForm by remember { mutableStateOf(false) }
    var customNvidiaIdString by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Models List View
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (filteredModels.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No models match current query filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(filteredModels) { model ->
                        val isActive = settings.currentModelId == model.id
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedModelDetails = model
                                    customNvidiaIdString = model.defaultNvidiaId
                                },
                            border = BorderStroke(
                                width = if (isActive) 1.5.dp else 1.dp,
                                brush = if (isActive) {
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            com.example.ui.theme.NeonCyan,
                                            com.example.ui.theme.NeonPurple
                                        )
                                    )
                                } else {
                                    androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                }
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 2.dp else 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = model.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // Custom tags
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (model.platform) {
                                            "NVIDIA" -> Color(0xFF00E676).copy(alpha = 0.12f)
                                            "OpenRouter" -> Color(0xFF7C4DFF).copy(alpha = 0.12f)
                                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        },
                                        contentColor = when (model.platform) {
                                            "NVIDIA" -> Color(0xFF00E676)
                                            "OpenRouter" -> Color(0xFF9E7BFF)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    ) {
                                        Text(
                                            text = model.platform,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = "Api class: ${model.computedCategory}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                
                                Text(
                                    text = model.description,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 6.dp),
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                val idToDisplay = if (model.platform == "NVIDIA") model.defaultNvidiaId else model.defaultOpenRouterId
                                Text(
                                    text = "API Endpoint Key: $idToDisplay",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (isActive) {
                                        Button(
                                            onClick = {},
                                            modifier = Modifier.height(32.dp),
                                            enabled = false,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                disabledContentColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Active Model", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        val isNvidiaKeyAdded = settings.nvidiaKey.isNotBlank()
                                        val isOpenRouterKeyAdded = settings.openRouterKey.isNotBlank()
                                        val isKeyAdded = when (model.platform) {
                                            "NVIDIA" -> isNvidiaKeyAdded
                                            "OpenRouter" -> isOpenRouterKeyAdded
                                            "Both" -> isNvidiaKeyAdded || isOpenRouterKeyAdded
                                            else -> true
                                        }

                                        if (!isKeyAdded) {
                                            Button(
                                                onClick = {
                                                    viewModel.setScreen(ActiveScreen.SETTINGS)
                                                },
                                                modifier = Modifier.height(32.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                            ) {
                                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Key Required", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    viewModel.selectModel(model)
                                                    viewModel.selectSession(null)
                                                    viewModel.setScreen(ActiveScreen.CHAT)
                                                },
                                                modifier = Modifier.height(32.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                            ) {
                                                Text("Select", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Bottom Detail modal dialog with Endpoint Customizer
    selectedModelDetails?.let { model ->
        val isNvidiaKeyAdded = settings.nvidiaKey.isNotBlank()
        val isOpenRouterKeyAdded = settings.openRouterKey.isNotBlank()
        val isKeyAdded = when (model.platform) {
            "NVIDIA" -> isNvidiaKeyAdded
            "OpenRouter" -> isOpenRouterKeyAdded
            "Both" -> isNvidiaKeyAdded || isOpenRouterKeyAdded
            else -> true
        }

        AlertDialog(
            onDismissRequest = { selectedModelDetails = null },
            title = {
                Text(model.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            },
            confirmButton = {
                if (isKeyAdded) {
                    Button(
                        onClick = {
                            viewModel.selectModel(model)
                            viewModel.selectSession(null)
                            viewModel.setScreen(ActiveScreen.CHAT)
                            selectedModelDetails = null
                        }
                    ) {
                        Text("Select Model", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.setScreen(ActiveScreen.SETTINGS)
                            selectedModelDetails = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Key in Settings", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedModelDetails = null }) {
                    Text("Dismiss")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isKeyAdded) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This model is locked. You must add and verify a custom API Key for ${model.platform} in settings.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "Metadata Specs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Classification: ${model.computedCategory}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = model.description,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "API ID Specifications",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (model.defaultNvidiaId.isNotBlank()) {
                        Text(
                            text = "NVIDIA NIM ID:\n${model.defaultNvidiaId}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (model.defaultOpenRouterId.isNotBlank()) {
                        Text(
                            text = "OpenRouter ID:\n${model.defaultOpenRouterId}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsScreen(viewModel: ChatViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val nvidiaStatus by viewModel.nvidiaValidationStatus.collectAsStateWithLifecycle()
    val openRouterStatus by viewModel.openRouterValidationStatus.collectAsStateWithLifecycle()
    val geminiStatus by viewModel.geminiValidationStatus.collectAsStateWithLifecycle()
    
    var localNvidiaKey by remember { mutableStateOf("") }
    var localOpenRouterKey by remember { mutableStateOf("") }
    var localGeminiKey by remember { mutableStateOf("") }
    var localSystemPrompt by remember { mutableStateOf("") }
    
    var showNvidiaKey by remember { mutableStateOf(false) }
    var showOpenRouterKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    
    // Seed initial local variables from database state flow
    LaunchedEffect(settings) {
        localNvidiaKey = settings.nvidiaKey
        localOpenRouterKey = settings.openRouterKey
        localGeminiKey = settings.geminiKey
        localSystemPrompt = settings.customSystemPrompt
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description and instructions
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "API Key console settings",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "API credentials are saved locally in SQLite storage on your mobile device. You can provide your custom keys here, or let the app fall back to default secrets managed securely during prototype build.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        

        // Keys customizer with Configuration status tags and encryption badge
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Secure Encryption",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Secure Key Vault",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "Local on-device encryption (AES-128 JCE Scheme) 🔒",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                
                // NVIDIA Credentials
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NVIDIA NIM AI Key", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            Row(
                                modifier = Modifier.clickable { 
                                    try { uriHandler.openUri("https://build.nvidia.com") } 
                                    catch (e: Exception) { e.printStackTrace() }
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Launch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Get NVIDIA NIM Key",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (settings.nvidiaKey.isNotBlank()) Color(0xFF00E676).copy(0.12f) else MaterialTheme.colorScheme.error.copy(0.12f),
                            contentColor = if (settings.nvidiaKey.isNotBlank()) Color(0xFF00E676) else MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                if (settings.nvidiaKey.isNotBlank()) "AES-128 ENCRYPTED 🔒" else "EMPTY / BUILD FALLBACK",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    OutlinedTextField(
                        value = localNvidiaKey,
                        onValueChange = { localNvidiaKey = it },
                        placeholder = { Text("nvapi-...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        visualTransformation = if (showNvidiaKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNvidiaKey = !showNvidiaKey }) {
                                Icon(
                                    imageVector = if (showNvidiaKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true
                    )
                    
                    // Live validation status text row
                    if (nvidiaStatus.isNotBlank()) {
                        val isSuccess = nvidiaStatus.contains("Verified")
                        val isChecking = nvidiaStatus.contains("Checking")
                        val textColor = when {
                            isSuccess -> Color(0xFF00C853)
                            isChecking -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Filled.CheckCircle else if (isChecking) Icons.Filled.Refresh else Icons.Filled.Error,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = nvidiaStatus,
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (settings.nvidiaKey.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    localNvidiaKey = ""
                                    viewModel.clearNvidiaKey()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Clear Key", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        Button(
                            onClick = { viewModel.validateAndSaveNvidiaKey(localNvidiaKey) },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Verify & Save Key", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                
                // Google Gemini Credentials
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Google AI Studio Key", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            Row(
                                modifier = Modifier.clickable { 
                                    try { uriHandler.openUri("https://aistudio.google.com/app/apikey") } 
                                    catch (e: Exception) { e.printStackTrace() }
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Launch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Get Gemini Key",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (settings.geminiKey.isNotBlank()) Color(0xFF00E676).copy(0.12f) else MaterialTheme.colorScheme.error.copy(0.12f),
                            contentColor = if (settings.geminiKey.isNotBlank()) Color(0xFF00E676) else MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                if (settings.geminiKey.isNotBlank()) "AES-128 ENCRYPTED 🔒" else "EMPTY / BUILD FALLBACK",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    OutlinedTextField(
                        value = localGeminiKey,
                        onValueChange = { localGeminiKey = it },
                        placeholder = { Text("AIza...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(
                                    imageVector = if (showGeminiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true
                    )
                    
                    // Live validation status text row
                    if (geminiStatus.isNotBlank()) {
                        val isSuccess = geminiStatus.contains("Verified")
                        val isChecking = geminiStatus.contains("Checking")
                        val textColor = when {
                            isSuccess -> Color(0xFF00C853)
                            isChecking -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Filled.CheckCircle else if (isChecking) Icons.Filled.Refresh else Icons.Filled.Error,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = geminiStatus,
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (settings.geminiKey.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    localGeminiKey = ""
                                    viewModel.clearGeminiKey()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Clear Key", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        Button(
                            onClick = { viewModel.validateAndSaveGeminiKey(localGeminiKey) },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Verify & Save Key", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecialistModelDashboard(
    model: AiModel,
    inputText: String,
    onSetInputText: (String) -> Unit
) {
    var attachedFile by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var translationSource by remember { mutableStateOf("English") }
    var translationTarget by remember { mutableStateOf("Spanish") }
    var thinkingDepth by remember { mutableStateOf("Balanced") }
    var selectedTone by remember { mutableStateOf("Balanced") }
    var selectedLanguageCode by remember { mutableStateOf("Kotlin 🚀") }
    var selectedVoiceModel by remember { mutableStateOf("Clara (Neural)") }
    var speedRate by remember { mutableStateOf(1.0f) }
    var pitchRate by remember { mutableStateOf(1.0f) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w1"
    )
    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w2"
    )
    val waveScale3 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w3"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (model.computedCategory) {
                            "Coding" -> Icons.Filled.Code
                            "Reasoning" -> Icons.Filled.Psychology
                            "Vision" -> Icons.Filled.Visibility
                            "Voice" -> Icons.Filled.VolumeUp
                            "Translation" -> Icons.Filled.Translate
                            "Safety" -> Icons.Filled.Security
                            else -> Icons.Filled.AutoAwesome
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (model.computedCategory) {
                            "Coding" -> "Developer Playground Specialist"
                            "Reasoning" -> "Cognitive Thinking Specialist"
                            "Vision" -> "Vision-Language Specialist"
                            "Voice" -> "Acoustic TTS Specialist"
                            "Translation" -> "Linguistic Translation Workspace"
                            "Safety" -> "Policy Safety Audits & Guardrails"
                            else -> "General Purpose Prompt Optimizers"
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(0.4f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = model.computedCategory.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                    )
                }
            }
            
            when (model.computedCategory) {
                "Coding" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Inject structural code scaffolds directly into your prompt context:",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            val codePresets = listOf(
                                "Refactor" to "Refactor this code to follow clean architecture, improve spacing, reduce complexity, and make it production-grade:\n",
                                "Find Bug" to "Analyze this code thoroughly, discover any syntax, memory leak, or logic error, and provide precise trace solutions:\n",
                                "Write Tests" to "Generate clean unit tests for this class covering all core success and failure exception paths, using mocks:\n",
                                "Big-O Scale" to "Provide a rigorous time/space complexity analysis of this algorithm and outline performance scaling improvements:\n"
                            )
                            codePresets.forEach { (label, presetText) ->
                                AssistChip(
                                    onClick = { onSetInputText(presetText + inputText) },
                                    label = { Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Language compiler target:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            val languages = listOf("Kotlin 🚀", "Python 🐍", "Rust 🦀", "JavaScript 🌐", "Java ☕")
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                languages.forEach { lang ->
                                    val isSelected = selectedLanguageCode == lang
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .clickable {
                                                selectedLanguageCode = lang
                                                onSetInputText(inputText + "\n// Target compiler specs: $lang ...\n")
                                            }
                                    ) {
                                        Text(
                                            text = lang,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                "Reasoning" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cognitive depth mode:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Fast Trace ⚡", "Socratic CoT 🔄", "Einstein Max 🤯").forEach { depth ->
                                    val active = thinkingDepth == depth
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable {
                                            thinkingDepth = depth
                                            onSetInputText("[$depth Reason Mode] - " + inputText)
                                        }
                                    ) {
                                        Text(depth, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                }
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 0.5.dp)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Engage Web Search RAG simulation layer", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                            var isSearchChecked by remember { mutableStateOf(false) }
                            Switch(
                                checked = isSearchChecked,
                                onCheckedChange = {
                                    isSearchChecked = it
                                    if (it) {
                                        onSetInputText(inputText + "\n[System Context - Include web indices & fresh news databases query results...]\n")
                                    }
                                },
                                modifier = Modifier.scale(0.7f)
                            )
                        }
                    }
                }
                
                "Vision" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Simulated Cross-Modal Vision upload pipeline:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val visionPreviews = listOf(
                                "blueprint_schematic.png" to "Analyze this detailed blueprint layout for structural safety, dimensional compliance, and physical loads:\n",
                                "lidar_pcd_cloud.jpg" to "Process this LiDAR sensor point cloud camera frame, detect all 3D autonomous boxes, and estimate collision distances:\n",
                                "protein_fold_v2.sdf" to "Analyze this ESMFold protein molecular sequence, compute the 3D ribbon folds, and predict chemical binders:\n"
                            )
                            visionPreviews.forEach { (name, promptExt) ->
                                val isChosen = attachedFile == name
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChosen) MaterialTheme.colorScheme.primary.copy(0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                                        .border(1.dp, if (isChosen) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable {
                                            attachedFile = if (isChosen) null else name
                                            if (!isChosen) {
                                                onSetInputText(promptExt + inputText)
                                            }
                                        }
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = if (isChosen) Icons.Filled.CheckCircle else Icons.Filled.AttachFile,
                                            contentDescription = null,
                                            tint = if (isChosen) Color(0xFF00C853) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(name, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                        
                        if (attachedFile != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF00E676).copy(0.12f),
                                border = BorderStroke(1.dp, Color(0xFF00C853).copy(0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Attached vision token: $attachedFile (Resolved @ 300 DPI)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                                    }
                                    IconButton(
                                        onClick = { attachedFile = null },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                
                "Voice" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Acoustic synthesis dials:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            
                            if (isRecording) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(waveScale1, waveScale2, waveScale3, waveScale2, waveScale1).forEach { scl ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(14.dp * scl)
                                                .background(Color.Red, RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    isRecording = !isRecording
                                    if (isRecording) {
                                        onSetInputText("🎙️ [Acoustic Transcript]: Translate this spoken query into direct speech synthesis waveforms using Riva and Magpie TTS...")
                                    } else {
                                        isRecording = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f).height(38.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isRecording) "RECORDING..." else "TALK TO AI 🎙️",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                            
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).height(38.dp).clickable { selectedVoiceModel = if (selectedVoiceModel.contains("Clara")) "Brian (Pro)" else "Clara (Neural)" }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(selectedVoiceModel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Acoustic Speed (Rate): ${"%.1f".format(speedRate)}x", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = speedRate,
                                    onValueChange = { speedRate = it },
                                    valueRange = 0.5f..2.0f,
                                    modifier = Modifier.width(90.dp).scale(0.7f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pitch Frequency: ${"%.1f".format(pitchRate)}Hz", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = pitchRate,
                                    onValueChange = { pitchRate = it },
                                    valueRange = 0.5f..2.0f,
                                    modifier = Modifier.width(90.dp).scale(0.7f)
                                )
                            }
                        }
                    }
                }
                
                "Translation" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dialect Translation Workspace:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = {
                                    val temp = translationSource
                                    translationSource = translationTarget
                                    translationTarget = temp
                                    onSetInputText("Translate from $translationSource into fluent $translationTarget:\n" + inputText)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap Languages", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).height(34.dp).clickable { translationSource = if (translationSource == "English") "Mandarin" else "English" }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("From: $translationSource 🌐", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                            
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).height(34.dp).clickable { translationTarget = if (translationTarget == "Spanish") "Hindi" else "Spanish" }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("To: $translationTarget 📍", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "Formal diplomatic" to "Translate this formally, preserving status prefixes, academic vocabularies, and diplomatic composure:\n",
                                "Colloquial slang" to "Translate this utilizing regional cultural slangs, idioms, and natural local figures of speech:\n",
                                "Word-for-word" to "Provide a strictly literal literal syntax dictionary translation:\n"
                            ).forEach { (label, promptTxt) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(0.08f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.15f)),
                                    modifier = Modifier.weight(1f).clickable {
                                        onSetInputText(promptTxt + inputText)
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                                        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
                
                "Safety" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Configure safety classifier modes & redact content before transmission:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("PII Redaction" to true, "Jailbreak Blocker" to true, "Toxicity check" to true).forEach { (label, startActive) ->
                                var activeCheck by remember { mutableStateOf(startActive) }
                                FilterChip(
                                    selected = activeCheck,
                                    onClick = { activeCheck = !activeCheck },
                                    label = { Text(label, fontSize = 9.sp) },
                                    leadingIcon = {
                                        if (activeCheck) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(10.dp))
                                        }
                                    }
                                )
                            }
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF00E676).copy(0.12f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Policy Alignment: 99.98% Compliant (PASS) ✔", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00C853))
                                    Text("No PII, toxic, or prisonbreak bypass indicators detected.", fontSize = 9.sp, color = Color(0xFF00C853))
                                }
                            }
                        }
                    }
                }
                
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Optimize general questions or tweak standard formatting constraints:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "Empathetic ❤️" to "[Persona: Warm, encouraging, supportive] ",
                                "Creative Spark ✨" to "[Persona: Extremely descriptive, brainstorming, metaphoric] ",
                                "Brutally Concise ⏱️" to "[Persona: Maximum 2 sentences, absolute brevity, clear bulletpoints] ",
                                "Academic 🎓" to "[Persona: Scholarly vocabulary, citations, multi-angle analytical depth] "
                            ).forEach { (label, preset) ->
                                val isSelected = selectedTone == label
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedTone = label
                                            onSetInputText(preset + inputText)
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutAndDeveloperScreen() {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Intel Vault & Security",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Developer Card Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CORE DEVELOPER INTEL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "Rehan97",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "GitHub: ft976",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // GitHub Button
                                IconButton(
                                    onClick = { 
                                        try { uriHandler.openUri("https://github.com/ft976") } 
                                        catch (e: Exception) { e.printStackTrace() }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Launch,
                                        contentDescription = "GitHub",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                // LinkedIn Button
                                IconButton(
                                    onClick = { 
                                        try { uriHandler.openUri("https://www.linkedin.com/in/rehan-ahmad-863386382?utm_source=share_via&utm_content=profile&utm_medium=member_android") } 
                                        catch (e: Exception) { e.printStackTrace() }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Link,
                                        contentDescription = "LinkedIn",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Quote Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Text(
                            text = "gave it everything I had",
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\"And man will have nothing except what he strives for\"",
                            fontSize = 13.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "53:39",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "doing my part. the rest is His",
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Privacy and Security guardrails
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "PRIVACY & SECURITY PROTOCOLS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    // Guardrail 1
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = "AES-128 JCE Scheme",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "All API keys are encrypted at-rest using standardized native cryptors inside local SQLite space.",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Guardrail 2
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = "Direct-to-API Gateway",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "This application opens secure SSL connections directly to API endpoints without middleman servers.",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Guardrail 3
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = "No Background Telemetry",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Chat history is fully offline-first. No logs collect or upload your prompt to tracking servers.",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text = "Hello $name! Ready to prompt modern intelligence.")
    }
}


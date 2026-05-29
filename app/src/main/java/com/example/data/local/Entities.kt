package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class SettingEntity(
    @PrimaryKey val id: String = "app_settings",
    val nvidiaKey: String = "",
    val openRouterKey: String = "",
    val currentModelId: String = "openrouter_free_auto",
    val currentPlatform: String = "OpenRouter", // "NVIDIA" or "OpenRouter"
    val customSystemPrompt: String = "You are a helpful, respectful, and highly intelligent AI assistant."
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val modelId: String = "openrouter_free_auto",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user", "assistant" or "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = "",
    val platformUsed: String = "",
    val isError: Boolean = false
)

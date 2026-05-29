package com.example.data.api

import com.example.data.model.AiModel
import java.io.IOException

sealed class GatewayResult {
    data class Success(
        val text: String,
        val mappedModelId: String,
        val platform: String
    ) : GatewayResult()

    data class Error(
        val message: String,
        val platform: String? = null
    ) : GatewayResult()
}

class UnifiedAiGatewayService(
    private val apiService: OpenAiApiService
) {
    suspend fun routeAndExecute(
        messages: List<ApiMessage>,
        systemPrompt: String?,
        selectedModel: AiModel,
        nvidiaKey: String,
        openRouterKey: String,
        preferredPlatform: String,
        temperature: Double = 0.7,
        maxTokens: Int = 2048
    ): GatewayResult {
        // 1. Determine destination platform based on model's default platform or preferred routing
        val targetPlatform = when (selectedModel.platform) {
            "NVIDIA" -> "NVIDIA"
            "OpenRouter" -> "OpenRouter"
            else -> preferredPlatform // default fallback for both
        }

        // 2. Resolve credentials safely
        val apiKey = if (targetPlatform == "NVIDIA") nvidiaKey else openRouterKey
        if (apiKey.isBlank()) {
            val endpointLabel = if (targetPlatform == "NVIDIA") "NVIDIA NIM" else "OpenRouter"
            return GatewayResult.Error(
                message = "The $endpointLabel key is missing. Please save standard keys or configure them in Settings.",
                platform = targetPlatform
            )
        }

        // 3. Map model identifiers for target gateway
        val modelApiId = when (targetPlatform) {
            "NVIDIA" -> if (selectedModel.defaultNvidiaId.isNotBlank()) selectedModel.defaultNvidiaId else selectedModel.id
            else -> if (selectedModel.defaultOpenRouterId.isNotBlank()) selectedModel.defaultOpenRouterId else selectedModel.id
        }

        // 4. Set platform URL
        val url = if (targetPlatform == "NVIDIA") {
            "https://integrate.api.nvidia.com/v1/chat/completions"
        } else {
            "https://openrouter.ai/api/v1/chat/completions"
        }

        // 5. Construct payload
        val apiMessages = mutableListOf<ApiMessage>()
        if (!systemPrompt.isNullOrBlank()) {
            apiMessages.add(ApiMessage("system", systemPrompt))
        }
        apiMessages.addAll(messages)

        val request = ChatCompletionRequest(
            model = modelApiId,
            messages = apiMessages,
            temperature = temperature,
            max_tokens = maxTokens
        )

        val authHeader = "Bearer $apiKey"

        // 6. Execute network transaction
        return try {
            val response = apiService.getChatCompletion(
                url = url,
                authHeader = authHeader,
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content
            if (content != null) {
                GatewayResult.Success(
                    text = content,
                    mappedModelId = modelApiId,
                    platform = targetPlatform
                )
            } else {
                GatewayResult.Error(
                    message = "Received an empty chat completion payload from $targetPlatform NIM gateway.",
                    platform = targetPlatform
                )
            }
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val rawError = e.response()?.errorBody()?.string() ?: ""
            val message = "Gateway Error (HTTP $code): ${rawError.ifBlank { e.message() }}"
            GatewayResult.Error(message = message, platform = targetPlatform)
        } catch (e: Exception) {
            val message = e.localizedMessage ?: "Network connection timeout during gateway proxy dispatch."
            GatewayResult.Error(message = message, platform = targetPlatform)
        }
    }
}

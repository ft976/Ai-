package com.example.data.api

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ApiMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val temperature: Double? = 0.7,
    val max_tokens: Int? = 2048
)

@JsonClass(generateAdapter = true)
data class ApiChoice(
    val index: Int?,
    val message: ApiMessage?,
    val finish_reason: String?
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    val id: String?,
    val choices: List<ApiChoice>?
)

interface OpenAiApiService {
    @POST
    suspend fun getChatCompletion(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: ChatCompletionRequest,
        @Header("HTTP-Referer") referer: String = "https://ai.studio/build",
        @Header("X-Title") appTitle: String = "Multi-Model AI Chat"
    ): ChatCompletionResponse

    @GET
    suspend fun listModels(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): retrofit2.Response<okhttp3.ResponseBody>
}

object ApiClient {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val service: OpenAiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://ai.studio/build/") // Dummy base URL required by Retrofit (overridden by @Url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(OpenAiApiService::class.java)
    }

    val gatewayService: UnifiedAiGatewayService by lazy {
        UnifiedAiGatewayService(service)
    }
}

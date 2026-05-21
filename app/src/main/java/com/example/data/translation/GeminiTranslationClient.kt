package com.example.data.translation

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiTranslationService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun translateText(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

class GeminiTranslationClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service = retrofit.create(GeminiTranslationService::class.java)

    suspend fun translateToArabic(text: String): String {
        if (text.trim().isBlank()) return ""
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            return "[Error: API translation requires a valid GEMINI_API_KEY. Please set GEMINI_API_KEY in the Secrets panel in AI Studio.]"
        }

        val systemMessage = "You are an expert manga and manhwa translator specialized in translating raw dialogues into natural, highly engaging, and context-aware Arabic. Clean up any weird scanning errors or layout fragments, ensure accurate translations that sound natural for comics, and output only the translated Arabic text."
        val prompt = "Translate the following English manga text into expressive Arabic comic dialogues:\n\n\"\"\"\n$text\n\"\"\""

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemMessage))),
            generationConfig = GeminiGenerationConfig(temperature = 0.3f)
        )

        return try {
            val response = service.translateText(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "No translation returned."
        } catch (e: Exception) {
            e.printStackTrace()
            "Translation Error: ${e.localizedMessage}"
        }
    }
}

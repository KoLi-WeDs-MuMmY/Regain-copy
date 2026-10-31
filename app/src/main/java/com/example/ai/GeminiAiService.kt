package com.example.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String
)

class GeminiAiService {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val candidateModels = listOf(
        "gemini-2.0-flash-lite",
        "gemini-1.5-flash"
    )
    private val systemInstructions = """
        You are Rega, a calm, wise, and encouraging Zen digital wellbeing coach. 
        Your goal is to help the user regain focus, manage screen time, and find balance in a noisy digital world.
        
        Guidelines:
        1. Speak with a peaceful, minimalist tone. Use metaphors related to nature (garden, stream, stone, breath).
        2. Focus strictly on productivity, mindfulness, digital wellbeing, and deep work habits.
        3. Do NOT generate images. You are a text-only companion.
        4. If the user asks about distracting apps (Instagram, YouTube, TikTok), provide strategies to shield focus and reduce dopamine loops.
        5. Encourage the user to use the 'Regain Zen' features like Pomodoro timers, ambient soundscapes, and short-form shields.
        6. Keep responses concise and impactful.
    """.trimIndent()

    suspend fun chat(
        apiKey: String,
        history: List<ChatMessage>,
        userMessage: String
    ): String? = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstructions))))

            val contentsArray = JSONArray()
            history.forEach { msg ->
                contentsArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                })
            }
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            })
            put("contents", contentsArray)

            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 512)
                put("temperature", 0.7)
            })
        }

        val bodyBytes = payload.toString().toRequestBody("application/json".toMediaType())

        for (model in candidateModels) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(bodyBytes)
                .build()

            try {
                val reply = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val bodyStr = response.body?.string() ?: return@use null
                    val root = JSONObject(bodyStr)
                    root.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                }
                if (!reply.isNullOrBlank()) {
                    return@withContext reply
                }
            } catch (_: Exception) {
                // Try next Flash model candidate
            }
        }
        null
    }

    suspend fun generateStructuredInsight(
        apiKey: String,
        prompt: String
    ): JSONObject? = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "You are a JSON-only digital wellbeing engine. Output strict JSON only."))))
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("maxOutputTokens", 512)
                put("temperature", 0.2)
            })
        }

        val bodyBytes = payload.toString().toRequestBody("application/json".toMediaType())

        for (model in candidateModels) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(bodyBytes)
                .build()

            try {
                val parsed = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val bodyStr = response.body?.string() ?: return@use null
                    val root = JSONObject(bodyStr)
                    val text = root.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: return@use null
                    JSONObject(text)
                }
                if (parsed != null) return@withContext parsed
            } catch (_: Exception) {
                // Try next Flash model candidate
            }
        }
        null
    }
}

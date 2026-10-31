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

    private val defaultModel = "gemini-1.5-flash"
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
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$defaultModel:generateContent?key=$apiKey"
        
        val contents = JSONArray()
        
        // Add system instruction as part of the first message or use system_instruction if supported in v1beta
        // For simplicity in OkHttp implementation, we prepend it to the context or use a special message.
        // Google AI SDK uses a specific field, but via REST it can be a part of the contents if the model supports it.
        // Alternatively, we use the v1beta system_instruction field.
        
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

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val root = JSONObject(bodyStr)
                return@withContext root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun generateStructuredInsight(
        apiKey: String,
        prompt: String
    ): JSONObject? = withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$defaultModel:generateContent?key=$apiKey"
        
        val payload = JSONObject().apply {
            put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "You are a JSON-only digital wellbeing engine. Output strict JSON only."))))
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("maxOutputTokens", 512)
                put("temperature", 0.2)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val root = JSONObject(bodyStr)
                val text = root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: return@withContext null
                
                return@withContext JSONObject(text)
            }
        } catch (e: Exception) {
            null
        }
    }
}

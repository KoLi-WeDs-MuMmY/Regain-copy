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

enum class AiProvider(
    val id: String,
    val displayName: String,
    val badgeLabel: String,
    val keyPrefixHint: String,
    val helpSubtitle: String,
    val defaultModelLabel: String
) {
    GROQ(
        id = "GROQ",
        displayName = "Groq Cloud (Free Tier)",
        badgeLabel = "NO BILLING • FAST",
        keyPrefixHint = "gsk_...",
        helpSubtitle = "100% free API keys at console.groq.com (Llama 3.3 70B & 3.1 8B Instant, 30 RPM free tier, no credit card required).",
        defaultModelLabel = "llama-3.3-70b-versatile"
    ),
    OPENROUTER(
        id = "OPENROUTER",
        displayName = "OpenRouter (Free Models)",
        badgeLabel = "FREE :free MODELS",
        keyPrefixHint = "sk-or-v1-...",
        helpSubtitle = "Free-tier keys at openrouter.ai/keys routing to Llama 3.3 70B (:free), Gemma 3 27B (:free), and DeepSeek V3 (:free).",
        defaultModelLabel = "meta-llama/llama-3.3-70b-instruct:free"
    ),
    GEMINI(
        id = "GEMINI",
        displayName = "Google Gemini API",
        badgeLabel = "AI STUDIO KEY",
        keyPrefixHint = "AIzaSy...",
        helpSubtitle = "Google AI Studio key (aistudio.google.com) for Gemini 2.0 Flash-Lite & 1.5 Flash.",
        defaultModelLabel = "gemini-2.0-flash-lite"
    );

    companion object {
        fun fromId(id: String?): AiProvider {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: GROQ
        }

        fun resolveEffectiveProvider(selectedId: String?, apiKey: String): AiProvider {
            val trimmed = apiKey.trim()
            return when {
                trimmed.startsWith("gsk_") -> GROQ
                trimmed.startsWith("sk-or-") -> OPENROUTER
                trimmed.startsWith("AIza") -> GEMINI
                else -> fromId(selectedId)
            }
        }
    }
}

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

    private val geminiModels = listOf(
        "gemini-2.0-flash-lite",
        "gemini-1.5-flash"
    )

    private val groqModels = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant"
    )

    private val openRouterFreeModels = listOf(
        "meta-llama/llama-3.3-70b-instruct:free",
        "google/gemma-3-27b-it:free",
        "deepseek/deepseek-chat-v3-0324:free"
    )

    private val systemInstructions = """
        You are Rega, a calm, wise, and encouraging Zen digital wellbeing coach inside Regain Zen.
        Your goal is to help the user regain focus, master Pomodoro deep work, manage screen time, and find balance in a noisy digital world.

        Guidelines:
        1. Speak with a peaceful, minimalist, tactile-chrono tone. Use subtle metaphors of nature and craft (garden, stream, stone, chronometer, breath).
        2. Focus strictly on productivity, mindfulness, study techniques, digital wellbeing, and deep work habits.
        3. Do NOT generate images. You are a text-only companion.
        4. If the user asks about distracting apps (Instagram Reels, YouTube Shorts, TikTok), provide actionable strategies to shield focus and break dopamine loops.
        5. Encourage using Regain Zen features like custom Pomodoro cycles, Focus Soundscapes, Task Checklists, and Aegis Shield.
        6. Keep responses concise (2–4 short paragraphs or bullet points) and immediately practical.
    """.trimIndent()

    suspend fun chat(
        apiKey: String,
        history: List<ChatMessage>,
        userMessage: String,
        providerId: String = "GROQ"
    ): String? = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return@withContext null

        val provider = AiProvider.resolveEffectiveProvider(providerId, cleanKey)
        when (provider) {
            AiProvider.GROQ -> chatOpenAiCompatible(
                endpoint = "https://api.groq.com/openai/v1/chat/completions",
                apiKey = cleanKey,
                models = groqModels,
                history = history,
                userMessage = userMessage,
                isOpenRouter = false
            )
            AiProvider.OPENROUTER -> chatOpenAiCompatible(
                endpoint = "https://openrouter.ai/api/v1/chat/completions",
                apiKey = cleanKey,
                models = openRouterFreeModels,
                history = history,
                userMessage = userMessage,
                isOpenRouter = true
            )
            AiProvider.GEMINI -> chatGemini(
                apiKey = cleanKey,
                history = history,
                userMessage = userMessage
            )
        }
    }

    private fun chatOpenAiCompatible(
        endpoint: String,
        apiKey: String,
        models: List<String>,
        history: List<ChatMessage>,
        userMessage: String,
        isOpenRouter: Boolean
    ): String? {
        for (model in models) {
            val messagesArray = JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("role", "system")
                        put("content", systemInstructions)
                    }
                )
                history.takeLast(10).forEach { msg ->
                    put(
                        JSONObject().apply {
                            put("role", if (msg.role == "user") "user" else "assistant")
                            put("content", msg.text)
                        }
                    )
                }
                put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    }
                )
            }

            val payload = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("max_tokens", 512)
                put("temperature", 0.7)
            }

            val bodyBytes = payload.toString().toRequestBody("application/json".toMediaType())
            val reqBuilder = Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(bodyBytes)

            if (isOpenRouter) {
                reqBuilder.header("HTTP-Referer", "https://regainzen.app")
                reqBuilder.header("X-Title", "Regain Zen")
            }

            try {
                val reply = httpClient.newCall(reqBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val bodyStr = response.body?.string() ?: return@use null
                    val root = JSONObject(bodyStr)
                    root.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                }
                if (!reply.isNullOrBlank()) {
                    return reply.trim()
                }
            } catch (_: Exception) {
                // Try next free-tier model candidate
            }
        }
        return null
    }

    private fun chatGemini(
        apiKey: String,
        history: List<ChatMessage>,
        userMessage: String
    ): String? {
        val payload = JSONObject().apply {
            put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstructions)))
            )

            val contentsArray = JSONArray()
            history.takeLast(10).forEach { msg ->
                contentsArray.put(
                    JSONObject().apply {
                        put("role", if (msg.role == "user") "user" else "model")
                        put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                    }
                )
            }
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
                }
            )
            put("contents", contentsArray)

            put(
                "generationConfig",
                JSONObject().apply {
                    put("maxOutputTokens", 512)
                    put("temperature", 0.7)
                }
            )
        }

        val bodyBytes = payload.toString().toRequestBody("application/json".toMediaType())

        for (model in geminiModels) {
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
                    return reply.trim()
                }
            } catch (_: Exception) {
                // Try next Gemini model candidate
            }
        }
        return null
    }
}

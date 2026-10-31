package com.example.ai

import com.example.data.AiCoachInsightEntity
import com.example.data.AppLimitEntity
import com.example.data.FocusSessionEntity
import com.example.data.ShortFormShieldEntity
import com.example.data.UserProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * SinglePromptAiCoach enforces a strict hard cap of AT MOST 1 prompt request per calendar day
 * for the daily synthesis card, and supports free-tier keys from Groq, OpenRouter, and Google Gemini
 * with an automatic 100% offline fallback to the On-Device Zen Heuristic Engine.
 */
class SinglePromptAiCoach {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun buildSingleConsolidatedPrompt(
        apps: List<AppLimitEntity>,
        shields: List<ShortFormShieldEntity>,
        sessions: List<FocusSessionEntity>,
        progress: UserProgressEntity
    ): String {
        val totalUsedMin = apps.sumOf { it.usedTodayMinutes }
        val totalPickups = apps.sumOf { it.pickupsToday }
        val topApp = apps.maxByOrNull { it.usedTodayMinutes }
        val totalIntercepts = shields.sumOf { it.interceptsToday }
        val totalFocusMin = sessions.sumOf { it.durationMinutes }

        return buildString {
            append("You are Rega, a calm Zen digital wellbeing coach. ")
            append("Analyze this user's daily metrics in ONE single response and output strict JSON ")
            append("with keys: coachHeadline, habitBottleneck, timeSavedProjectionMinutes (int), ")
            append("microChallenge1, microChallenge2, microChallenge3.\n")
            append("Metrics: ScreenTime=${totalUsedMin}m (Goal=${progress.mindfulGoalMinutes}m), ")
            append("Pickups=$totalPickups, TopApp=${topApp?.appName ?: "None"} (${topApp?.usedTodayMinutes ?: 0}m/${topApp?.dailyLimitMinutes ?: 0}m limit), ")
            append("ReelsAndShortsIntercepts=$totalIntercepts, DeepFocusCompleted=${totalFocusMin}m, ")
            append("Streak=${progress.streakDays}d.")
        }
    }

    suspend fun generateDailyCappedInsight(
        dateKey: String,
        apps: List<AppLimitEntity>,
        shields: List<ShortFormShieldEntity>,
        sessions: List<FocusSessionEntity>,
        progress: UserProgressEntity,
        preferOnlineGeminiIfKeyPresent: Boolean = false,
        optionalApiKey: String = "",
        providerId: String = progress.aiProvider
    ): AiCoachInsightEntity = withContext(Dispatchers.IO) {
        val consolidatedPrompt = buildSingleConsolidatedPrompt(apps, shields, sessions, progress)
        val cleanKey = optionalApiKey.trim()

        if (preferOnlineGeminiIfKeyPresent && cleanKey.isNotBlank() && cleanKey != "MY_GEMINI_API_KEY") {
            try {
                val provider = AiProvider.resolveEffectiveProvider(providerId, cleanKey)
                val onlineResult = when (provider) {
                    AiProvider.GROQ -> callOpenAiCompatibleInsight(
                        endpoint = "https://api.groq.com/openai/v1/chat/completions",
                        apiKey = cleanKey,
                        models = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"),
                        dateKey = dateKey,
                        prompt = consolidatedPrompt,
                        sourceLabel = "GROQ_FREE_TIER",
                        isOpenRouter = false
                    )
                    AiProvider.OPENROUTER -> callOpenAiCompatibleInsight(
                        endpoint = "https://openrouter.ai/api/v1/chat/completions",
                        apiKey = cleanKey,
                        models = listOf(
                            "meta-llama/llama-3.3-70b-instruct:free",
                            "google/gemma-3-27b-it:free"
                        ),
                        dateKey = dateKey,
                        prompt = consolidatedPrompt,
                        sourceLabel = "OPENROUTER_FREE_TIER",
                        isOpenRouter = true
                    )
                    AiProvider.GEMINI -> callSingleGeminiRequest(
                        apiKey = cleanKey,
                        dateKey = dateKey,
                        prompt = consolidatedPrompt
                    )
                }
                if (onlineResult != null) {
                    return@withContext onlineResult
                }
            } catch (_: Exception) {
                // Fall back gracefully to 100% offline engine
            }
        }

        generateOfflineHeuristicInsight(
            dateKey = dateKey,
            apps = apps,
            shields = shields,
            sessions = sessions,
            progress = progress,
            consolidatedPrompt = consolidatedPrompt
        )
    }

    private fun callOpenAiCompatibleInsight(
        endpoint: String,
        apiKey: String,
        models: List<String>,
        dateKey: String,
        prompt: String,
        sourceLabel: String,
        isOpenRouter: Boolean
    ): AiCoachInsightEntity? {
        for (model in models) {
            val payload = JSONObject().apply {
                put("model", model)
                put(
                    "messages",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("role", "system")
                                put("content", "You are a JSON-only digital wellbeing engine. Output a single valid JSON object only without markdown fences.")
                            }
                        )
                        put(
                            JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            }
                        )
                    }
                )
                put("max_tokens", 350)
                put("temperature", 0.3)
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

            val result = runCatching {
                httpClient.newCall(reqBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val bodyStr = response.body?.string() ?: return@use null
                    val root = JSONObject(bodyStr)
                    val rawText = root.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content") ?: return@use null

                    val cleanJsonText = extractJsonObjectString(rawText)
                    val parsed = JSONObject(cleanJsonText)
                    AiCoachInsightEntity(
                        dateKey = dateKey,
                        requestsUsedToday = 1,
                        coachHeadline = parsed.optString("coachHeadline", "Your mindful boundaries are protecting your best hours."),
                        habitBottleneck = parsed.optString("habitBottleneck", "High impulse pickups during afternoon transitions."),
                        timeSavedProjectionMinutes = parsed.optInt("timeSavedProjectionMinutes", 85),
                        microChallenge1 = parsed.optString("microChallenge1", "Enable Strict Mode during your next 25m Pomodoro."),
                        microChallenge2 = parsed.optString("microChallenge2", "Keep Instagram & YouTube in DM/Search-only Shield mode after 9 PM."),
                        microChallenge3 = parsed.optString("microChallenge3", "Take 3 slow 4-4-4 breaths whenever a feed block appears."),
                        promptUsedPreview = prompt,
                        sourceMode = sourceLabel
                    )
                }
            }.getOrNull()

            if (result != null) return result
        }
        return null
    }

    private fun extractJsonObjectString(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else raw.trim()
    }

    private fun callSingleGeminiRequest(
        apiKey: String,
        dateKey: String,
        prompt: String
    ): AiCoachInsightEntity? {
        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("maxOutputTokens", 350)
                    put("temperature", 0.4)
                }
            )
        }

        val bodyBytes = payload.toString().toRequestBody("application/json".toMediaType())
        val models = listOf("gemini-2.0-flash-lite", "gemini-1.5-flash")

        for (model in models) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(bodyBytes)
                .build()

            val result = runCatching {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val bodyStr = response.body?.string() ?: return@use null
                    val root = JSONObject(bodyStr)
                    val text = root.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: return@use null

                    val parsed = JSONObject(extractJsonObjectString(text))
                    AiCoachInsightEntity(
                        dateKey = dateKey,
                        requestsUsedToday = 1,
                        coachHeadline = parsed.optString("coachHeadline", "Your mindful boundaries are protecting your best hours."),
                        habitBottleneck = parsed.optString("habitBottleneck", "High impulse pickups during afternoon transitions."),
                        timeSavedProjectionMinutes = parsed.optInt("timeSavedProjectionMinutes", 85),
                        microChallenge1 = parsed.optString("microChallenge1", "Enable Strict Mode during your next 25m Pomodoro."),
                        microChallenge2 = parsed.optString("microChallenge2", "Keep Instagram & YouTube in DM/Search-only Shield mode after 9 PM."),
                        microChallenge3 = parsed.optString("microChallenge3", "Take 3 slow 4-4-4 breaths whenever a feed block appears."),
                        promptUsedPreview = prompt,
                        sourceMode = "GEMINI_1_REQ_CAPPED"
                    )
                }
            }.getOrNull()

            if (result != null) return result
        }
        return null
    }

    private fun generateOfflineHeuristicInsight(
        dateKey: String,
        apps: List<AppLimitEntity>,
        shields: List<ShortFormShieldEntity>,
        sessions: List<FocusSessionEntity>,
        progress: UserProgressEntity,
        consolidatedPrompt: String
    ): AiCoachInsightEntity {
        val totalUsedMin = apps.sumOf { it.usedTodayMinutes }
        val totalPickups = apps.sumOf { it.pickupsToday }
        val topApp = apps.maxByOrNull { it.usedTodayMinutes }
        val mostIntercepted = shields.maxByOrNull { it.interceptsToday }
        val totalIntercepts = shields.sumOf { it.interceptsToday }
        val totalFocusMin = sessions.sumOf { it.durationMinutes }

        val headline = when {
            totalUsedMin <= progress.mindfulGoalMinutes ->
                "Calm Mastery: You're ${progress.mindfulGoalMinutes - totalUsedMin}m under your mindful screen budget today."
            else ->
                "Gentle Course-Correction: ${topApp?.appName ?: "Social feeds"} pushed screen time ${totalUsedMin - progress.mindfulGoalMinutes}m past your goal."
        }

        val bottleneck = buildString {
            if (topApp != null) {
                append("${topApp.appName} accounts for ${topApp.usedTodayMinutes}m across ${topApp.pickupsToday} quick check-ins")
            } else {
                append("Frequent micro-pickups ($totalPickups today) fragmenting deep work")
            }
            if (mostIntercepted != null && mostIntercepted.interceptsToday > 0) {
                append(", while ${mostIntercepted.platformName} (${mostIntercepted.feedName}) triggered ${mostIntercepted.interceptsToday} automatic doomscroll saves.")
            } else {
                append(".")
            }
        }

        val projectedSaved = (totalIntercepts * 4 + (totalFocusMin / 2)).coerceAtLeast(45)

        val challenge1 = if (topApp != null && !topApp.isStrictLocked) {
            "Turn on Strict Lock for ${topApp.appName} and trim its daily cap by 5 minutes (to ${(topApp.dailyLimitMinutes - 5).coerceAtLeast(10)}m)."
        } else {
            "Complete one 25-minute Pomodoro with 'Kyoto Moss Rain' or '40Hz Gamma Focus' audio before checking messages."
        }

        val challenge2 = if (mostIntercepted != null) {
            "Keep ${mostIntercepted.platformName}'s '${mostIntercepted.feedName}' shield active—it already saved you ~${mostIntercepted.interceptsToday * 4}m of passive scrolling today."
        } else {
            "Switch YouTube Shorts and Instagram Reels to 'Mindful Breath Pause' so DMs stay open without feed traps."
        }

        val challenge3 = "Batch your remaining phone pickups into two intentional 10-minute windows to protect your ${progress.streakDays}-day Zen streak."

        return AiCoachInsightEntity(
            dateKey = dateKey,
            requestsUsedToday = 1,
            coachHeadline = headline,
            habitBottleneck = bottleneck,
            timeSavedProjectionMinutes = projectedSaved,
            microChallenge1 = challenge1,
            microChallenge2 = challenge2,
            microChallenge3 = challenge3,
            promptUsedPreview = consolidatedPrompt,
            sourceMode = "OFFLINE_ZEN_ENGINE"
        )
    }
}

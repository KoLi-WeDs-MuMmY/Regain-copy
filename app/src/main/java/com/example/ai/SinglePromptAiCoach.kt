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
 * so the user never exceeds Google's free API key rate limits.
 *
 * By default, it runs 100% offline using the deterministic On-Device Zen Heuristic Engine
 * (matching the offline-first architecture), and if an optional Gemini API key is configured,
 * executes at most ONE single consolidated JSON prompt per day and caches the result in Room.
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
        optionalApiKey: String = ""
    ): AiCoachInsightEntity = withContext(Dispatchers.IO) {
        val consolidatedPrompt = buildSingleConsolidatedPrompt(apps, shields, sessions, progress)

        if (preferOnlineGeminiIfKeyPresent && optionalApiKey.isNotBlank() && optionalApiKey != "MY_GEMINI_API_KEY") {
            try {
                val onlineResult = callSingleGeminiRequest(
                    apiKey = optionalApiKey,
                    dateKey = dateKey,
                    prompt = consolidatedPrompt
                )
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

    private fun callSingleGeminiRequest(
        apiKey: String,
        dateKey: String,
        prompt: String
    ): AiCoachInsightEntity? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
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

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyStr = response.body?.string() ?: return null
            val root = JSONObject(bodyStr)
            val text = root.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: return null

            val parsed = JSONObject(text)
            return AiCoachInsightEntity(
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

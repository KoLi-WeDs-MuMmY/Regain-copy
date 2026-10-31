package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class AppLimitEntity(
    val packageName: String,
    val appName: String,
    val category: String,
    val iconKey: String,
    val dailyLimitMinutes: Int,
    val usedTodayMinutes: Int,
    val pickupsToday: Int,
    val blockedCountToday: Int,
    val isStrictLocked: Boolean,
    val isEnabled: Boolean,
    val isBlockedCompletely: Boolean = false
)

data class ShortFormShieldEntity(
    val id: String,
    val platformName: String,
    val feedName: String,
    val shieldMode: String, // "BLOCK_COMPLETELY", "DAILY_CAP_15M", "MINDFUL_BREATH_PAUSE"
    val dailyCapMinutes: Int,
    val usedMinutesToday: Int,
    val interceptsToday: Int,
    val isEnabled: Boolean,
    val allowDirectMessages: Boolean
)

data class FocusSessionEntity(
    val id: Long = 0,
    val title: String,
    val tag: String,
    val mode: String, // "POMODORO", "COUNTDOWN", "STOPWATCH"
    val durationMinutes: Int,
    val durationSeconds: Int = 0,
    val ambientSound: String,
    val coinsEarned: Int,
    val completedAtTimestamp: Long = System.currentTimeMillis()
) {
    val exactDurationFormatted: String
        get() {
            val totalSecs = if (durationSeconds > 0) durationSeconds else durationMinutes * 60
            val hrs = totalSecs / 3600
            val mins = (totalSecs % 3600) / 60
            val secs = totalSecs % 60
            return when {
                hrs > 0 -> "${hrs}h ${mins}m ${secs}s"
                mins > 0 && secs > 0 -> "${mins}m ${secs}s"
                mins > 0 -> "${mins}m 0s"
                else -> "${secs}s"
            }
        }
}

data class ScheduleBlockEntity(
    val id: Long = 0,
    val title: String,
    val timeWindowLabel: String,
    val daysSummary: String,
    val targetSummary: String,
    val isStrictMode: Boolean,
    val isEnabled: Boolean
)

data class DailyQuestEntity(
    val id: String,
    val title: String,
    val subtitle: String,
    val rewardCoins: Int,
    val currentProgress: Int,
    val targetProgress: Int,
    val isCompleted: Boolean,
    val isClaimed: Boolean
)

data class UserProgressEntity(
    val id: Int = 1,
    val regaLevel: Int = 1,
    val regaStageName: String = "Mindful Pebble Seed",
    val leafCoins: Int = 0,
    val streakDays: Int = 0,
    val mindfulGoalMinutes: Int = 120,
    val savedMinutesToday: Int = 0,
    val mindfulBreathsCompleted: Int = 0,
    val strictModeGlobal: Boolean = false,
    val strictPassphrase: String = "I choose mindful presence",
    val customGeminiApiKey: String = "",
    val aiProvider: String = "GROQ"
)

data class AiCoachInsightEntity(
    val dateKey: String, // YYYY-MM-DD
    val requestsUsedToday: Int, // Strictly capped at 1 per day
    val coachHeadline: String,
    val habitBottleneck: String,
    val timeSavedProjectionMinutes: Int,
    val microChallenge1: String,
    val microChallenge2: String,
    val microChallenge3: String,
    val promptUsedPreview: String,
    val sourceMode: String, // "OFFLINE_ZEN_ENGINE" or "GEMINI_1_REQ_CAPPED"
    val generatedAtTimestamp: Long = System.currentTimeMillis()
)

interface RegainDao {
    fun getAllAppLimits(): Flow<List<AppLimitEntity>>
    suspend fun insertAppLimits(items: List<AppLimitEntity>)
    suspend fun updateAppLimit(item: AppLimitEntity)
    suspend fun deleteAppLimit(packageName: String)

    fun getAllShortFormShields(): Flow<List<ShortFormShieldEntity>>
    suspend fun insertShortFormShields(items: List<ShortFormShieldEntity>)
    suspend fun updateShortFormShield(item: ShortFormShieldEntity)

    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>>
    suspend fun insertFocusSession(item: FocusSessionEntity)
    suspend fun insertFocusSessions(items: List<FocusSessionEntity>)

    fun getAllSchedules(): Flow<List<ScheduleBlockEntity>>
    suspend fun insertSchedules(items: List<ScheduleBlockEntity>)
    suspend fun insertSchedule(item: ScheduleBlockEntity)
    suspend fun updateSchedule(item: ScheduleBlockEntity)
    suspend fun deleteSchedule(id: Long)

    fun getAllDailyQuests(): Flow<List<DailyQuestEntity>>
    suspend fun insertDailyQuests(items: List<DailyQuestEntity>)
    suspend fun updateDailyQuest(item: DailyQuestEntity)

    fun getUserProgress(): Flow<UserProgressEntity?>
    suspend fun upsertUserProgress(progress: UserProgressEntity)

    fun getAllAiInsights(): Flow<List<AiCoachInsightEntity>>
    suspend fun getAiInsightForDate(dateKey: String): AiCoachInsightEntity?
    suspend fun upsertAiInsight(insight: AiCoachInsightEntity)
}

class RegainDatabase private constructor(private val appContext: Context) :
    SQLiteOpenHelper(appContext, "regain_zen_offline_v11.db", null, 4),
    RegainDao {

    private val appLimitsFlow = MutableStateFlow<List<AppLimitEntity>>(emptyList())
    private val shieldsFlow = MutableStateFlow<List<ShortFormShieldEntity>>(emptyList())
    private val sessionsFlow = MutableStateFlow<List<FocusSessionEntity>>(emptyList())
    private val schedulesFlow = MutableStateFlow<List<ScheduleBlockEntity>>(emptyList())
    private val questsFlow = MutableStateFlow<List<DailyQuestEntity>>(emptyList())
    private val progressFlow = MutableStateFlow<UserProgressEntity?>(null)
    private val insightsFlow = MutableStateFlow<List<AiCoachInsightEntity>>(emptyList())

    init {
        refreshAllFlowsSync()
    }

    fun regainDao(): RegainDao = this

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_limits (
                packageName TEXT PRIMARY KEY,
                appName TEXT NOT NULL,
                category TEXT NOT NULL,
                iconKey TEXT NOT NULL,
                dailyLimitMinutes INTEGER NOT NULL,
                usedTodayMinutes INTEGER NOT NULL,
                pickupsToday INTEGER NOT NULL,
                blockedCountToday INTEGER NOT NULL,
                isStrictLocked INTEGER NOT NULL,
                isEnabled INTEGER NOT NULL,
                isBlockedCompletely INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS short_form_shields (
                id TEXT PRIMARY KEY,
                platformName TEXT NOT NULL,
                feedName TEXT NOT NULL,
                shieldMode TEXT NOT NULL,
                dailyCapMinutes INTEGER NOT NULL,
                usedMinutesToday INTEGER NOT NULL,
                interceptsToday INTEGER NOT NULL,
                isEnabled INTEGER NOT NULL,
                allowDirectMessages INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS focus_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                tag TEXT NOT NULL,
                mode TEXT NOT NULL,
                durationMinutes INTEGER NOT NULL,
                durationSeconds INTEGER NOT NULL DEFAULT 0,
                ambientSound TEXT NOT NULL,
                coinsEarned INTEGER NOT NULL,
                completedAtTimestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS schedule_blocks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                timeWindowLabel TEXT NOT NULL,
                daysSummary TEXT NOT NULL,
                targetSummary TEXT NOT NULL,
                isStrictMode INTEGER NOT NULL,
                isEnabled INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_quests (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                rewardCoins INTEGER NOT NULL,
                currentProgress INTEGER NOT NULL,
                targetProgress INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                isClaimed INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_progress (
                id INTEGER PRIMARY KEY,
                regaLevel INTEGER NOT NULL,
                regaStageName TEXT NOT NULL,
                leafCoins INTEGER NOT NULL,
                streakDays INTEGER NOT NULL,
                mindfulGoalMinutes INTEGER NOT NULL,
                savedMinutesToday INTEGER NOT NULL,
                mindfulBreathsCompleted INTEGER NOT NULL,
                strictModeGlobal INTEGER NOT NULL,
                strictPassphrase TEXT NOT NULL,
                customGeminiApiKey TEXT NOT NULL DEFAULT '',
                aiProvider TEXT NOT NULL DEFAULT 'GROQ'
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ai_coach_insights (
                dateKey TEXT PRIMARY KEY,
                requestsUsedToday INTEGER NOT NULL,
                coachHeadline TEXT NOT NULL,
                habitBottleneck TEXT NOT NULL,
                timeSavedProjectionMinutes INTEGER NOT NULL,
                microChallenge1 TEXT NOT NULL,
                microChallenge2 TEXT NOT NULL,
                microChallenge3 TEXT NOT NULL,
                promptUsedPreview TEXT NOT NULL,
                sourceMode TEXT NOT NULL,
                generatedAtTimestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
        try {
            db.execSQL("ALTER TABLE app_limits ADD COLUMN isBlockedCompletely INTEGER NOT NULL DEFAULT 0")
        } catch (_: Exception) {
        }
        try {
            db.execSQL("ALTER TABLE focus_sessions ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
        } catch (_: Exception) {
        }
        try {
            db.execSQL("ALTER TABLE user_progress ADD COLUMN customGeminiApiKey TEXT NOT NULL DEFAULT ''")
        } catch (_: Exception) {
        }
        try {
            db.execSQL("ALTER TABLE user_progress ADD COLUMN aiProvider TEXT NOT NULL DEFAULT 'GROQ'")
        } catch (_: Exception) {
        }
    }

    fun getBlockingRuleForPackageSync(packageName: String): AppLimitEntity? {
        val currentApps = appLimitsFlow.value.ifEmpty {
            val fresh = queryAppLimits(writableDatabase)
            appLimitsFlow.value = fresh
            fresh
        }
        return currentApps.firstOrNull { app ->
            app.packageName == packageName &&
                app.isEnabled &&
                (app.isBlockedCompletely || app.isStrictLocked || app.usedTodayMinutes >= app.dailyLimitMinutes)
        }
    }

    fun getShortFormShieldForPackageSync(packageName: String): ShortFormShieldEntity? {
        val shieldId = when (packageName) {
            "com.instagram.android" -> "instagram_reels"
            "com.google.android.youtube" -> "youtube_shorts"
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> "tiktok_feed"
            "com.snapchat.android" -> "snapchat_spotlight"
            "com.reddit.frontpage", "com.twitter.android" -> "reddit_popular"
            else -> null
        } ?: return null
        val currentShields = shieldsFlow.value.ifEmpty {
            val fresh = queryShields(writableDatabase)
            shieldsFlow.value = fresh
            fresh
        }
        return currentShields.firstOrNull { it.id == shieldId && it.isEnabled }
    }

    fun syncUsageStatsFromServiceSync(context: Context) {
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }
            val stats = usm.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                cal.timeInMillis,
                System.currentTimeMillis()
            ) ?: return
            val db = writableDatabase
            val currentList = appLimitsFlow.value.ifEmpty { queryAppLimits(db) }
            var changed = false
            stats.forEach { stat ->
                val match = currentList.find { it.packageName == stat.packageName }
                val minutes = (stat.totalTimeInForeground / 60000L).toInt()
                if (match != null && minutes >= 0 && match.usedTodayMinutes != minutes) {
                    val updated = match.copy(usedTodayMinutes = minutes)
                    db.insertWithOnConflict("app_limits", null, updated.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
                    changed = true
                }
            }
            if (changed) {
                appLimitsFlow.value = queryAppLimits(db)
            }
        } catch (_: Exception) {
        }
    }

    fun incrementBlockCountSync(packageName: String) {
        val current = appLimitsFlow.value.firstOrNull { it.packageName == packageName } ?: return
        val updated = current.copy(blockedCountToday = current.blockedCountToday + 1)
        val db = writableDatabase
        db.insertWithOnConflict("app_limits", null, updated.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        appLimitsFlow.value = queryAppLimits(db)
    }

    fun incrementShortFormInterceptSync(shieldId: String) {
        val current = shieldsFlow.value.firstOrNull { it.id == shieldId } ?: return
        val updated = current.copy(interceptsToday = current.interceptsToday + 1)
        val db = writableDatabase
        db.insertWithOnConflict("short_form_shields", null, updated.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        shieldsFlow.value = queryShields(db)
    }

    fun getStrictPassphraseSync(): String {
        return progressFlow.value?.strictPassphrase ?: "I choose mindful presence"
    }

    fun recordMindfulInterceptVictorySync() {
        val db = writableDatabase
        val currentProgress = progressFlow.value ?: queryUserProgress(db) ?: UserProgressEntity()
        val updatedCoins = currentProgress.leafCoins + 15
        val newLevel = (updatedCoins / 120).coerceAtLeast(1)
        val updatedProgress = currentProgress.copy(
            leafCoins = updatedCoins,
            regaLevel = newLevel,
            savedMinutesToday = currentProgress.savedMinutesToday + 8,
            mindfulBreathsCompleted = currentProgress.mindfulBreathsCompleted + 1
        )
        db.insertWithOnConflict("user_progress", null, updatedProgress.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        progressFlow.value = queryUserProgress(db)

        val breathQuest = questsFlow.value.find { it.id == "quest_mindful_breath" }
        if (breathQuest != null && !breathQuest.isCompleted) {
            val nextProg = (breathQuest.currentProgress + 1).coerceAtMost(breathQuest.targetProgress)
            val updatedQuest = breathQuest.copy(
                currentProgress = nextProg,
                isCompleted = nextProg >= breathQuest.targetProgress
            )
            db.insertWithOnConflict("daily_quests", null, updatedQuest.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            questsFlow.value = queryQuests(db)
        }
    }

    fun recordFiveMinGraceBreathSync() {
        val db = writableDatabase
        val currentProgress = progressFlow.value ?: queryUserProgress(db) ?: UserProgressEntity()
        val updatedProgress = currentProgress.copy(
            mindfulBreathsCompleted = currentProgress.mindfulBreathsCompleted + 1
        )
        db.insertWithOnConflict("user_progress", null, updatedProgress.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        progressFlow.value = queryUserProgress(db)
    }

    private fun refreshAllFlowsSync() {
        val db = writableDatabase
        appLimitsFlow.value = queryAppLimits(db)
        shieldsFlow.value = queryShields(db)
        sessionsFlow.value = querySessions(db)
        schedulesFlow.value = querySchedules(db)
        questsFlow.value = queryQuests(db)
        progressFlow.value = queryUserProgress(db)
        insightsFlow.value = queryInsights(db)
    }

    private fun queryAppLimits(db: SQLiteDatabase): List<AppLimitEntity> {
        val list = mutableListOf<AppLimitEntity>()
        db.rawQuery("SELECT * FROM app_limits ORDER BY isBlockedCompletely DESC, usedTodayMinutes DESC", null).use { c ->
            val blockedCol = c.getColumnIndex("isBlockedCompletely")
            while (c.moveToNext()) {
                list.add(
                    AppLimitEntity(
                        packageName = c.getString(c.getColumnIndexOrThrow("packageName")),
                        appName = c.getString(c.getColumnIndexOrThrow("appName")),
                        category = c.getString(c.getColumnIndexOrThrow("category")),
                        iconKey = c.getString(c.getColumnIndexOrThrow("iconKey")),
                        dailyLimitMinutes = c.getInt(c.getColumnIndexOrThrow("dailyLimitMinutes")),
                        usedTodayMinutes = c.getInt(c.getColumnIndexOrThrow("usedTodayMinutes")),
                        pickupsToday = c.getInt(c.getColumnIndexOrThrow("pickupsToday")),
                        blockedCountToday = c.getInt(c.getColumnIndexOrThrow("blockedCountToday")),
                        isStrictLocked = c.getInt(c.getColumnIndexOrThrow("isStrictLocked")) != 0,
                        isEnabled = c.getInt(c.getColumnIndexOrThrow("isEnabled")) != 0,
                        isBlockedCompletely = if (blockedCol >= 0) c.getInt(blockedCol) != 0 else false
                    )
                )
            }
        }
        return list
    }

    private fun queryShields(db: SQLiteDatabase): List<ShortFormShieldEntity> {
        val list = mutableListOf<ShortFormShieldEntity>()
        db.rawQuery("SELECT * FROM short_form_shields ORDER BY interceptsToday DESC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    ShortFormShieldEntity(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        platformName = c.getString(c.getColumnIndexOrThrow("platformName")),
                        feedName = c.getString(c.getColumnIndexOrThrow("feedName")),
                        shieldMode = c.getString(c.getColumnIndexOrThrow("shieldMode")),
                        dailyCapMinutes = c.getInt(c.getColumnIndexOrThrow("dailyCapMinutes")),
                        usedMinutesToday = c.getInt(c.getColumnIndexOrThrow("usedMinutesToday")),
                        interceptsToday = c.getInt(c.getColumnIndexOrThrow("interceptsToday")),
                        isEnabled = c.getInt(c.getColumnIndexOrThrow("isEnabled")) != 0,
                        allowDirectMessages = c.getInt(c.getColumnIndexOrThrow("allowDirectMessages")) != 0
                    )
                )
            }
        }
        return list
    }

    private fun querySessions(db: SQLiteDatabase): List<FocusSessionEntity> {
        val list = mutableListOf<FocusSessionEntity>()
        db.rawQuery("SELECT * FROM focus_sessions ORDER BY completedAtTimestamp DESC", null).use { c ->
            val secCol = c.getColumnIndex("durationSeconds")
            while (c.moveToNext()) {
                val mins = c.getInt(c.getColumnIndexOrThrow("durationMinutes"))
                val secs = if (secCol >= 0) c.getInt(secCol) else mins * 60
                list.add(
                    FocusSessionEntity(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        title = c.getString(c.getColumnIndexOrThrow("title")),
                        tag = c.getString(c.getColumnIndexOrThrow("tag")),
                        mode = c.getString(c.getColumnIndexOrThrow("mode")),
                        durationMinutes = mins,
                        durationSeconds = secs,
                        ambientSound = c.getString(c.getColumnIndexOrThrow("ambientSound")),
                        coinsEarned = c.getInt(c.getColumnIndexOrThrow("coinsEarned")),
                        completedAtTimestamp = c.getLong(c.getColumnIndexOrThrow("completedAtTimestamp"))
                    )
                )
            }
        }
        return list
    }

    private fun querySchedules(db: SQLiteDatabase): List<ScheduleBlockEntity> {
        val list = mutableListOf<ScheduleBlockEntity>()
        db.rawQuery("SELECT * FROM schedule_blocks ORDER BY id ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    ScheduleBlockEntity(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        title = c.getString(c.getColumnIndexOrThrow("title")),
                        timeWindowLabel = c.getString(c.getColumnIndexOrThrow("timeWindowLabel")),
                        daysSummary = c.getString(c.getColumnIndexOrThrow("daysSummary")),
                        targetSummary = c.getString(c.getColumnIndexOrThrow("targetSummary")),
                        isStrictMode = c.getInt(c.getColumnIndexOrThrow("isStrictMode")) != 0,
                        isEnabled = c.getInt(c.getColumnIndexOrThrow("isEnabled")) != 0
                    )
                )
            }
        }
        return list
    }

    private fun queryQuests(db: SQLiteDatabase): List<DailyQuestEntity> {
        val list = mutableListOf<DailyQuestEntity>()
        db.rawQuery("SELECT * FROM daily_quests ORDER BY id ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    DailyQuestEntity(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        title = c.getString(c.getColumnIndexOrThrow("title")),
                        subtitle = c.getString(c.getColumnIndexOrThrow("subtitle")),
                        rewardCoins = c.getInt(c.getColumnIndexOrThrow("rewardCoins")),
                        currentProgress = c.getInt(c.getColumnIndexOrThrow("currentProgress")),
                        targetProgress = c.getInt(c.getColumnIndexOrThrow("targetProgress")),
                        isCompleted = c.getInt(c.getColumnIndexOrThrow("isCompleted")) != 0,
                        isClaimed = c.getInt(c.getColumnIndexOrThrow("isClaimed")) != 0
                    )
                )
            }
        }
        return list
    }

    private fun queryUserProgress(db: SQLiteDatabase): UserProgressEntity? {
        db.rawQuery("SELECT * FROM user_progress WHERE id = 1 LIMIT 1", null).use { c ->
            if (c.moveToFirst()) {
                val apiKeyCol = c.getColumnIndex("customGeminiApiKey")
                val providerCol = c.getColumnIndex("aiProvider")
                return UserProgressEntity(
                    id = c.getInt(c.getColumnIndexOrThrow("id")),
                    regaLevel = c.getInt(c.getColumnIndexOrThrow("regaLevel")),
                    regaStageName = c.getString(c.getColumnIndexOrThrow("regaStageName")),
                    leafCoins = c.getInt(c.getColumnIndexOrThrow("leafCoins")),
                    streakDays = c.getInt(c.getColumnIndexOrThrow("streakDays")),
                    mindfulGoalMinutes = c.getInt(c.getColumnIndexOrThrow("mindfulGoalMinutes")),
                    savedMinutesToday = c.getInt(c.getColumnIndexOrThrow("savedMinutesToday")),
                    mindfulBreathsCompleted = c.getInt(c.getColumnIndexOrThrow("mindfulBreathsCompleted")),
                    strictModeGlobal = c.getInt(c.getColumnIndexOrThrow("strictModeGlobal")) != 0,
                    strictPassphrase = c.getString(c.getColumnIndexOrThrow("strictPassphrase")),
                    customGeminiApiKey = if (apiKeyCol >= 0) c.getString(apiKeyCol).orEmpty() else "",
                    aiProvider = if (providerCol >= 0) c.getString(providerCol).orEmpty().ifBlank { "GROQ" } else "GROQ"
                )
            }
        }
        return null
    }

    private fun queryInsights(db: SQLiteDatabase): List<AiCoachInsightEntity> {
        val list = mutableListOf<AiCoachInsightEntity>()
        db.rawQuery("SELECT * FROM ai_coach_insights ORDER BY dateKey DESC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    AiCoachInsightEntity(
                        dateKey = c.getString(c.getColumnIndexOrThrow("dateKey")),
                        requestsUsedToday = c.getInt(c.getColumnIndexOrThrow("requestsUsedToday")),
                        coachHeadline = c.getString(c.getColumnIndexOrThrow("coachHeadline")),
                        habitBottleneck = c.getString(c.getColumnIndexOrThrow("habitBottleneck")),
                        timeSavedProjectionMinutes = c.getInt(c.getColumnIndexOrThrow("timeSavedProjectionMinutes")),
                        microChallenge1 = c.getString(c.getColumnIndexOrThrow("microChallenge1")),
                        microChallenge2 = c.getString(c.getColumnIndexOrThrow("microChallenge2")),
                        microChallenge3 = c.getString(c.getColumnIndexOrThrow("microChallenge3")),
                        promptUsedPreview = c.getString(c.getColumnIndexOrThrow("promptUsedPreview")),
                        sourceMode = c.getString(c.getColumnIndexOrThrow("sourceMode")),
                        generatedAtTimestamp = c.getLong(c.getColumnIndexOrThrow("generatedAtTimestamp"))
                    )
                )
            }
        }
        return list
    }

    override fun getAllAppLimits(): Flow<List<AppLimitEntity>> = appLimitsFlow.asStateFlow()

    override suspend fun insertAppLimits(items: List<AppLimitEntity>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            items.forEach { item ->
                db.insertWithOnConflict("app_limits", null, item.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        appLimitsFlow.value = queryAppLimits(db)
    }

    override suspend fun updateAppLimit(item: AppLimitEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.insertWithOnConflict("app_limits", null, item.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        appLimitsFlow.value = queryAppLimits(db)
    }

    override suspend fun deleteAppLimit(packageName: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("app_limits", "packageName = ?", arrayOf(packageName))
        appLimitsFlow.value = queryAppLimits(db)
    }

    override fun getAllShortFormShields(): Flow<List<ShortFormShieldEntity>> = shieldsFlow.asStateFlow()

    override suspend fun insertShortFormShields(items: List<ShortFormShieldEntity>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            items.forEach { item ->
                db.insertWithOnConflict("short_form_shields", null, item.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        shieldsFlow.value = queryShields(db)
    }

    override suspend fun updateShortFormShield(item: ShortFormShieldEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.insertWithOnConflict("short_form_shields", null, item.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        shieldsFlow.value = queryShields(db)
    }

    override fun getAllFocusSessions(): Flow<List<FocusSessionEntity>> = sessionsFlow.asStateFlow()

    override suspend fun insertFocusSession(item: FocusSessionEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.insertWithOnConflict("focus_sessions", null, item.toContentValues(includeId = item.id != 0L), SQLiteDatabase.CONFLICT_REPLACE)
        sessionsFlow.value = querySessions(db)
    }

    override suspend fun insertFocusSessions(items: List<FocusSessionEntity>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            items.forEach { item ->
                db.insertWithOnConflict("focus_sessions", null, item.toContentValues(includeId = item.id != 0L), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        sessionsFlow.value = querySessions(db)
    }

    override fun getAllSchedules(): Flow<List<ScheduleBlockEntity>> = schedulesFlow.asStateFlow()

    override suspend fun insertSchedules(items: List<ScheduleBlockEntity>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            items.forEach { item ->
                db.insertWithOnConflict("schedule_blocks", null, item.toContentValues(includeId = item.id != 0L), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        schedulesFlow.value = querySchedules(db)
    }

    override suspend fun insertSchedule(item: ScheduleBlockEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.insertWithOnConflict("schedule_blocks", null, item.toContentValues(includeId = item.id != 0L), SQLiteDatabase.CONFLICT_REPLACE)
        schedulesFlow.value = querySchedules(db)
    }

    override suspend fun updateSchedule(item: ScheduleBlockEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.insertWithOnConflict("schedule_blocks", null, item.toContentValues(includeId = true), SQLiteDatabase.CONFLICT_REPLACE)
        schedulesFlow.value = querySchedules(db)
    }

    override suspend fun deleteSchedule(id: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("schedule_blocks", "id = ?", arrayOf(id.toString()))
        schedulesFlow.value = querySchedules(db)
    }

    override fun getAllDailyQuests(): Flow<List<DailyQuestEntity>> = questsFlow.asStateFlow()

    override suspend fun insertDailyQuests(items: List<DailyQuestEntity>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            items.forEach { item ->
                db.insertWithOnConflict("daily_quests", null, item.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        questsFlow.value = queryQuests(db)
    }

    override suspend fun updateDailyQuest(item: DailyQuestEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.insertWithOnConflict("daily_quests", null, item.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        questsFlow.value = queryQuests(db)
    }

    override fun getUserProgress(): Flow<UserProgressEntity?> = progressFlow.asStateFlow()

    override suspend fun upsertUserProgress(progress: UserProgressEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.insertWithOnConflict("user_progress", null, progress.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        progressFlow.value = queryUserProgress(db)
    }

    override fun getAllAiInsights(): Flow<List<AiCoachInsightEntity>> = insightsFlow.asStateFlow()

    override suspend fun getAiInsightForDate(dateKey: String): AiCoachInsightEntity? = withContext(Dispatchers.IO) {
        insightsFlow.value.firstOrNull { it.dateKey == dateKey }
    }

    override suspend fun upsertAiInsight(insight: AiCoachInsightEntity) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.insertWithOnConflict("ai_coach_insights", null, insight.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        insightsFlow.value = queryInsights(db)
    }

    companion object {
        @Volatile
        private var INSTANCE: RegainDatabase? = null

        fun getDatabase(context: Context): RegainDatabase {
            val appCtx = context.applicationContext
            val current = INSTANCE
            if (current != null && current.appContext === appCtx) {
                return current
            }
            return synchronized(this) {
                val existing = INSTANCE
                if (existing != null && existing.appContext === appCtx) {
                    existing
                } else {
                    val instance = RegainDatabase(appCtx)
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
}

private fun AppLimitEntity.toContentValues() = ContentValues().apply {
    put("packageName", packageName)
    put("appName", appName)
    put("category", category)
    put("iconKey", iconKey)
    put("dailyLimitMinutes", dailyLimitMinutes)
    put("usedTodayMinutes", usedTodayMinutes)
    put("pickupsToday", pickupsToday)
    put("blockedCountToday", blockedCountToday)
    put("isStrictLocked", if (isStrictLocked) 1 else 0)
    put("isEnabled", if (isEnabled) 1 else 0)
    put("isBlockedCompletely", if (isBlockedCompletely) 1 else 0)
}

private fun ShortFormShieldEntity.toContentValues() = ContentValues().apply {
    put("id", id)
    put("platformName", platformName)
    put("feedName", feedName)
    put("shieldMode", shieldMode)
    put("dailyCapMinutes", dailyCapMinutes)
    put("usedMinutesToday", usedMinutesToday)
    put("interceptsToday", interceptsToday)
    put("isEnabled", if (isEnabled) 1 else 0)
    put("allowDirectMessages", if (allowDirectMessages) 1 else 0)
}

private fun FocusSessionEntity.toContentValues(includeId: Boolean) = ContentValues().apply {
    if (includeId) put("id", id)
    put("title", title)
    put("tag", tag)
    put("mode", mode)
    put("durationMinutes", durationMinutes)
    put("durationSeconds", durationSeconds)
    put("ambientSound", ambientSound)
    put("coinsEarned", coinsEarned)
    put("completedAtTimestamp", completedAtTimestamp)
}

private fun ScheduleBlockEntity.toContentValues(includeId: Boolean) = ContentValues().apply {
    if (includeId) put("id", id)
    put("title", title)
    put("timeWindowLabel", timeWindowLabel)
    put("daysSummary", daysSummary)
    put("targetSummary", targetSummary)
    put("isStrictMode", if (isStrictMode) 1 else 0)
    put("isEnabled", if (isEnabled) 1 else 0)
}

private fun DailyQuestEntity.toContentValues() = ContentValues().apply {
    put("id", id)
    put("title", title)
    put("subtitle", subtitle)
    put("rewardCoins", rewardCoins)
    put("currentProgress", currentProgress)
    put("targetProgress", targetProgress)
    put("isCompleted", if (isCompleted) 1 else 0)
    put("isClaimed", if (isClaimed) 1 else 0)
}

private fun UserProgressEntity.toContentValues() = ContentValues().apply {
    put("id", id)
    put("regaLevel", regaLevel)
    put("regaStageName", regaStageName)
    put("leafCoins", leafCoins)
    put("streakDays", streakDays)
    put("mindfulGoalMinutes", mindfulGoalMinutes)
    put("savedMinutesToday", savedMinutesToday)
    put("mindfulBreathsCompleted", mindfulBreathsCompleted)
    put("strictModeGlobal", if (strictModeGlobal) 1 else 0)
    put("strictPassphrase", strictPassphrase)
    put("customGeminiApiKey", customGeminiApiKey)
    put("aiProvider", aiProvider)
}

private fun AiCoachInsightEntity.toContentValues() = ContentValues().apply {
    put("dateKey", dateKey)
    put("requestsUsedToday", requestsUsedToday)
    put("coachHeadline", coachHeadline)
    put("habitBottleneck", habitBottleneck)
    put("timeSavedProjectionMinutes", timeSavedProjectionMinutes)
    put("microChallenge1", microChallenge1)
    put("microChallenge2", microChallenge2)
    put("microChallenge3", microChallenge3)
    put("promptUsedPreview", promptUsedPreview)
    put("sourceMode", sourceMode)
    put("generatedAtTimestamp", generatedAtTimestamp)
}

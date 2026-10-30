package com.example.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegainRepository(
    private val dao: RegainDao,
    private val appContext: Context
) {
    val appLimits: Flow<List<AppLimitEntity>> = dao.getAllAppLimits()
    val shortFormShields: Flow<List<ShortFormShieldEntity>> = dao.getAllShortFormShields()
    val focusSessions: Flow<List<FocusSessionEntity>> = dao.getAllFocusSessions()
    val schedules: Flow<List<ScheduleBlockEntity>> = dao.getAllSchedules()
    val dailyQuests: Flow<List<DailyQuestEntity>> = dao.getAllDailyQuests()
    val userProgress: Flow<UserProgressEntity?> = dao.getUserProgress()
    val aiInsights: Flow<List<AiCoachInsightEntity>> = dao.getAllAiInsights()

    fun todayDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                appContext.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    suspend fun ensureSeededAndSyncSystemUsage() {
        val existingProgress = dao.getUserProgress().firstOrNull()
        if (existingProgress == null) {
            dao.upsertUserProgress(
                UserProgressEntity(
                    id = 1,
                    regaLevel = 3,
                    regaStageName = "Sprouting Sage Spirit",
                    leafCoins = 340,
                    streakDays = 6,
                    mindfulGoalMinutes = 150,
                    savedMinutesToday = 74,
                    mindfulBreathsCompleted = 9,
                    strictModeGlobal = false
                )
            )
        }

        val currentApps = dao.getAllAppLimits().firstOrNull()
        if (currentApps.isNullOrEmpty()) {
            dao.insertAppLimits(
                listOf(
                    AppLimitEntity(
                        packageName = "com.instagram.android",
                        appName = "Instagram",
                        category = "Social & Reels",
                        iconKey = "instagram",
                        dailyLimitMinutes = 35,
                        usedTodayMinutes = 31,
                        pickupsToday = 18,
                        blockedCountToday = 4,
                        isStrictLocked = true,
                        isEnabled = true
                    ),
                    AppLimitEntity(
                        packageName = "com.google.android.youtube",
                        appName = "YouTube",
                        category = "Video & Shorts",
                        iconKey = "youtube",
                        dailyLimitMinutes = 45,
                        usedTodayMinutes = 38,
                        pickupsToday = 12,
                        blockedCountToday = 3,
                        isStrictLocked = false,
                        isEnabled = true
                    ),
                    AppLimitEntity(
                        packageName = "com.zhiliaoapp.musically",
                        appName = "TikTok",
                        category = "Short-Form Video",
                        iconKey = "tiktok",
                        dailyLimitMinutes = 20,
                        usedTodayMinutes = 20,
                        pickupsToday = 15,
                        blockedCountToday = 7,
                        isStrictLocked = true,
                        isEnabled = true
                    ),
                    AppLimitEntity(
                        packageName = "com.twitter.android",
                        appName = "X (Twitter)",
                        category = "News & Feeds",
                        iconKey = "x",
                        dailyLimitMinutes = 25,
                        usedTodayMinutes = 14,
                        pickupsToday = 9,
                        blockedCountToday = 1,
                        isStrictLocked = false,
                        isEnabled = true
                    ),
                    AppLimitEntity(
                        packageName = "com.reddit.frontpage",
                        appName = "Reddit",
                        category = "Forums & Threads",
                        iconKey = "reddit",
                        dailyLimitMinutes = 30,
                        usedTodayMinutes = 19,
                        pickupsToday = 7,
                        blockedCountToday = 2,
                        isStrictLocked = false,
                        isEnabled = true
                    ),
                    AppLimitEntity(
                        packageName = "com.snapchat.android",
                        appName = "Snapchat",
                        category = "Messaging & Spotlight",
                        iconKey = "snapchat",
                        dailyLimitMinutes = 25,
                        usedTodayMinutes = 11,
                        pickupsToday = 10,
                        blockedCountToday = 1,
                        isStrictLocked = false,
                        isEnabled = false
                    )
                )
            )
        }

        // Merge real device UsageStats if permission is granted
        if (hasUsageStatsPermission()) {
            try {
                val usm = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val stats = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    cal.timeInMillis,
                    System.currentTimeMillis()
                )
                val currentList = dao.getAllAppLimits().firstOrNull().orEmpty()
                stats?.forEach { stat ->
                    val match = currentList.find { it.packageName == stat.packageName }
                    val minutes = (stat.totalTimeInForeground / 60000L).toInt()
                    if (match != null && minutes > 0) {
                        dao.updateAppLimit(match.copy(usedTodayMinutes = minutes))
                    }
                }
            } catch (_: Exception) {
                // Keep local Room records if UsageStats query fails
            }
        }

        val currentShields = dao.getAllShortFormShields().firstOrNull()
        if (currentShields.isNullOrEmpty()) {
            dao.insertShortFormShields(
                listOf(
                    ShortFormShieldEntity(
                        id = "instagram_reels",
                        platformName = "Instagram",
                        feedName = "Reels Tab & Infinite Explore",
                        shieldMode = "BLOCK_COMPLETELY",
                        dailyCapMinutes = 0,
                        usedMinutesToday = 0,
                        interceptsToday = 9,
                        isEnabled = true,
                        allowDirectMessages = true
                    ),
                    ShortFormShieldEntity(
                        id = "youtube_shorts",
                        platformName = "YouTube",
                        feedName = "Shorts Shelf & Vertical Feed",
                        shieldMode = "MINDFUL_BREATH_PAUSE",
                        dailyCapMinutes = 15,
                        usedMinutesToday = 6,
                        interceptsToday = 6,
                        isEnabled = true,
                        allowDirectMessages = true
                    ),
                    ShortFormShieldEntity(
                        id = "tiktok_feed",
                        platformName = "TikTok",
                        feedName = "For You Infinite Scroll",
                        shieldMode = "BLOCK_COMPLETELY",
                        dailyCapMinutes = 0,
                        usedMinutesToday = 0,
                        interceptsToday = 11,
                        isEnabled = true,
                        allowDirectMessages = false
                    ),
                    ShortFormShieldEntity(
                        id = "snapchat_spotlight",
                        platformName = "Snapchat",
                        feedName = "Spotlight & Discover Stories",
                        shieldMode = "DAILY_CAP_15M",
                        dailyCapMinutes = 15,
                        usedMinutesToday = 8,
                        interceptsToday = 3,
                        isEnabled = true,
                        allowDirectMessages = true
                    ),
                    ShortFormShieldEntity(
                        id = "reddit_popular",
                        platformName = "Reddit & X",
                        feedName = "Algorithmic Popular / For You",
                        shieldMode = "MINDFUL_BREATH_PAUSE",
                        dailyCapMinutes = 20,
                        usedMinutesToday = 12,
                        interceptsToday = 4,
                        isEnabled = true,
                        allowDirectMessages = true
                    )
                )
            )
        }

        val currentSessions = dao.getAllFocusSessions().firstOrNull()
        if (currentSessions.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            dao.insertFocusSessions(
                listOf(
                    FocusSessionEntity(
                        title = "Morning Deep Work Sprint",
                        tag = "Coding & Build",
                        mode = "POMODORO",
                        durationMinutes = 50,
                        ambientSound = "Zen Rain",
                        coinsEarned = 50,
                        completedAtTimestamp = now - 3 * 3600_000L
                    ),
                    FocusSessionEntity(
                        title = "Chapter 4 Organic Chemistry",
                        tag = "Deep Study",
                        mode = "POMODORO",
                        durationMinutes = 25,
                        ambientSound = "Brown Noise",
                        coinsEarned = 25,
                        completedAtTimestamp = now - 7 * 3600_000L
                    ),
                    FocusSessionEntity(
                        title = "Uninterrupted Evening Reading",
                        tag = "Mindful Reading",
                        mode = "COUNTDOWN",
                        durationMinutes = 30,
                        ambientSound = "Forest Wind",
                        coinsEarned = 30,
                        completedAtTimestamp = now - 22 * 3600_000L
                    )
                )
            )
        }

        val currentSchedules = dao.getAllSchedules().firstOrNull()
        if (currentSchedules.isNullOrEmpty()) {
            dao.insertSchedules(
                listOf(
                    ScheduleBlockEntity(
                        title = "Morning Mindful Awakening",
                        timeWindowLabel = "06:30 – 09:00",
                        daysSummary = "Every Day",
                        targetSummary = "All Social Media & Short-Form Feeds",
                        isStrictMode = true,
                        isEnabled = true
                    ),
                    ScheduleBlockEntity(
                        title = "Deep Work & Study Sanctuary",
                        timeWindowLabel = "09:30 – 13:00",
                        daysSummary = "Mon · Tue · Wed · Thu · Fri",
                        targetSummary = "Reels, Shorts, TikTok & Games",
                        isStrictMode = true,
                        isEnabled = true
                    ),
                    ScheduleBlockEntity(
                        title = "Digital Sunset & Sleep Wind-Down",
                        timeWindowLabel = "22:00 – 06:00",
                        daysSummary = "Every Night",
                        targetSummary = "All Entertainment & Infinite Scroll",
                        isStrictMode = false,
                        isEnabled = true
                    )
                )
            )
        }

        val currentQuests = dao.getAllDailyQuests().firstOrNull()
        if (currentQuests.isNullOrEmpty()) {
            dao.insertDailyQuests(
                listOf(
                    DailyQuestEntity(
                        id = "quest_focus_25",
                        title = "Complete 1 Deep Focus Session",
                        subtitle = "Run the Pomodoro or Countdown timer to nurture Rega",
                        rewardCoins = 40,
                        currentProgress = 1,
                        targetProgress = 1,
                        isCompleted = true,
                        isClaimed = false
                    ),
                    DailyQuestEntity(
                        id = "quest_reels_shield",
                        title = "Keep Reels & Shorts Shield Active",
                        subtitle = "Block infinite vertical video feeds while keeping DMs open",
                        rewardCoins = 35,
                        currentProgress = 4,
                        targetProgress = 4,
                        isCompleted = true,
                        isClaimed = false
                    ),
                    DailyQuestEntity(
                        id = "quest_mindful_breath",
                        title = "Practice 3 Mindful Breath Pauses",
                        subtitle = "Complete a 4-4-4 breathing pause instead of impulse scrolling",
                        rewardCoins = 50,
                        currentProgress = 2,
                        targetProgress = 3,
                        isCompleted = false,
                        isClaimed = false
                    )
                )
            )
        }
    }

    suspend fun updateAppLimit(entity: AppLimitEntity) = dao.updateAppLimit(entity)
    suspend fun updateShortFormShield(entity: ShortFormShieldEntity) = dao.updateShortFormShield(entity)
    suspend fun insertFocusSession(session: FocusSessionEntity) = dao.insertFocusSession(session)
    suspend fun updateSchedule(schedule: ScheduleBlockEntity) = dao.updateSchedule(schedule)
    suspend fun insertSchedule(schedule: ScheduleBlockEntity) = dao.insertSchedule(schedule)
    suspend fun deleteSchedule(id: Long) = dao.deleteSchedule(id)
    suspend fun updateDailyQuest(quest: DailyQuestEntity) = dao.updateDailyQuest(quest)
    suspend fun upsertUserProgress(progress: UserProgressEntity) = dao.upsertUserProgress(progress)
    suspend fun getAiInsightForDate(dateKey: String) = dao.getAiInsightForDate(dateKey)
    suspend fun upsertAiInsight(insight: AiCoachInsightEntity) = dao.upsertAiInsight(insight)
}

package com.example.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SelectableDeviceApp(
    val packageName: String,
    val appName: String,
    val category: String
)

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

    suspend fun getDiscoverableAppsForBlocker(): List<SelectableDeviceApp> = withContext(Dispatchers.IO) {
        val discovered = mutableMapOf<String, SelectableDeviceApp>()

        // 1. Query real installed launchable apps on the user's device
        try {
            val pm = appContext.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            for (info in resolveInfos) {
                val pkg = info.activityInfo?.packageName ?: continue
                if (pkg == appContext.packageName) continue
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                if (label.isNotBlank()) {
                    discovered[pkg] = SelectableDeviceApp(
                        packageName = pkg,
                        appName = label,
                        category = "Installed Device App"
                    )
                }
            }
        } catch (_: Exception) {
        }

        // 2. Include popular distracting apps catalog so user can pre-block any app
        val catalog = listOf(
            SelectableDeviceApp("com.instagram.android", "Instagram", "Social & Reels"),
            SelectableDeviceApp("com.google.android.youtube", "YouTube", "Video & Shorts"),
            SelectableDeviceApp("com.zhiliaoapp.musically", "TikTok", "Short-Form Video"),
            SelectableDeviceApp("com.twitter.android", "X (Twitter)", "News & Feeds"),
            SelectableDeviceApp("com.reddit.frontpage", "Reddit", "Forums & Threads"),
            SelectableDeviceApp("com.snapchat.android", "Snapchat", "Messaging & Spotlight"),
            SelectableDeviceApp("com.facebook.katana", "Facebook", "Social Feed & Reels"),
            SelectableDeviceApp("com.netflix.mediaclient", "Netflix", "Video Streaming"),
            SelectableDeviceApp("com.discord", "Discord", "Communities & Chat"),
            SelectableDeviceApp("tv.twitch.android.app", "Twitch", "Live Streams"),
            SelectableDeviceApp("com.pinterest", "Pinterest", "Visual Discovery"),
            SelectableDeviceApp("com.whatsapp", "WhatsApp", "Messaging"),
            SelectableDeviceApp("org.telegram.messenger", "Telegram", "Channels & Chat"),
            SelectableDeviceApp("com.supercell.clashroyale", "Clash Royale", "Mobile Gaming"),
            SelectableDeviceApp("com.roblox.client", "Roblox", "Mobile Gaming")
        )
        for (item in catalog) {
            if (!discovered.containsKey(item.packageName)) {
                discovered[item.packageName] = item
            }
        }

        discovered.values.sortedBy { it.appName.lowercase(Locale.US) }
    }

    suspend fun ensureSeededAndSyncSystemUsage() {
        val existingProgress = dao.getUserProgress().firstOrNull()
        if (existingProgress == null) {
            dao.upsertUserProgress(
                UserProgressEntity(
                    id = 1,
                    regaLevel = 1,
                    regaStageName = "Mindful Pebble Seed",
                    leafCoins = 0,
                    streakDays = 0,
                    mindfulGoalMinutes = 120,
                    savedMinutesToday = 0,
                    mindfulBreathsCompleted = 0,
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
                        dailyLimitMinutes = 30,
                        usedTodayMinutes = 0,
                        pickupsToday = 0,
                        blockedCountToday = 0,
                        isStrictLocked = true,
                        isEnabled = true,
                        isBlockedCompletely = false
                    ),
                    AppLimitEntity(
                        packageName = "com.google.android.youtube",
                        appName = "YouTube",
                        category = "Video & Shorts",
                        iconKey = "youtube",
                        dailyLimitMinutes = 45,
                        usedTodayMinutes = 0,
                        pickupsToday = 0,
                        blockedCountToday = 0,
                        isStrictLocked = false,
                        isEnabled = true,
                        isBlockedCompletely = false
                    ),
                    AppLimitEntity(
                        packageName = "com.zhiliaoapp.musically",
                        appName = "TikTok",
                        category = "Short-Form Video",
                        iconKey = "tiktok",
                        dailyLimitMinutes = 20,
                        usedTodayMinutes = 0,
                        pickupsToday = 0,
                        blockedCountToday = 0,
                        isStrictLocked = true,
                        isEnabled = true,
                        isBlockedCompletely = true
                    ),
                    AppLimitEntity(
                        packageName = "com.twitter.android",
                        appName = "X (Twitter)",
                        category = "News & Feeds",
                        iconKey = "x",
                        dailyLimitMinutes = 25,
                        usedTodayMinutes = 0,
                        pickupsToday = 0,
                        blockedCountToday = 0,
                        isStrictLocked = false,
                        isEnabled = true,
                        isBlockedCompletely = false
                    ),
                    AppLimitEntity(
                        packageName = "com.reddit.frontpage",
                        appName = "Reddit",
                        category = "Forums & Threads",
                        iconKey = "reddit",
                        dailyLimitMinutes = 30,
                        usedTodayMinutes = 0,
                        pickupsToday = 0,
                        blockedCountToday = 0,
                        isStrictLocked = false,
                        isEnabled = true,
                        isBlockedCompletely = false
                    ),
                    AppLimitEntity(
                        packageName = "com.snapchat.android",
                        appName = "Snapchat",
                        category = "Messaging & Spotlight",
                        iconKey = "snapchat",
                        dailyLimitMinutes = 25,
                        usedTodayMinutes = 0,
                        pickupsToday = 0,
                        blockedCountToday = 0,
                        isStrictLocked = false,
                        isEnabled = false,
                        isBlockedCompletely = false
                    )
                )
            )
        }

        syncSystemUsageNow()

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
                        interceptsToday = 0,
                        isEnabled = true,
                        allowDirectMessages = true
                    ),
                    ShortFormShieldEntity(
                        id = "youtube_shorts",
                        platformName = "YouTube",
                        feedName = "Shorts Shelf & Vertical Feed",
                        shieldMode = "MINDFUL_BREATH_PAUSE",
                        dailyCapMinutes = 15,
                        usedMinutesToday = 0,
                        interceptsToday = 0,
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
                        interceptsToday = 0,
                        isEnabled = true,
                        allowDirectMessages = false
                    ),
                    ShortFormShieldEntity(
                        id = "snapchat_spotlight",
                        platformName = "Snapchat",
                        feedName = "Spotlight & Discover Stories",
                        shieldMode = "DAILY_CAP_15M",
                        dailyCapMinutes = 15,
                        usedMinutesToday = 0,
                        interceptsToday = 0,
                        isEnabled = true,
                        allowDirectMessages = true
                    ),
                    ShortFormShieldEntity(
                        id = "reddit_popular",
                        platformName = "Reddit & X",
                        feedName = "Algorithmic Popular / For You",
                        shieldMode = "MINDFUL_BREATH_PAUSE",
                        dailyCapMinutes = 20,
                        usedMinutesToday = 0,
                        interceptsToday = 0,
                        isEnabled = true,
                        allowDirectMessages = true
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
                        currentProgress = 0,
                        targetProgress = 1,
                        isCompleted = false,
                        isClaimed = false
                    ),
                    DailyQuestEntity(
                        id = "quest_reels_shield",
                        title = "Block an App or Activate Reels Shield",
                        subtitle = "Choose an app to block or toggle a short-form feed shield",
                        rewardCoins = 35,
                        currentProgress = 0,
                        targetProgress = 1,
                        isCompleted = false,
                        isClaimed = false
                    ),
                    DailyQuestEntity(
                        id = "quest_mindful_breath",
                        title = "Practice 3 Mindful Breath Pauses",
                        subtitle = "Complete a 4-4-4 breathing pause instead of impulse scrolling",
                        rewardCoins = 50,
                        currentProgress = 0,
                        targetProgress = 3,
                        isCompleted = false,
                        isClaimed = false
                    )
                )
            )
        }
    }

    suspend fun syncSystemUsageNow() {
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
                    if (match != null && minutes >= 0) {
                        dao.updateAppLimit(match.copy(usedTodayMinutes = minutes))
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun updateAppLimit(entity: AppLimitEntity) = dao.updateAppLimit(entity)
    suspend fun deleteAppLimit(packageName: String) = dao.deleteAppLimit(packageName)
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

package com.example.blocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.RegainDatabase
import com.example.notifications.RegainNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Persistent Background Monitor Service that watches foreground app launches via UsageStatsManager
 * even when Regain Zen is minimized or closed, and triggers the centered WindowManager Floating
 * Pop-Up Card + Heads-Up Notification over blocked apps.
 */
class RegainBlockerMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var lastKnownForegroundPackage: String? = null
    private var lastTriggeredPackage: String? = null
    private var lastTriggeredTimeMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        startAsForegroundShield()
        startForegroundPackageMonitorLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForegroundShield()
        if (monitorJob?.isActive != true) {
            startForegroundPackageMonitorLoop()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startAsForegroundShield() {
        ensureMonitorChannelCreated(this)
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            5001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_BACKGROUND_SHIELD)
            .setSmallIcon(R.drawable.ic_rega_mascot)
            .setContentTitle("Regain Zen Background Shield Active")
            .setContentText("Monitoring blocked apps & Reels/Shorts to display mindful pop-ups")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID_SHIELD,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID_SHIELD, notification)
            }
        } catch (_: Exception) {
        }
    }

    private fun startForegroundPackageMonitorLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkCurrentForegroundAppAndIntercept()
                } catch (_: Exception) {
                }
                delay(1200L)
            }
        }
    }

    private fun checkCurrentForegroundAppAndIntercept() {
        if (!RegainPermissionsManager.hasUsageStatsPermission(applicationContext)) {
            return
        }

        val currentPkg = detectForegroundPackage(applicationContext) ?: return
        lastKnownForegroundPackage = currentPkg

        // Ignore our own app, system UI, settings, or home launchers
        if (currentPkg == packageName ||
            currentPkg.startsWith("com.android.systemui") ||
            currentPkg.startsWith("com.android.settings") ||
            currentPkg.contains("launcher") ||
            currentPkg.contains("permissioncontroller")
        ) {
            return
        }

        if (RegainOverlayWindowManager.isPackageInGraceWindow(currentPkg)) {
            return
        }

        val now = System.currentTimeMillis()
        if (currentPkg == lastTriggeredPackage &&
            (RegainOverlayWindowManager.isOverlayCurrentlyShowing() || (now - lastTriggeredTimeMs) < 6_000L)
        ) {
            return
        }

        val db = RegainDatabase.getDatabase(applicationContext)
        val matchingAppRule = db.getBlockingRuleForPackageSync(currentPkg)
        val matchingShield = db.getShortFormShieldForPackageSync(currentPkg)

        if (matchingAppRule != null) {
            lastTriggeredPackage = currentPkg
            lastTriggeredTimeMs = now
            db.incrementBlockCountSync(currentPkg)

            val subtitle = if (matchingAppRule.isBlockedCompletely) {
                "Blocked by Regain Zen App Blocker"
            } else {
                "Daily Limit Reached (${matchingAppRule.usedTodayMinutes}m / ${matchingAppRule.dailyLimitMinutes}m)"
            }

            RegainNotificationHelper.sendHeadsUpInterceptNotification(
                context = applicationContext,
                targetName = matchingAppRule.appName,
                reasonSubtitle = subtitle
            )

            val shownOverlay = RegainOverlayWindowManager.showFloatingBlockerPopUp(
                context = applicationContext,
                packageName = currentPkg,
                targetName = matchingAppRule.appName,
                reasonSubtitle = subtitle,
                usedMinutes = matchingAppRule.usedTodayMinutes,
                limitMinutes = matchingAppRule.dailyLimitMinutes,
                isStrictLocked = matchingAppRule.isStrictLocked,
                keepDmsAvailable = false
            )

            if (!shownOverlay) {
                launchFallbackSanctuaryActivity(matchingAppRule.appName, subtitle)
            }
        } else if (matchingShield != null && !matchingShield.allowDirectMessages) {
            lastTriggeredPackage = currentPkg
            lastTriggeredTimeMs = now
            db.incrementShortFormInterceptSync(matchingShield.id)

            val subtitle = "${matchingShield.platformName} (${matchingShield.feedName}) Full Shield Active"
            RegainNotificationHelper.sendHeadsUpInterceptNotification(
                context = applicationContext,
                targetName = matchingShield.platformName,
                reasonSubtitle = subtitle
            )

            val shownOverlay = RegainOverlayWindowManager.showFloatingBlockerPopUp(
                context = applicationContext,
                packageName = currentPkg,
                targetName = "${matchingShield.platformName} · ${matchingShield.feedName}",
                reasonSubtitle = subtitle,
                usedMinutes = matchingShield.usedMinutesToday,
                limitMinutes = matchingShield.dailyCapMinutes.coerceAtLeast(15),
                isStrictLocked = false,
                keepDmsAvailable = false
            )

            if (!shownOverlay) {
                launchFallbackSanctuaryActivity(matchingShield.platformName, subtitle)
            }
        }
    }

    private fun detectForegroundPackage(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 25_000L

        var latestPackage: String? = null
        var latestEventTime = 0L

        try {
            val usageEvents = usm.queryEvents(beginTime, endTime)
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    if (event.timeStamp >= latestEventTime) {
                        latestEventTime = event.timeStamp
                        latestPackage = event.packageName
                    }
                }
            }
        } catch (_: Exception) {
        }

        if (!latestPackage.isNullOrBlank()) {
            return latestPackage
        }

        // Fallback to recent UsageStats if no transition event occurred in the last 25 seconds
        return try {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, endTime - 60_000L, endTime)
            stats?.maxByOrNull { it.lastTimeUsed }
                ?.takeIf { (endTime - it.lastTimeUsed) < 30_000L }
                ?.packageName ?: lastKnownForegroundPackage
        } catch (_: Exception) {
            lastKnownForegroundPackage
        }
    }

    private fun launchFallbackSanctuaryActivity(targetName: String, subtitle: String) {
        try {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(RegainNotificationHelper.EXTRA_INTERCEPT_TARGET, targetName)
                putExtra(RegainNotificationHelper.EXTRA_INTERCEPT_SUBTITLE, subtitle)
            }
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    companion object {
        const val CHANNEL_BACKGROUND_SHIELD = "regain_background_shield_v11"
        const val NOTIFICATION_ID_SHIELD = 1105
        const val ACTION_STOP_SERVICE = "com.example.blocker.STOP_MONITOR_SERVICE"

        fun ensureServiceRunning(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, RegainBlockerMonitorService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            } catch (_: Exception) {
            }
        }

        private fun ensureMonitorChannelCreated(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_BACKGROUND_SHIELD,
                    "Background App Blocker Monitor",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps Regain Zen's app blocker and floating pop-up shield active in the background"
                }
                manager?.createNotificationChannel(channel)
            }
        }
    }
}

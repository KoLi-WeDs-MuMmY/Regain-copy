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
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
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
 * and AccessibilityService even when Regain Zen is minimized or closed, and immediately triggers
 * the centered WindowManager Floating Pop-Up Card + Heads-Up Notification over blocked apps.
 *
 * Includes PARTIAL_WAKE_LOCK + silent AudioTrack heartbeat so OEM battery savers (Xiaomi HyperOS/MIUI,
 * ColorOS/Realme, Vivo, OneUI) never freeze the blocker while YouTube or Instagram is open.
 */
class RegainBlockerMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var monitorJob: Job? = null
    private var silentAudioJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var silentAudioTrack: AudioTrack? = null

    @Volatile
    private var lastKnownForegroundPackage: String? = null

    @Volatile
    private var lastTriggeredPackage: String? = null

    @Volatile
    private var lastTriggeredTimeMs: Long = 0L

    private var lastUsageSyncTimeMs: Long = 0L

    private val mainTickerRunnable = object : Runnable {
        override fun run() {
            serviceScope.launch {
                try {
                    checkCurrentForegroundAppAndIntercept()
                } catch (_: Exception) {
                }
            }
            mainHandler.postDelayed(this, 450L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        acquireBackgroundWakeLock()
        startAsForegroundShield()
        startSilentAudioKeepAlive()
        startForegroundPackageMonitorLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        acquireBackgroundWakeLock()
        startAsForegroundShield()
        if (silentAudioJob?.isActive != true) {
            startSilentAudioKeepAlive()
        }
        if (monitorJob?.isActive != true) {
            startForegroundPackageMonitorLoop()
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Re-ensure service continues running even if user swipes away recent tasks
        ensureServiceRunning(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mainHandler.removeCallbacks(mainTickerRunnable)
        monitorJob?.cancel()
        silentAudioJob?.cancel()
        stopSilentAudioTrack()
        releaseBackgroundWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun acquireBackgroundWakeLock() {
        try {
            if (wakeLock?.isHeld == true) return
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RegainZen::BackgroundBlockerWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseBackgroundWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    /**
     * Plays a zero-amplitude (100% silent) low-sample-rate audio stream in the background.
     * This prevents aggressive OEM cgroup freezers from suspending our background blocker loop
     * when the user leaves Regain Zen and opens YouTube, Instagram, or TikTok.
     */
    private fun startSilentAudioKeepAlive() {
        silentAudioJob?.cancel()
        silentAudioJob = serviceScope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 8000
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(1600)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.setVolume(0f)
                track.play()
                silentAudioTrack = track

                val silentBuffer = ShortArray(800) // 100ms of silence
                while (isActive) {
                    track.write(silentBuffer, 0, silentBuffer.size)
                    delay(250L)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun stopSilentAudioTrack() {
        try {
            silentAudioTrack?.stop()
        } catch (_: Exception) {
        }
        try {
            silentAudioTrack?.release()
        } catch (_: Exception) {
        }
        silentAudioTrack = null
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
            .setContentText("Actively guarding against blocked apps, Reels & Shorts in real time")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID_SHIELD,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID_SHIELD, notification)
            }
        } catch (_: Exception) {
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
    }

    private fun startForegroundPackageMonitorLoop() {
        mainHandler.removeCallbacks(mainTickerRunnable)
        mainHandler.post(mainTickerRunnable)

        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkCurrentForegroundAppAndIntercept()
                } catch (_: Exception) {
                }
                delay(400L)
            }
        }
    }

    private fun checkCurrentForegroundAppAndIntercept() {
        // Never fire a delayed background block if the user is already inside Regain Zen!
        if (MainActivity.isAppInForeground) {
            lastTriggeredPackage = null
            return
        }

        val now = System.currentTimeMillis()
        val db = RegainDatabase.getDatabase(applicationContext)

        // Periodically sync UsageStats in the background so daily time caps trigger while using blocked apps
        if (now - lastUsageSyncTimeMs > 5_000L && RegainPermissionsManager.hasUsageStatsPermission(applicationContext)) {
            lastUsageSyncTimeMs = now
            db.syncUsageStatsFromServiceSync(applicationContext)
        }

        val currentPkg = detectForegroundPackage(applicationContext) ?: return
        lastKnownForegroundPackage = currentPkg

        // Ignore our own app, system UI, keyboards, settings, or home launchers
        if (currentPkg == packageName ||
            currentPkg.startsWith("com.android.systemui") ||
            currentPkg.startsWith("com.android.settings") ||
            currentPkg.contains("inputmethod") ||
            currentPkg.contains("keyboard") ||
            currentPkg.contains("launcher") ||
            currentPkg.contains("permissioncontroller")
        ) {
            if (!RegainOverlayWindowManager.isOverlayCurrentlyShowing()) {
                lastTriggeredPackage = null
            }
            return
        }

        if (RegainOverlayWindowManager.isPackageInGraceWindow(currentPkg)) {
            return
        }

        if (currentPkg == lastTriggeredPackage &&
            (RegainOverlayWindowManager.isOverlayCurrentlyShowing() || (now - lastTriggeredTimeMs) < 2_500L)
        ) {
            return
        }

        val matchingAppRule = db.getBlockingRuleForPackageSync(currentPkg)
        val matchingShield = db.getShortFormShieldForPackageSync(currentPkg)

        if (matchingAppRule != null) {
            lastTriggeredPackage = currentPkg
            lastTriggeredTimeMs = now
            db.incrementBlockCountSync(currentPkg)

            val subtitle = when {
                matchingAppRule.isBlockedCompletely && matchingAppRule.isStrictLocked ->
                    "Strict Mode + App Blocker Active — Blocked Immediately"
                matchingAppRule.isBlockedCompletely ->
                    "Blocked by Regain Zen App Blocker"
                matchingAppRule.isStrictLocked ->
                    "Strict Mode Active — Mindful pause required"
                else ->
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
                keepDmsAvailable = false,
                onOverlayFailedFallback = {
                    RegainAccessibilityBlockerService.performHomeActionIfAvailable()
                    launchFallbackSanctuaryActivity(matchingAppRule.appName, subtitle)
                }
            )

            if (!shownOverlay) {
                RegainAccessibilityBlockerService.performHomeActionIfAvailable()
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
                keepDmsAvailable = false,
                onOverlayFailedFallback = {
                    RegainAccessibilityBlockerService.performHomeActionIfAvailable()
                    launchFallbackSanctuaryActivity(matchingShield.platformName, subtitle)
                }
            )

            if (!shownOverlay) {
                RegainAccessibilityBlockerService.performHomeActionIfAvailable()
                launchFallbackSanctuaryActivity(matchingShield.platformName, subtitle)
            }
        }
    }

    private fun detectForegroundPackage(context: Context): String? {
        val now = System.currentTimeMillis()

        // 1. Check live AccessibilityService event first (0ms latency when Accessibility is enabled)
        val a11yPkg = RegainAccessibilityBlockerService.latestForegroundPackage
        val a11yTime = RegainAccessibilityBlockerService.latestForegroundTimestamp
        if (!a11yPkg.isNullOrBlank() && (now - a11yTime) < 15_000L) {
            return a11yPkg
        }

        // 2. Query UsageStatsManager UsageEvents over a 15-minute window
        if (!RegainPermissionsManager.hasUsageStatsPermission(context)) {
            return null
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val beginTime = now - 900_000L

        var currentForegroundPkg: String? = null
        var currentForegroundClass: String? = null
        var latestEventTime = 0L

        try {
            val usageEvents = usm.queryEvents(beginTime, now)
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (pkg.startsWith("com.android.systemui") ||
                    pkg.contains("inputmethod") ||
                    pkg.contains("keyboard")
                ) {
                    continue
                }
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        if (event.timeStamp >= latestEventTime) {
                            latestEventTime = event.timeStamp
                            currentForegroundPkg = pkg
                            currentForegroundClass = event.className
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.MOVE_TO_BACKGROUND,
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        // CRITICAL: Only clear if the EXACT SAME Activity class paused/stopped!
                        // Otherwise SplashActivity stopping 500ms after HomeActivity resumed
                        // inside YouTube/Instagram would falsely clear currentForegroundPkg.
                        if (pkg == currentForegroundPkg &&
                            event.className == currentForegroundClass &&
                            event.timeStamp > latestEventTime
                        ) {
                            latestEventTime = event.timeStamp
                            currentForegroundPkg = null
                            currentForegroundClass = null
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        if (!currentForegroundPkg.isNullOrBlank()) {
            return currentForegroundPkg
        }

        return lastKnownForegroundPackage?.takeIf { it != packageName }
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
        const val CHANNEL_BACKGROUND_SHIELD = "regain_background_shield_v12"
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
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Keeps Regain Zen's app blocker and floating pop-up shield active in the background"
                    setSound(null, null)
                    enableVibration(false)
                }
                manager?.createNotificationChannel(channel)
            }
        }
    }
}

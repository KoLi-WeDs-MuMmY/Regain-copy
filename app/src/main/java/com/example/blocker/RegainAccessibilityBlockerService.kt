package com.example.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.MainActivity
import com.example.data.RegainDatabase
import com.example.notifications.RegainNotificationHelper

/**
 * Real Android AccessibilityService that monitors foreground app switches and short-form video feeds
 * with 0ms latency. When the user opens a blocked app (e.g., YouTube in Strict Mode) or navigates into
 * Reels/Shorts, it immediately displays Regain Zen's centered WindowManager Floating Pop-Up Card Window,
 * vibrates, and fires a heads-up notification.
 */
class RegainAccessibilityBlockerService : AccessibilityService() {

    private var lastInterceptTimestamp: Long = 0L
    private var lastInterceptedPackage: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        RegainBlockerMonitorService.ensureServiceRunning(applicationContext)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) {
            instance = null
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()
            ?: try { rootInActiveWindow?.packageName?.toString() } catch (_: Exception) { null }
            ?: return

        if (pkg == packageName) {
            latestForegroundPackage = pkg
            latestForegroundTimestamp = System.currentTimeMillis()
            return
        }

        // Ignore system UI, keyboards, permission controllers, or home launchers
        if (pkg.startsWith("com.android.systemui") ||
            pkg.startsWith("com.android.settings") ||
            pkg.contains("inputmethod") ||
            pkg.contains("keyboard") ||
            pkg.contains("launcher") ||
            pkg.contains("permissioncontroller")
        ) {
            return
        }

        val now = System.currentTimeMillis()
        latestForegroundPackage = pkg
        latestForegroundTimestamp = now

        if (MainActivity.isAppInForeground) {
            return
        }

        if (RegainOverlayWindowManager.isPackageInGraceWindow(pkg)) {
            return
        }

        if (pkg == lastInterceptedPackage &&
            (RegainOverlayWindowManager.isOverlayCurrentlyShowing() || (now - lastInterceptTimestamp) < 3_500L)
        ) {
            return
        }

        val db = RegainDatabase.getDatabase(applicationContext)
        val matchingRule = db.getBlockingRuleForPackageSync(pkg)
        if (matchingRule != null) {
            lastInterceptedPackage = pkg
            lastInterceptTimestamp = now
            db.incrementBlockCountSync(pkg)

            val subtitle = when {
                matchingRule.isBlockedCompletely && matchingRule.isStrictLocked ->
                    "Strict Mode + App Blocker Active — Blocked Immediately"
                matchingRule.isBlockedCompletely ->
                    "App Blocker Active — Chosen to block completely"
                matchingRule.isStrictLocked ->
                    "Strict Mode Active — Mindful pause required"
                else ->
                    "Daily Limit Reached (${matchingRule.usedTodayMinutes}m / ${matchingRule.dailyLimitMinutes}m)"
            }

            RegainNotificationHelper.sendHeadsUpInterceptNotification(
                context = applicationContext,
                targetName = matchingRule.appName,
                reasonSubtitle = subtitle
            )

            val shownOverlay = RegainOverlayWindowManager.showFloatingBlockerPopUp(
                context = applicationContext,
                packageName = pkg,
                targetName = matchingRule.appName,
                reasonSubtitle = subtitle,
                usedMinutes = matchingRule.usedTodayMinutes,
                limitMinutes = matchingRule.dailyLimitMinutes,
                isStrictLocked = matchingRule.isStrictLocked,
                keepDmsAvailable = false,
                onOverlayFailedFallback = {
                    try {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    } catch (_: Exception) {
                    }
                    launchFallbackSanctuaryActivity(matchingRule.appName, subtitle)
                }
            )

            if (!shownOverlay) {
                try {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                } catch (_: Exception) {
                }
                launchFallbackSanctuaryActivity(matchingRule.appName, subtitle)
            }
            return
        }

        // Check Targeted Short-Form Shield (Reels, Shorts, TikTok, Spotlight)
        val shield = db.getShortFormShieldForPackageSync(pkg)
        if (shield != null) {
            val shouldBlockFeed = !shield.allowDirectMessages || isShortFormFeedVisibleInWindow(pkg)
            if (shouldBlockFeed) {
                lastInterceptedPackage = pkg
                lastInterceptTimestamp = now
                db.incrementShortFormInterceptSync(shield.id)

                val subtitle = "${shield.platformName} (${shield.feedName}) Shield Active"
                RegainNotificationHelper.sendHeadsUpInterceptNotification(
                    context = applicationContext,
                    targetName = "${shield.platformName} · ${shield.feedName}",
                    reasonSubtitle = subtitle
                )

                val shownOverlay = RegainOverlayWindowManager.showFloatingBlockerPopUp(
                    context = applicationContext,
                    packageName = pkg,
                    targetName = "${shield.platformName} · ${shield.feedName}",
                    reasonSubtitle = subtitle,
                    usedMinutes = shield.usedMinutesToday,
                    limitMinutes = shield.dailyCapMinutes.coerceAtLeast(15),
                    isStrictLocked = false,
                    keepDmsAvailable = shield.allowDirectMessages,
                    onOverlayFailedFallback = {
                        try {
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        } catch (_: Exception) {
                        }
                        launchFallbackSanctuaryActivity("${shield.platformName} (${shield.feedName})", subtitle)
                    }
                )

                if (!shownOverlay) {
                    try {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    } catch (_: Exception) {
                    }
                    launchFallbackSanctuaryActivity("${shield.platformName} (${shield.feedName})", subtitle)
                }
            }
        }
    }

    private fun isShortFormFeedVisibleInWindow(pkg: String): Boolean {
        if (pkg == "com.zhiliaoapp.musically" || pkg == "com.ss.android.ugc.trill") {
            return true
        }
        val root: AccessibilityNodeInfo = try { rootInActiveWindow } catch (_: Exception) { null } ?: return false
        return try {
            val reelsNodes = root.findAccessibilityNodeInfosByText("Reels")
            val shortsNodes = root.findAccessibilityNodeInfosByText("Shorts")
            val spotlightNodes = root.findAccessibilityNodeInfosByText("Spotlight")
            (reelsNodes != null && reelsNodes.any { it.isSelected || it.isVisibleToUser }) ||
                (shortsNodes != null && shortsNodes.any { it.isSelected }) ||
                (spotlightNodes != null && spotlightNodes.any { it.isSelected })
        } catch (_: Exception) {
            false
        }
    }

    private fun launchFallbackSanctuaryActivity(targetName: String, subtitle: String) {
        try {
            val interceptIntent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(RegainNotificationHelper.EXTRA_INTERCEPT_TARGET, targetName)
                putExtra(RegainNotificationHelper.EXTRA_INTERCEPT_SUBTITLE, subtitle)
            }
            startActivity(interceptIntent)
        } catch (_: Exception) {
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    companion object {
        @Volatile
        private var instance: RegainAccessibilityBlockerService? = null

        @Volatile
        var latestForegroundPackage: String? = null

        @Volatile
        var latestForegroundTimestamp: Long = 0L

        fun isConnectedInstanceAlive(): Boolean = instance != null

        fun performHomeActionIfAvailable() {
            try {
                instance?.performGlobalAction(GLOBAL_ACTION_HOME)
            } catch (_: Exception) {
            }
        }
    }
}

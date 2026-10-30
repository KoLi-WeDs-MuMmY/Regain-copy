package com.example.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.MainActivity
import com.example.data.RegainDatabase
import com.example.notifications.RegainNotificationHelper

/**
 * Real Android AccessibilityService that monitors foreground app switches and short-form video feeds.
 * When the user opens a blocked app or navigates into Reels/Shorts, it displays Regain Zen's
 * centered WindowManager Floating Pop-Up Card Window and fires a heads-up notification.
 */
class RegainAccessibilityBlockerService : AccessibilityService() {

    private var lastInterceptTimestamp: Long = 0L
    private var lastInterceptedPackage: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        RegainBlockerMonitorService.ensureServiceRunning(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        // Ignore our own package and system UI/settings/launchers
        if (pkg == packageName ||
            pkg.startsWith("com.android.systemui") ||
            pkg.startsWith("com.android.settings") ||
            pkg.contains("launcher") ||
            pkg.contains("permissioncontroller")
        ) {
            return
        }

        if (RegainOverlayWindowManager.isPackageInGraceWindow(pkg)) {
            return
        }

        val now = System.currentTimeMillis()
        if (pkg == lastInterceptedPackage &&
            (RegainOverlayWindowManager.isOverlayCurrentlyShowing() || (now - lastInterceptTimestamp) < 6_000L)
        ) {
            return
        }

        val db = RegainDatabase.getDatabase(applicationContext)
        val matchingRule = db.getBlockingRuleForPackageSync(pkg)
        if (matchingRule != null) {
            lastInterceptedPackage = pkg
            lastInterceptTimestamp = now
            db.incrementBlockCountSync(pkg)

            val subtitle = if (matchingRule.isBlockedCompletely) {
                "App Blocker Active — Chosen to block completely"
            } else {
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
                keepDmsAvailable = false
            )

            if (!shownOverlay) {
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
                    keepDmsAvailable = shield.allowDirectMessages
                )

                if (!shownOverlay) {
                    launchFallbackSanctuaryActivity("${shield.platformName} (${shield.feedName})", subtitle)
                }
            }
        }
    }

    private fun isShortFormFeedVisibleInWindow(pkg: String): Boolean {
        if (pkg == "com.zhiliaoapp.musically" || pkg == "com.ss.android.ugc.trill") {
            return true
        }
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return false
        return try {
            val reelsNodes = root.findAccessibilityNodeInfosByText("Reels")
            val shortsNodes = root.findAccessibilityNodeInfosByText("Shorts")
            val spotlightNodes = root.findAccessibilityNodeInfosByText("Spotlight")
            (reelsNodes != null && reelsNodes.any { it.isSelected }) ||
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
}

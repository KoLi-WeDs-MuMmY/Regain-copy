package com.example.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity
import com.example.data.RegainDatabase
import com.example.notifications.RegainNotificationHelper

/**
 * Real Android AccessibilityService that monitors foreground app switches and short-form video feeds.
 * When the user opens an app they chose to block in Regain Zen (or exceeds its daily limit),
 * it fires a heads-up pop-up notification and launches Regain Zen's animated Mindful Breathwork
 * Pop-Up Overlay to pull the user out of the distracting app or Shorts/Reels feed.
 */
class RegainAccessibilityBlockerService : AccessibilityService() {

    private var lastInterceptTimestamp: Long = 0L
    private var lastInterceptedPackage: String = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        // Ignore our own package and system UI/settings
        if (pkg == packageName ||
            pkg.startsWith("com.android.systemui") ||
            pkg.startsWith("com.android.settings") ||
            pkg.contains("launcher")
        ) {
            return
        }

        val now = System.currentTimeMillis()
        if (pkg == lastInterceptedPackage && (now - lastInterceptTimestamp) < 8_000L) {
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

            // 1. Show high-priority heads-up pop-up notification
            RegainNotificationHelper.sendHeadsUpInterceptNotification(
                context = applicationContext,
                targetName = matchingRule.appName,
                reasonSubtitle = subtitle
            )

            // 2. Pull user out of the blocked app and display the animated Mindful Intercept Pop-Up
            val interceptIntent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(RegainNotificationHelper.EXTRA_INTERCEPT_TARGET, matchingRule.appName)
                putExtra(RegainNotificationHelper.EXTRA_INTERCEPT_SUBTITLE, subtitle)
            }
            startActivity(interceptIntent)
        }
    }

    override fun onInterrupt() {
        // No-op
    }
}

package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object RegainNotificationHelper {
    const val CHANNEL_POPUP_ALERTS = "regain_popup_alerts_v11"
    const val CHANNEL_FOCUS_TIMER = "regain_focus_timer_v11"

    const val EXTRA_INTERCEPT_TARGET = "extra_intercept_target"
    const val EXTRA_INTERCEPT_SUBTITLE = "extra_intercept_subtitle"

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val popupChannel = NotificationChannel(
                CHANNEL_POPUP_ALERTS,
                "App Blocker & Mindful Pop-Up Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up pop-up notifications when blocked apps, Reels, or Shorts are intercepted"
                enableVibration(true)
            }

            val timerChannel = NotificationChannel(
                CHANNEL_FOCUS_TIMER,
                "Study Timer & Exact Session Summaries",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pop-up notifications for study timer progress and exact recorded study duration"
                enableVibration(true)
            }

            manager.createNotificationChannel(popupChannel)
            manager.createNotificationChannel(timerChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendHeadsUpInterceptNotification(
        context: Context,
        targetName: String,
        reasonSubtitle: String
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_INTERCEPT_TARGET, targetName)
            putExtra(EXTRA_INTERCEPT_SUBTITLE, reasonSubtitle)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            targetName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_POPUP_ALERTS)
            .setSmallIcon(R.drawable.ic_rega_mascot)
            .setContentTitle("Mindful Shield: $targetName Blocked")
            .setContentText("$reasonSubtitle — Take a 4-4-4 breath and return to focus.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$reasonSubtitle. Rega stepped in to protect your focus. Tap to open the mindful breathing screen."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2001 + (targetName.hashCode() and 0xFF), notification)
        } catch (_: SecurityException) {
        }
    }

    fun sendStudySessionCompletedNotification(
        context: Context,
        tag: String,
        exactDurationFormatted: String,
        coinsEarned: Int
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS_TIMER)
            .setSmallIcon(R.drawable.ic_rega_mascot)
            .setContentTitle("Study Session Recorded: $exactDurationFormatted")
            .setContentText("Exact study time ($exactDurationFormatted of $tag) saved! Earned +$coinsEarned Leaf Coins.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(3001, notification)
        } catch (_: SecurityException) {
        }
    }

    fun sendAppBlockerUpdatedNotification(
        context: Context,
        appName: String,
        isBlocked: Boolean
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val statusTitle = if (isBlocked) {
            "App Blocker Active: $appName"
        } else {
            "App Blocker Relaxed: $appName"
        }
        val statusBody = if (isBlocked) {
            "$appName is now actively blocked. Opening it will trigger Regain Zen's animated pop-up shield."
        } else {
            "$appName has been removed from immediate block mode."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_POPUP_ALERTS)
            .setSmallIcon(R.drawable.ic_rega_mascot)
            .setContentTitle(statusTitle)
            .setContentText(statusBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(4001, notification)
        } catch (_: SecurityException) {
        }
    }

    fun sendPomodoroBreakNotification(
        context: Context,
        title: String,
        body: String
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3005,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS_TIMER)
            .setSmallIcon(R.drawable.ic_rega_mascot)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(3005, notification)
        } catch (_: SecurityException) {
        }
    }
}

package com.example.blocker

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.notifications.RegainNotificationHelper

data class BlockerPermissionStatus(
    val hasNotificationsPermission: Boolean = false,
    val hasOverlayPopUpPermission: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val hasAccessibilityBlockerEnabled: Boolean = false
) {
    val grantedCount: Int
        get() = listOf(
            hasNotificationsPermission,
            hasOverlayPopUpPermission,
            hasUsageStatsPermission,
            hasAccessibilityBlockerEnabled
        ).count { it }

    val allGranted: Boolean
        get() = grantedCount == 4
}

object RegainPermissionsManager {

    fun checkAllPermissions(context: Context): BlockerPermissionStatus {
        return BlockerPermissionStatus(
            hasNotificationsPermission = RegainNotificationHelper.hasNotificationPermission(context),
            hasOverlayPopUpPermission = hasOverlayPermission(context),
            hasUsageStatsPermission = hasUsageStatsPermission(context),
            hasAccessibilityBlockerEnabled = isAccessibilityBlockerEnabled(context)
        )
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun isAccessibilityBlockerEnabled(context: Context): Boolean {
        return try {
            val expectedComponent = ComponentName(context, RegainAccessibilityBlockerService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.split(':').any {
                ComponentName.unflattenFromString(it)?.equals(expectedComponent) == true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openOverlayPermissionSettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppDetailsSettings(context)
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppDetailsSettings(context)
        }
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppDetailsSettings(context)
        }
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppDetailsSettings(context)
        }
    }

    private fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

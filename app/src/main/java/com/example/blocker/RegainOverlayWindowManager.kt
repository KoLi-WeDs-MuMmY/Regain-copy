package com.example.blocker

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.RegainDatabase
import com.example.ui.components.BlockerInterceptCardSlot
import com.example.ui.components.BlockerOverlayContainer
import com.example.ui.components.LocalBlockerOverlayDismissTrigger
import com.example.ui.components.routeToAndroidHomeScreen
import com.example.ui.theme.RegainZenTheme
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the Android system WindowManager (`TYPE_APPLICATION_OVERLAY`) intercept window
 * centered over blocked apps, Reels, and Shorts — even when Regain Zen is in the background.
 *
 * Version 1.2 Architecture:
 * - Hosts the decoupled Jetpack Compose [BlockerOverlayContainer] and modular [BlockerInterceptCardSlot]
 *   directly inside the overlay window via [ComposeView] + [OverlayComposeLifecycleOwner].
 * - Never animates WindowManager layout parameters via `updateViewLayout`.
 * - Waits for [BlockerOverlayContainer]'s 200ms exit transition (`fadeOut + slideOutVertically`)
 *   to complete before removing the view and routing to `Intent.CATEGORY_HOME`.
 */
object RegainOverlayWindowManager {

    const val EXTRA_RETURNED_FROM_OVERLAY = "extra_returned_from_overlay_target"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val graceExpiryMap = ConcurrentHashMap<String, Long>()

    @Volatile
    private var activeOverlayView: View? = null

    @Volatile
    private var activeLifecycleOwner: OverlayComposeLifecycleOwner? = null

    @Volatile
    private var activeTargetPackage: String? = null

    fun isPackageInGraceWindow(packageName: String): Boolean {
        val expiry = graceExpiryMap[packageName] ?: return false
        val now = System.currentTimeMillis()
        if (now < expiry) {
            return true
        }
        graceExpiryMap.remove(packageName)
        return false
    }

    fun getRemainingGraceSeconds(packageName: String): Int {
        val expiry = graceExpiryMap[packageName] ?: return 0
        val remainingMs = expiry - System.currentTimeMillis()
        if (remainingMs <= 0L) {
            graceExpiryMap.remove(packageName)
            return 0
        }
        return (remainingMs / 1000L).toInt()
    }

    fun clearGraceWindow(packageName: String) {
        if (packageName.isNotBlank()) {
            graceExpiryMap.remove(packageName)
        }
    }

    fun clearAllGraceWindows() {
        graceExpiryMap.clear()
    }

    /**
     * Grants a break window for the specified [minutes] (e.g., 5, 10, 15, or 30 minutes).
     */
    fun grantBreakMinutes(packageName: String, minutes: Int) {
        if (packageName.isNotBlank() && minutes > 0) {
            graceExpiryMap[packageName] = System.currentTimeMillis() + minutes * 60_000L
        }
    }

    fun grantFiveMinuteGrace(packageName: String) {
        grantBreakMinutes(packageName, 5)
    }

    private fun grantBriefReturnCooldown(packageName: String) {
        if (packageName.isNotBlank()) {
            graceExpiryMap[packageName] = System.currentTimeMillis() + 2_000L
        }
    }

    fun isOverlayCurrentlyShowing(): Boolean = activeOverlayView != null

    /**
     * Attaches the decoupled Compose [BlockerOverlayContainer] over the active screen via WindowManager.
     */
    fun showFloatingBlockerPopUp(
        context: Context,
        packageName: String,
        targetName: String,
        reasonSubtitle: String,
        usedMinutes: Int,
        limitMinutes: Int,
        isStrictLocked: Boolean,
        keepDmsAvailable: Boolean = false,
        onOverlayFailedFallback: (() -> Unit)? = null
    ): Boolean {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(appContext)) {
            return false
        }

        val showTask = Runnable {
            val attached = showComposeOverlayOnMainThread(
                appContext = appContext,
                packageName = packageName,
                targetName = targetName,
                reasonSubtitle = reasonSubtitle,
                usedMinutes = usedMinutes,
                limitMinutes = limitMinutes,
                isStrictLocked = isStrictLocked
            )
            if (!attached) {
                onOverlayFailedFallback?.invoke()
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            showTask.run()
        } else {
            mainHandler.postAtFrontOfQueue(showTask)
        }
        return true
    }

    fun dismissFloatingPopUp(context: Context) {
        val appContext = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            removeOverlayViewImmediate(appContext)
        } else {
            mainHandler.post {
                removeOverlayViewImmediate(appContext)
            }
        }
    }

    private fun triggerMindfulVibrationWarning(context: Context) {
        try {
            val pattern = longArrayOf(0L, 90L, 70L, 140L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vm?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun showComposeOverlayOnMainThread(
        appContext: Context,
        packageName: String,
        targetName: String,
        reasonSubtitle: String,
        usedMinutes: Int,
        limitMinutes: Int,
        isStrictLocked: Boolean
    ): Boolean {
        if (activeOverlayView != null && activeTargetPackage == packageName) {
            return true
        }
        removeOverlayViewImmediate(appContext)

        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return false
        val db = RegainDatabase.getDatabase(appContext)

        val lifecycleOwner = OverlayComposeLifecycleOwner().apply {
            onCreate()
            onResume()
        }

        val rootContainer = FrameLayout(appContext).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }

        var selectedBreakMinutes = 0

        val composeView = ComposeView(appContext).apply {
            setContent {
                RegainZenTheme(darkTheme = true) {
                    BlockerOverlayContainer(
                        onDismissComplete = {
                            if (selectedBreakMinutes > 0) {
                                grantBreakMinutes(packageName, selectedBreakMinutes)
                                db.recordFiveMinGraceBreathSync()
                                removeOverlayViewImmediate(appContext)
                            } else {
                                grantBriefReturnCooldown(packageName)
                                db.recordMindfulInterceptVictorySync()
                                removeOverlayViewImmediate(appContext)
                                RegainAccessibilityBlockerService.performHomeActionIfAvailable()
                                routeToAndroidHomeScreen(appContext)
                            }
                        }
                    ) {
                        val triggerDismiss = LocalBlockerOverlayDismissTrigger.current

                        BlockerInterceptCardSlot(
                            targetName = targetName,
                            targetSubtitle = reasonSubtitle,
                            usedMinutes = usedMinutes,
                            limitMinutes = limitMinutes,
                            isStrictLocked = isStrictLocked,
                            onReturnToFocusClick = {
                                selectedBreakMinutes = 0
                                triggerDismiss()
                            },
                            onSelectBreakMinutes = { minutes ->
                                selectedBreakMinutes = minutes
                                triggerDismiss()
                            }
                        )
                    }
                }
            }
        }

        rootContainer.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Always include FLAG_NOT_FOCUSABLE so Android 12-15 never blocks or delays
        // the overlay window when launched from a background service over YouTube/Instagram.
        val primaryFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            primaryFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val addedSuccessfully = try {
            windowManager.addView(rootContainer, params)
            true
        } catch (_: Exception) {
            lifecycleOwner.onDestroy()
            false
        }

        if (!addedSuccessfully) {
            activeOverlayView = null
            activeLifecycleOwner = null
            activeTargetPackage = null
            return false
        }

        activeOverlayView = rootContainer
        activeLifecycleOwner = lifecycleOwner
        activeTargetPackage = packageName
        triggerMindfulVibrationWarning(appContext)
        return true
    }

    private fun removeOverlayViewImmediate(appContext: Context) {
        val owner = activeLifecycleOwner
        activeLifecycleOwner = null
        owner?.onDestroy()

        val view = activeOverlayView ?: return
        activeOverlayView = null
        activeTargetPackage = null
        try {
            val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            windowManager?.removeViewImmediate(view)
        } catch (_: Exception) {
        }
    }
}

/**
 * Self-contained LifecycleOwner, ViewModelStoreOwner, and SavedStateRegistryOwner enabling
 * Jetpack Compose ([ComposeView] and [BlockerOverlayContainer]) to run inside a system
 * `WindowManager` overlay without an Activity host.
 */
private class OverlayComposeLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        try {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        } catch (_: Exception) {
        }
    }
}

package com.example.blocker

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.data.RegainDatabase
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the real Android system WindowManager (TYPE_APPLICATION_OVERLAY) Floating Pop-Up Card
 * centered directly over blocked apps, Reels, and Shorts — even when Regain Zen is in the background.
 *
 * Uses FLAG_NOT_FOCUSABLE for both Normal and Strict Mode (with tap-to-confirm Strict Phrase chips)
 * so Android 12–15 WindowManager never suppresses or delays the floating pop-up when another app
 * (e.g., YouTube, Instagram, TikTok) is in the foreground.
 */
object RegainOverlayWindowManager {

    const val EXTRA_RETURNED_FROM_OVERLAY = "extra_returned_from_overlay_target"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val graceExpiryMap = ConcurrentHashMap<String, Long>()

    @Volatile
    private var activeOverlayView: View? = null

    @Volatile
    private var activeTargetPackage: String? = null

    private var countdownRunnable: Runnable? = null
    private var pulseAnimator: ValueAnimator? = null

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

    fun grantFiveMinuteGrace(packageName: String) {
        if (packageName.isNotBlank()) {
            graceExpiryMap[packageName] = System.currentTimeMillis() + 5 * 60_000L
        }
    }

    private fun grantBriefReturnCooldown(packageName: String) {
        if (packageName.isNotBlank()) {
            graceExpiryMap[packageName] = System.currentTimeMillis() + 2_000L
        }
    }

    fun isOverlayCurrentlyShowing(): Boolean = activeOverlayView != null

    /**
     * Displays the centered Floating Pop-Up Card Window over the current screen using WindowManager.
     * Returns true if overlay permission is granted and the window attachment was scheduled.
     * If WindowManager fails to attach the view on a restricted OEM ROM, [onOverlayFailedFallback] is invoked.
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
            val attached = showOnMainThread(
                appContext = appContext,
                packageName = packageName,
                targetName = targetName,
                reasonSubtitle = reasonSubtitle,
                usedMinutes = usedMinutes,
                limitMinutes = limitMinutes,
                isStrictLocked = isStrictLocked,
                keepDmsAvailable = keepDmsAvailable
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

    private fun showOnMainThread(
        appContext: Context,
        packageName: String,
        targetName: String,
        reasonSubtitle: String,
        usedMinutes: Int,
        limitMinutes: Int,
        isStrictLocked: Boolean,
        keepDmsAvailable: Boolean
    ): Boolean {
        if (activeOverlayView != null && activeTargetPackage == packageName) {
            return true
        }
        removeOverlayViewImmediate(appContext)

        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return false
        val themedContext = ContextThemeWrapper(appContext, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val db = RegainDatabase.getDatabase(appContext)
        val strictPassphrase = db.getStrictPassphraseSync()
        val targetWords = strictPassphrase
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("I", "choose", "mindful", "presence") }

        var remainingSeconds = 8
        var confirmedWordCount = 0

        val rootContainer = FrameLayout(themedContext).apply {
            setPadding(dp(themedContext, 18), dp(themedContext, 22), dp(themedContext, 18), dp(themedContext, 22))
        }

        val cardLayout = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(themedContext, 20), dp(themedContext, 18), dp(themedContext, 20), dp(themedContext, 18))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(themedContext, 28).toFloat()
                setColor(Color.parseColor("#FBF9F5")) // Warm Alabaster Cream
                setStroke(dp(themedContext, 2), Color.parseColor("#2E5A44")) // Deep Forest Sage border
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dp(themedContext, 16).toFloat()
            }
        }

        // 1. Top Badge Pill
        val badgeText = TextView(themedContext).apply {
            text = when {
                keepDmsAvailable -> "REGAIN ZEN · TARGETED FEED SHIELD (DMs OPEN)"
                isStrictLocked -> "REGAIN ZEN · STRICT MODE APP BLOCKER"
                else -> "REGAIN ZEN · FLOATING APP BLOCKER SHIELD"
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (isStrictLocked) Color.parseColor("#8E3B24") else Color.parseColor("#2E5A44"))
            setPadding(dp(themedContext, 12), dp(themedContext, 5), dp(themedContext, 12), dp(themedContext, 5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(themedContext, 50).toFloat()
                setColor(if (isStrictLocked) Color.parseColor("#F7DFD6") else Color.parseColor("#D8E8DC"))
            }
        }
        cardLayout.addView(
            badgeText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(themedContext, 8)
            }
        )

        // 2. Headline & Reason Subtitle
        val titleView = TextView(themedContext).apply {
            text = "Pause Before Opening $targetName"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setTextColor(Color.parseColor("#1C2520"))
            gravity = Gravity.CENTER
        }
        cardLayout.addView(
            titleView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(themedContext, 4)
            }
        )

        val subtitleView = TextView(themedContext).apply {
            text = reasonSubtitle
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(Color.parseColor("#54635A"))
            gravity = Gravity.CENTER
        }
        cardLayout.addView(
            subtitleView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(themedContext, 10)
            }
        )

        // 3. Animated 60fps Lotus Breathing Orb View
        val lotusOrbView = LotusBreathingOrbView(themedContext)
        cardLayout.addView(
            lotusOrbView,
            LinearLayout.LayoutParams(dp(themedContext, 146), dp(themedContext, 146)).apply {
                bottomMargin = dp(themedContext, 8)
            }
        )

        val breathInstructionView = TextView(themedContext).apply {
            text = "Inhale deeply through your nose (4s)..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.ITALIC)
            setTextColor(Color.parseColor("#2E5A44"))
            gravity = Gravity.CENTER
        }
        cardLayout.addView(
            breathInstructionView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(themedContext, 10)
            }
        )

        // 4. Stats Row Pill
        val statsPill = TextView(themedContext).apply {
            val modeText = if (isStrictLocked) "Strict Locked" else "Mindful Pause"
            text = "Used Today: ${usedMinutes}m   ·   Daily Cap: ${limitMinutes}m   ·   $modeText"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#35473E"))
            gravity = Gravity.CENTER
            setPadding(dp(themedContext, 12), dp(themedContext, 7), dp(themedContext, 12), dp(themedContext, 7))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(themedContext, 14).toFloat()
                setColor(Color.parseColor("#EFECE4"))
            }
        }
        cardLayout.addView(
            statsPill,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(themedContext, 10)
            }
        )

        // 5. Secondary Button reference (updated by countdown & strict phrase chips)
        val allowGraceButton = Button(themedContext).apply {
            isAllCaps = false
            text = "Breathe (8s) to Unlock 5m Grace"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(Color.parseColor("#54635A"))
            isEnabled = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(themedContext, 14).toFloat()
                setColor(Color.parseColor("#E6E2D8"))
                setStroke(dp(themedContext, 1), Color.parseColor("#B5C0B8"))
            }
        }

        fun updateGraceButtonState() {
            val phraseMatched = !isStrictLocked || confirmedWordCount >= targetWords.size
            val canUnlock = remainingSeconds <= 0 && phraseMatched
            allowGraceButton.isEnabled = canUnlock
            if (remainingSeconds > 0) {
                allowGraceButton.text = "Breathe (${remainingSeconds}s) for 5m Intentional Grace"
                allowGraceButton.setTextColor(Color.parseColor("#6E7B73"))
            } else if (!phraseMatched) {
                allowGraceButton.text = "Tap Strict Phrase Words Above (${confirmedWordCount}/${targetWords.size})"
                allowGraceButton.setTextColor(Color.parseColor("#C86D51"))
            } else {
                allowGraceButton.text = "Allow 5m Intentional Grace Window"
                allowGraceButton.setTextColor(Color.parseColor("#2E5A44"))
            }
        }

        // Strict Mode Tap-to-Confirm Passphrase Chips (Works over all apps without needing IME focus)
        if (isStrictLocked) {
            val strictPrompt = TextView(themedContext).apply {
                text = "Strict Mode: Tap each word in order to affirm \"$strictPassphrase\":"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setTextColor(Color.parseColor("#54635A"))
                gravity = Gravity.CENTER
            }
            cardLayout.addView(
                strictPrompt,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(themedContext, 6)
                }
            )

            val chipsRow = LinearLayout(themedContext).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            targetWords.forEachIndexed { index, word ->
                val chipBtn = TextView(themedContext).apply {
                    text = word
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.parseColor("#2E5A44"))
                    setPadding(dp(themedContext, 10), dp(themedContext, 6), dp(themedContext, 10), dp(themedContext, 6))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(themedContext, 50).toFloat()
                        setColor(Color.WHITE)
                        setStroke(dp(themedContext, 1), Color.parseColor("#7A9E7E"))
                    }
                    setOnClickListener {
                        if (confirmedWordCount == index) {
                            confirmedWordCount++
                            setTextColor(Color.WHITE)
                            background = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = dp(themedContext, 50).toFloat()
                                setColor(Color.parseColor("#2E5A44"))
                            }
                            updateGraceButtonState()
                        }
                    }
                }
                chipsRow.addView(
                    chipBtn,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = dp(themedContext, 3)
                        marginEnd = dp(themedContext, 3)
                    }
                )
            }

            cardLayout.addView(
                chipsRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(themedContext, 10)
                }
            )
        }

        // 6. Primary Button: Close Blocked App & Return inside Regain Zen Focus Sanctuary
        val returnSanctuaryButton = Button(themedContext).apply {
            isAllCaps = false
            text = "Close $targetName & Return to Sanctuary (+15 Coins)"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(themedContext, 14), dp(themedContext, 11), dp(themedContext, 14), dp(themedContext, 11))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(themedContext, 16).toFloat()
                setColor(Color.parseColor("#2E5A44")) // Deep Forest Sage
            }
            setOnClickListener {
                grantBriefReturnCooldown(packageName)
                db.recordMindfulInterceptVictorySync()
                removeOverlayViewImmediate(appContext)
                // Also ask AccessibilityService (if active) to exit the blocked app to Home first
                RegainAccessibilityBlockerService.performHomeActionIfAvailable()
                val sanctuaryIntent = Intent(appContext, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    putExtra(EXTRA_RETURNED_FROM_OVERLAY, targetName)
                }
                try {
                    appContext.startActivity(sanctuaryIntent)
                } catch (_: Exception) {
                }
            }
        }
        cardLayout.addView(
            returnSanctuaryButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(themedContext, 8)
            }
        )

        // 7. Secondary Button: Allow 5m Intentional Grace
        allowGraceButton.setOnClickListener {
            grantFiveMinuteGrace(packageName)
            db.recordFiveMinGraceBreathSync()
            removeOverlayViewImmediate(appContext)
        }
        cardLayout.addView(
            allowGraceButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        rootContainer.addView(
            cardLayout,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // CRITICAL: Always include FLAG_NOT_FOCUSABLE so Android 12-15 never blocks or delays
        // the floating overlay window when launched from a background service over YouTube/Instagram.
        val primaryFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            primaryFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.65f
        }

        val addedSuccessfully = try {
            windowManager.addView(rootContainer, params)
            true
        } catch (_: Exception) {
            // Fallback without FLAG_DIM_BEHIND for strict OEM WindowManagers
            try {
                val fallbackParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                rootContainer.setBackgroundColor(Color.parseColor("#99121A16"))
                windowManager.addView(rootContainer, fallbackParams)
                true
            } catch (_: Exception) {
                false
            }
        }

        if (!addedSuccessfully) {
            activeOverlayView = null
            activeTargetPackage = null
            return false
        }

        activeOverlayView = rootContainer
        activeTargetPackage = packageName
        triggerMindfulVibrationWarning(appContext)

        // Pop-in spring entrance animation
        cardLayout.scaleX = 0.84f
        cardLayout.scaleY = 0.84f
        cardLayout.alpha = 0f
        cardLayout.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(350L)
            .setInterpolator(OvershootInterpolator(1.12f))
            .start()

        // Start 60fps breathing lotus orb animation
        pulseAnimator = ValueAnimator.ofFloat(0.68f, 1.0f).apply {
            duration = 3200L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val scale = anim.animatedValue as Float
                lotusOrbView.updatePulseScale(scale)
            }
            start()
        }

        // Start 8-second mindful breathing countdown
        val ticker = object : Runnable {
            override fun run() {
                if (activeOverlayView == null) return
                lotusOrbView.updateCountdown(remainingSeconds)
                breathInstructionView.text = when {
                    remainingSeconds >= 6 -> "Inhale deeply through your nose (4s)..."
                    remainingSeconds >= 3 -> "Hold gently inside your chest (4s)..."
                    remainingSeconds > 0 -> "Exhale slowly & release impulse..."
                    else -> "Mindful pause complete. Choose intentionally."
                }
                updateGraceButtonState()
                if (remainingSeconds > 0) {
                    remainingSeconds--
                    mainHandler.postDelayed(this, 1000L)
                }
            }
        }
        countdownRunnable = ticker
        mainHandler.post(ticker)
        return true
    }

    private fun removeOverlayViewImmediate(appContext: Context) {
        countdownRunnable?.let { mainHandler.removeCallbacks(it) }
        countdownRunnable = null
        pulseAnimator?.cancel()
        pulseAnimator = null

        val view = activeOverlayView ?: return
        activeOverlayView = null
        activeTargetPackage = null
        try {
            val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            windowManager?.removeViewImmediate(view)
        } catch (_: Exception) {
        }
    }

    private fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}

/**
 * Custom Canvas view rendering the animated 4-4-4 Zen Lotus Breathing Orb inside the system overlay window.
 */
private class LotusBreathingOrbView(context: Context) : View(context) {

    private var pulseScale: Float = 0.75f
    private var countdownSeconds: Int = 8

    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val midRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#2E5A44")
        alpha = 110
    }
    private val innerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#7A9E7E")
        alpha = 55
    }
    private val coreRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#C86D51")
        alpha = 175
    }
    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C2520")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            22f,
            context.resources.displayMetrics
        )
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E5A44")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            11f,
            context.resources.displayMetrics
        )
    }

    fun updatePulseScale(scale: Float) {
        pulseScale = scale
        invalidate()
    }

    fun updateCountdown(seconds: Int) {
        countdownSeconds = seconds
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = (minOf(width, height) / 2f) * 0.94f
        val currentRadius = (maxRadius * pulseScale).coerceAtLeast(12f)

        outerGlowPaint.shader = RadialGradient(
            cx,
            cy,
            currentRadius,
            intArrayOf(
                Color.parseColor("#667A9E7E"),
                Color.parseColor("#222E5A44"),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, currentRadius, outerGlowPaint)
        canvas.drawCircle(cx, cy, currentRadius * 0.74f, midRingPaint)
        canvas.drawCircle(cx, cy, currentRadius * 0.56f, innerFillPaint)
        canvas.drawCircle(cx, cy, maxRadius * 0.38f, coreRingPaint)

        val mainLabel = if (countdownSeconds > 0) "${countdownSeconds}s" else "Ready"
        canvas.drawText(mainLabel, cx, cy + (centerTextPaint.textSize * 0.15f), centerTextPaint)
        canvas.drawText("4-4-4 BREATH", cx, cy + (centerTextPaint.textSize * 0.85f), subTextPaint)
    }
}

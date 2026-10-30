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
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.data.RegainDatabase
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the real Android system WindowManager (TYPE_APPLICATION_OVERLAY) Floating Pop-Up Card
 * centered directly over blocked apps, Reels, and Shorts — even when Regain Zen is in the background.
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

    fun grantFiveMinuteGrace(packageName: String) {
        if (packageName.isNotBlank()) {
            graceExpiryMap[packageName] = System.currentTimeMillis() + 5 * 60_000L
        }
    }

    fun isOverlayCurrentlyShowing(): Boolean = activeOverlayView != null

    /**
     * Displays the centered Floating Pop-Up Card Window over the current screen using WindowManager.
     * Returns true if the system overlay window was shown (or updated), false if overlay permission is missing.
     */
    fun showFloatingBlockerPopUp(
        context: Context,
        packageName: String,
        targetName: String,
        reasonSubtitle: String,
        usedMinutes: Int,
        limitMinutes: Int,
        isStrictLocked: Boolean,
        keepDmsAvailable: Boolean = false
    ): Boolean {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(appContext)) {
            return false
        }

        mainHandler.post {
            showOnMainThread(
                appContext = appContext,
                packageName = packageName,
                targetName = targetName,
                reasonSubtitle = reasonSubtitle,
                usedMinutes = usedMinutes,
                limitMinutes = limitMinutes,
                isStrictLocked = isStrictLocked,
                keepDmsAvailable = keepDmsAvailable
            )
        }
        return true
    }

    fun dismissFloatingPopUp(context: Context) {
        val appContext = context.applicationContext
        mainHandler.post {
            removeOverlayViewImmediate(appContext)
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
    ) {
        if (activeOverlayView != null && activeTargetPackage == packageName) {
            return
        }
        removeOverlayViewImmediate(appContext)

        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val db = RegainDatabase.getDatabase(appContext)
        val strictPassphrase = db.getStrictPassphraseSync()

        var remainingSeconds = 8
        var typedPassphrase = ""

        val rootContainer = FrameLayout(appContext).apply {
            setPadding(dp(appContext, 20), dp(appContext, 24), dp(appContext, 20), dp(appContext, 24))
        }

        val cardLayout = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(appContext, 22), dp(appContext, 20), dp(appContext, 22), dp(appContext, 20))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(appContext, 28).toFloat()
                setColor(Color.parseColor("#FBF9F5")) // Warm Alabaster Cream
                setStroke(dp(appContext, 2), Color.parseColor("#2E5A44")) // Deep Forest Sage border
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dp(appContext, 16).toFloat()
            }
        }

        // 1. Top Badge Pill
        val badgeText = TextView(appContext).apply {
            text = if (keepDmsAvailable) {
                "REGAIN ZEN · TARGETED FEED SHIELD (DMs OPEN)"
            } else {
                "REGAIN ZEN · FLOATING APP BLOCKER SHIELD"
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#2E5A44"))
            setPadding(dp(appContext, 12), dp(appContext, 5), dp(appContext, 12), dp(appContext, 5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(appContext, 50).toFloat()
                setColor(Color.parseColor("#D8E8DC"))
            }
        }
        cardLayout.addView(
            badgeText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(appContext, 10)
            }
        )

        // 2. Headline & Reason Subtitle
        val titleView = TextView(appContext).apply {
            text = "Pause Before Opening $targetName"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
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
                bottomMargin = dp(appContext, 4)
            }
        )

        val subtitleView = TextView(appContext).apply {
            text = reasonSubtitle
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#54635A"))
            gravity = Gravity.CENTER
        }
        cardLayout.addView(
            subtitleView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(appContext, 12)
            }
        )

        // 3. Animated 60fps Lotus Breathing Orb View
        val lotusOrbView = LotusBreathingOrbView(appContext)
        cardLayout.addView(
            lotusOrbView,
            LinearLayout.LayoutParams(dp(appContext, 156), dp(appContext, 156)).apply {
                bottomMargin = dp(appContext, 8)
            }
        )

        val breathInstructionView = TextView(appContext).apply {
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
                bottomMargin = dp(appContext, 12)
            }
        )

        // 4. Stats Row Pill
        val statsPill = TextView(appContext).apply {
            val modeText = if (isStrictLocked) "Strict Locked" else "Mindful Pause"
            text = "Used Today: ${usedMinutes}m   ·   Daily Cap: ${limitMinutes}m   ·   $modeText"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#35473E"))
            gravity = Gravity.CENTER
            setPadding(dp(appContext, 12), dp(appContext, 8), dp(appContext, 12), dp(appContext, 8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(appContext, 14).toFloat()
                setColor(Color.parseColor("#EFECE4"))
            }
        }
        cardLayout.addView(
            statsPill,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(appContext, 12)
            }
        )

        // 5. Secondary Button reference (updated by countdown & strict input)
        val allowGraceButton = Button(appContext).apply {
            isAllCaps = false
            text = "Breathe (8s) to Unlock 5m Grace"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#54635A"))
            isEnabled = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(appContext, 14).toFloat()
                setColor(Color.parseColor("#E6E2D8"))
                setStroke(dp(appContext, 1), Color.parseColor("#B5C0B8"))
            }
        }

        fun updateGraceButtonState() {
            val phraseMatched = !isStrictLocked ||
                typedPassphrase.trim().equals(strictPassphrase.trim(), ignoreCase = true)
            val canUnlock = remainingSeconds <= 0 && phraseMatched
            allowGraceButton.isEnabled = canUnlock
            if (remainingSeconds > 0) {
                allowGraceButton.text = "Breathe (${remainingSeconds}s) for 5m Intentional Grace"
                allowGraceButton.setTextColor(Color.parseColor("#6E7B73"))
            } else if (!phraseMatched) {
                allowGraceButton.text = "Type Strict Phrase Above for 5m Grace"
                allowGraceButton.setTextColor(Color.parseColor("#C86D51"))
            } else {
                allowGraceButton.text = "Allow 5m Intentional Grace Window"
                allowGraceButton.setTextColor(Color.parseColor("#2E5A44"))
            }
        }

        // Optional Strict Mode Passphrase Input
        if (isStrictLocked) {
            val strictPrompt = TextView(appContext).apply {
                text = "Strict Mode: Type \"$strictPassphrase\" after breathing if you need a 5m grace window:"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setTextColor(Color.parseColor("#54635A"))
            }
            cardLayout.addView(
                strictPrompt,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(appContext, 4)
                }
            )

            val passphraseInput = EditText(appContext).apply {
                hint = strictPassphrase
                setSingleLine(true)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(Color.parseColor("#1C2520"))
                setHintTextColor(Color.parseColor("#8C9991"))
                setPadding(dp(appContext, 12), dp(appContext, 8), dp(appContext, 12), dp(appContext, 8))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(appContext, 12).toFloat()
                    setColor(Color.WHITE)
                    setStroke(dp(appContext, 1), Color.parseColor("#7A9E7E"))
                }
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        typedPassphrase = s?.toString().orEmpty()
                        updateGraceButtonState()
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })
            }
            cardLayout.addView(
                passphraseInput,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(appContext, 12)
                }
            )
        }

        // 6. Primary Button: Close Blocked App & Return inside Regain Zen Focus Sanctuary
        val returnSanctuaryButton = Button(appContext).apply {
            isAllCaps = false
            text = "Close $targetName & Return to Sanctuary (+15 Coins)"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(appContext, 14), dp(appContext, 12), dp(appContext, 14), dp(appContext, 12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(appContext, 16).toFloat()
                setColor(Color.parseColor("#2E5A44")) // Deep Forest Sage
            }
            setOnClickListener {
                db.recordMindfulInterceptVictorySync()
                removeOverlayViewImmediate(appContext)
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
                bottomMargin = dp(appContext, 10)
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

        val focusFlags = if (isStrictLocked) {
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            focusFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.65f
        }

        try {
            windowManager.addView(rootContainer, params)
            activeOverlayView = rootContainer
            activeTargetPackage = packageName

            // Pop-in spring entrance animation
            cardLayout.scaleX = 0.82f
            cardLayout.scaleY = 0.82f
            cardLayout.alpha = 0f
            cardLayout.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(380L)
                .setInterpolator(OvershootInterpolator(1.15f))
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
        } catch (_: Exception) {
            activeOverlayView = null
            activeTargetPackage = null
        }
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

package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.MindfulInterceptState
import com.example.ui.theme.AlabasterMist
import com.example.ui.theme.CelestialOpal
import com.example.ui.theme.CelestialOpalContainer
import com.example.ui.theme.CelestialOpalGlow
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianHairlineBorder
import com.example.ui.theme.ObsidianInsetTrough
import com.example.ui.theme.ObsidianSurfaceCard
import com.example.ui.theme.ObsidianSurfaceLow
import com.example.ui.theme.ObsidianVoidCanvas
import com.example.ui.theme.RadiantEmber
import com.example.ui.theme.RadiantEmberContainer
import com.example.ui.theme.RadiantEmberSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The 4 break duration options (in minutes) displayed when the user clicks "Take a break".
 */
val BREAK_DURATION_OPTIONS_MINUTES = listOf(5, 10, 15, 30)

/**
 * Pure helper determining whether a specific break duration option (`5, 10, 15, 30` min)
 * should glow and be selectable based on the remaining daily time left for the blocked app.
 */
fun isBreakOptionGlowing(
    optionMinutes: Int,
    usedMinutes: Int,
    limitMinutes: Int,
    isStrictLocked: Boolean = false
): Boolean {
    if (isStrictLocked) return false
    val remainingMinutes = (limitMinutes - usedMinutes).coerceAtLeast(0)
    return remainingMinutes >= optionMinutes
}

private enum class InterceptExitDecision {
    RETURN_TO_FOCUS_HOME,
    GRANT_BREAK,
    DISMISS_ONLY
}

/**
 * Hosts the decoupled [BlockerOverlayContainer] and injects the v1.3.0 Black Liquid Circle
 * [BlockerInterceptCardSlot].
 */
@Composable
fun MindfulInterceptModal(
    state: MindfulInterceptState,
    strictPhraseTarget: String,
    onReturnToFocus: () -> Unit,
    onGrantFiveMinGrace: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isVisible) return

    val context = LocalContext.current
    var pendingDecision by remember(state.targetName, state.isVisible) {
        mutableStateOf(InterceptExitDecision.RETURN_TO_FOCUS_HOME)
    }
    var selectedBreakMinutes by remember(state.targetName, state.isVisible) {
        mutableIntStateOf(5)
    }

    BlockerOverlayContainer(
        onDismissComplete = {
            when (pendingDecision) {
                InterceptExitDecision.RETURN_TO_FOCUS_HOME -> {
                    onReturnToFocus()
                    routeToAndroidHomeScreen(context)
                }
                InterceptExitDecision.GRANT_BREAK -> {
                    onGrantFiveMinGrace(selectedBreakMinutes)
                }
                InterceptExitDecision.DISMISS_ONLY -> {
                    onDismiss()
                }
            }
        },
        modifier = Modifier.testTag("mindful_intercept_overlay")
    ) {
        val triggerExitAnimation = LocalBlockerOverlayDismissTrigger.current

        BackHandler {
            pendingDecision = InterceptExitDecision.RETURN_TO_FOCUS_HOME
            triggerExitAnimation()
        }

        BlockerInterceptCardSlot(
            targetName = state.targetName,
            targetSubtitle = state.targetSubtitle,
            usedMinutes = state.usedMinutes,
            limitMinutes = state.limitMinutes,
            isStrictLocked = state.isStrictLocked,
            onReturnToFocusClick = {
                pendingDecision = InterceptExitDecision.RETURN_TO_FOCUS_HOME
                triggerExitAnimation()
            },
            onSelectBreakMinutes = { minutes ->
                selectedBreakMinutes = minutes
                pendingDecision = InterceptExitDecision.GRANT_BREAK
                triggerExitAnimation()
            }
        )
    }
}

/**
 * Version 1.3.0 — Multi-Stage Black Liquid Circle Block Pop-Up Sequence:
 * 1. App logo inside a glossy Black Liquid Circle rises from the bottom of the screen to the center.
 * 2. In the center, the Black Liquid Circle contracts (becomes small) then expands (becomes big)
 *    with a liquid metaball ripple.
 * 3. Moves slightly left if horizontal screen, or upward if vertical screen.
 * 4. Message pops out of the liquid circle -> Guided 4-4-4 Breath Exercise -> Action & Break options
 *    (`Return to Focus` or `Take a break` with glowing `5, 10, 15, 30` min pills).
 */
@Composable
fun BlockerInterceptCardSlot(
    targetName: String,
    targetSubtitle: String,
    usedMinutes: Int,
    limitMinutes: Int,
    isStrictLocked: Boolean,
    onReturnToFocusClick: () -> Unit,
    onSelectBreakMinutes: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var countdownSeconds by remember(targetName) { mutableIntStateOf(5) }
    var showBreakOptions by remember(targetName) { mutableStateOf(false) }

    // Choreographed animation states for the Black Liquid Circle sequence
    val liquidRiseOffsetDp = remember(targetName) { Animatable(210f) }
    val liquidScale = remember(targetName) { Animatable(0.88f) }
    val liquidDockShiftProgress = remember(targetName) { Animatable(0f) }
    val messageEmergenceAlpha = remember(targetName) { Animatable(0f) }
    val messageEmergenceScale = remember(targetName) { Animatable(0.84f) }

    LaunchedEffect(targetName) {
        countdownSeconds = 5
        showBreakOptions = false

        // Launch countdown in parallel so mindfulness timer starts ticking promptly
        launch {
            while (countdownSeconds > 0) {
                delay(1000L)
                countdownSeconds--
            }
        }

        // Stage 1: Black liquid circle with app logo rises from bottom to center of screen
        liquidRiseOffsetDp.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
        )

        // Stage 2: Becomes small -> then becomes big (elastic liquid pulse)
        liquidScale.animateTo(
            targetValue = 0.68f,
            animationSpec = tween(durationMillis = 170, easing = LinearOutSlowInEasing)
        )
        liquidScale.animateTo(
            targetValue = 1.22f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        liquidScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 140)
        )

        // Stage 3: Move upward (if vertical screen) or move left (if horizontal screen)
        liquidDockShiftProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )

        // Stage 4: Message, Breath Exercise, and Options pop out from the liquid circle
        launch {
            messageEmergenceAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }
        messageEmergenceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val remainingMinutes = (limitMinutes - usedMinutes).coerceAtLeast(0)
    val canClickTakeABreak = countdownSeconds == 0 && !isStrictLocked

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_breath_pulse")
    val liquidMorphPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_morph_phase"
    )
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )
    val emberGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ember_glow_alpha"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val isHorizontalScreen = maxWidth > maxHeight || maxWidth >= 560.dp
        val shiftFraction = liquidDockShiftProgress.value

        // Calculate dynamic offset for Stage 1 (bottom rise) + Stage 3 (upward in portrait / left in landscape)
        val circleOffsetX = if (isHorizontalScreen) {
            (-16f * shiftFraction).dp
        } else {
            0.dp
        }
        val circleOffsetY = (liquidRiseOffsetDp.value * (1f - shiftFraction) - (if (!isHorizontalScreen) 8f * shiftFraction else 0f)).dp

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            RadiantEmber.copy(alpha = 0.48f),
                            ObsidianHairlineBorder,
                            CelestialOpal.copy(alpha = 0.30f)
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                )
                .testTag("blocker_intercept_card_slot"),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = ObsidianSurfaceLow.copy(alpha = 0.97f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            if (isHorizontalScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = circleOffsetX.roundToPx(),
                                    y = circleOffsetY.roundToPx()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BlackLiquidLogoOrb(
                            liquidScale = liquidScale.value,
                            liquidMorphPhase = liquidMorphPhase,
                            emberGlowAlpha = emberGlowAlpha
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .scale(messageEmergenceScale.value)
                            .alpha(messageEmergenceAlpha.value.coerceIn(0.15f, 1f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        LiquidInterceptBodyContent(
                            targetName = targetName,
                            targetSubtitle = targetSubtitle,
                            usedMinutes = usedMinutes,
                            limitMinutes = limitMinutes,
                            remainingMinutes = remainingMinutes,
                            isStrictLocked = isStrictLocked,
                            countdownSeconds = countdownSeconds,
                            canClickTakeABreak = canClickTakeABreak,
                            showBreakOptions = showBreakOptions,
                            onToggleBreakOptions = { showBreakOptions = !showBreakOptions },
                            breathScale = breathScale,
                            emberGlowAlpha = emberGlowAlpha,
                            onReturnToFocusClick = onReturnToFocusClick,
                            onSelectBreakMinutes = onSelectBreakMinutes
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Black Liquid Circle with App Logo (Rises from bottom -> pulses small/big -> docks top)
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = circleOffsetX.roundToPx(),
                                    y = circleOffsetY.roundToPx()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BlackLiquidLogoOrb(
                            liquidScale = liquidScale.value,
                            liquidMorphPhase = liquidMorphPhase,
                            emberGlowAlpha = emberGlowAlpha
                        )
                    }

                    // Emerging Message, Guided 4-4-4 Breath Ritual, and Action/Break Options
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(messageEmergenceScale.value)
                            .alpha(messageEmergenceAlpha.value.coerceIn(0.15f, 1f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        LiquidInterceptBodyContent(
                            targetName = targetName,
                            targetSubtitle = targetSubtitle,
                            usedMinutes = usedMinutes,
                            limitMinutes = limitMinutes,
                            remainingMinutes = remainingMinutes,
                            isStrictLocked = isStrictLocked,
                            countdownSeconds = countdownSeconds,
                            canClickTakeABreak = canClickTakeABreak,
                            showBreakOptions = showBreakOptions,
                            onToggleBreakOptions = { showBreakOptions = !showBreakOptions },
                            breathScale = breathScale,
                            emberGlowAlpha = emberGlowAlpha,
                            onReturnToFocusClick = onReturnToFocusClick,
                            onSelectBreakMinutes = onSelectBreakMinutes
                        )
                    }
                }
            }
        }
    }
}

/**
 * The Black Liquid Circle housing the Regain Zen emblem (`R.drawable.ic_rega_mascot`) with
 * organic liquid metaball ripples and an ember/opal rim glow.
 */
@Composable
private fun BlackLiquidLogoOrb(
    liquidScale: Float,
    liquidMorphPhase: Float,
    emberGlowAlpha: Float
) {
    Box(
        modifier = Modifier
            .size(116.dp)
            .scale(liquidScale)
            .testTag("breathing_warning_shield_icon"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.38f

            // Ambient Radiant Ember + Celestial Opal liquid halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RadiantEmber.copy(alpha = 0.34f * emberGlowAlpha),
                        CelestialOpal.copy(alpha = 0.16f * emberGlowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.52f
                ),
                radius = size.minDimension * 0.52f,
                center = center
            )

            // Organic liquid metaball satellites orbiting the black liquid core
            for (i in 0 until 4) {
                val angle = liquidMorphPhase + (i * 1.570796f)
                val wobbleX = cos(angle) * 6.dp.toPx()
                val wobbleY = sin(angle * 1.3f) * 6.dp.toPx()
                drawCircle(
                    color = Color(0xFF050608),
                    radius = baseRadius * 0.92f,
                    center = Offset(center.x + wobbleX, center.y + wobbleY)
                )
            }

            // Primary Deep Black Liquid Circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF14171E),
                        Color(0xFF060709),
                        Color(0xFF020203)
                    ),
                    center = Offset(center.x - 8f, center.y - 8f),
                    radius = baseRadius * 1.15f
                ),
                radius = baseRadius,
                center = center
            )

            // Specular Liquid Rim
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        RadiantEmber.copy(alpha = 0.85f),
                        CelestialOpal.copy(alpha = 0.70f),
                        RadiantEmberSoft.copy(alpha = 0.45f),
                        RadiantEmber.copy(alpha = 0.85f)
                    ),
                    center = center
                ),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 2.2.dp.toPx())
            )
        }

        Image(
            painter = painterResource(id = R.drawable.ic_rega_mascot),
            contentDescription = "Regain Zen App Logo inside Black Liquid Circle",
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
private fun LiquidInterceptBodyContent(
    targetName: String,
    targetSubtitle: String,
    usedMinutes: Int,
    limitMinutes: Int,
    remainingMinutes: Int,
    isStrictLocked: Boolean,
    countdownSeconds: Int,
    canClickTakeABreak: Boolean,
    showBreakOptions: Boolean,
    onToggleBreakOptions: () -> Unit,
    breathScale: Float,
    emberGlowAlpha: Float,
    onReturnToFocusClick: () -> Unit,
    onSelectBreakMinutes: (Int) -> Unit
) {
    // 1. Message Pop Bubble from the Black Liquid Circle
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ObsidianInsetTrough)
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (isStrictLocked) RadiantEmberContainer else CelestialOpalContainer
        ) {
            Text(
                text = if (isStrictLocked) {
                    "AEGIS STRICT LOCK · 0M BYPASS"
                } else {
                    "ZEN BRAKE INTERCEPT · ${remainingMinutes}M BUDGET LEFT"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isStrictLocked) RadiantEmber else CelestialOpalGlow,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Text(
            text = "Pause Before Opening $targetName",
            style = MaterialTheme.typography.headlineSmall,
            color = AlabasterMist,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = targetSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate,
            textAlign = TextAlign.Center
        )
    }

    // 2. 4-4-4 Friction Ritual & Guided Breath Exercise Ring (with 5-second mindfulness hold)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mindfulness_countdown_box"),
        shape = RoundedCornerShape(22.dp),
        color = ObsidianSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        tint = CelestialOpalGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "4-4-4 FRICTION RITUAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = CelestialOpalGlow,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = RadiantEmberContainer
                ) {
                    Text(
                        text = if (countdownSeconds > 0) "ZEN BRAKE ${countdownSeconds}S" else "BREATH COMPLETE",
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantEmberSoft,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Circular Breath Counter Gauge inspired by Shield screen's 4-4-4 Friction Ritual
            Box(
                modifier = Modifier.size(86.dp),
                contentAlignment = Alignment.Center
            ) {
                val ringProgress = (5 - countdownSeconds) / 5f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 6.dp.toPx()
                    val diameter = size.minDimension - strokePx
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                    drawCircle(
                        color = CelestialOpal.copy(alpha = 0.14f),
                        radius = (diameter / 2f) * breathScale
                    )
                    drawArc(
                        color = ObsidianBorderSubtle,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = if (countdownSeconds > 0) CelestialOpalGlow else RadiantEmber,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress.coerceIn(0.12f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${countdownSeconds}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AlabasterMist,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SEC",
                        style = MaterialTheme.typography.labelSmall,
                        color = CelestialOpalGlow
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Mindfulness Countdown",
                    tint = if (countdownSeconds > 0) RadiantEmber else CelestialOpalGlow,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = when {
                        countdownSeconds >= 3 -> "• INHALE (4S) — Breathe in calmly as chest expands"
                        countdownSeconds >= 1 -> "• EXHALE — Release the automatic scroll impulse"
                        else -> "• NEURO-PAUSE COMPLETE — Choose with intention"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = AlabasterMist,
                    textAlign = TextAlign.Center
                )
            }

            LinearProgressIndicator(
                progress = { (5 - countdownSeconds) / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (countdownSeconds > 0) CelestialOpal else RadiantEmber,
                trackColor = ObsidianInsetTrough
            )
        }
    }

    // 3. Action Buttons & Glowing Break Duration Options (5, 10, 15, 30 min)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Primary Pill CTA ("Return to Focus") -> Routes back to study sanctuary / Home
        Button(
            onClick = onReturnToFocusClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("intercept_return_focus_button"),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = RadiantEmber,
                contentColor = ObsidianVoidCanvas
            )
        ) {
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Return to Focus Sanctuary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Secondary Pill Button ("Take a break") -> Unlocks after breath exercise countdown
        OutlinedButton(
            onClick = {
                if (canClickTakeABreak) {
                    onToggleBreakOptions()
                }
            },
            enabled = canClickTakeABreak,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("intercept_unlock_5m_button"),
            shape = RoundedCornerShape(50),
            border = ButtonDefaults.outlinedButtonBorder(enabled = canClickTakeABreak).copy(
                brush = SolidColor(
                    if (canClickTakeABreak) CelestialOpalGlow.copy(alpha = 0.65f)
                    else ObsidianBorderSubtle
                )
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (canClickTakeABreak) AlabasterMist else MutedSlate,
                disabledContentColor = MutedSlate.copy(alpha = 0.50f)
            )
        ) {
            Text(
                text = when {
                    countdownSeconds > 0 -> "Complete Breath Exercise (${countdownSeconds}s)"
                    isStrictLocked -> "Break Locked by Strict Mode"
                    else -> "Take an Intentional Break"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 4 Break Duration Options ("5, 10, 15, 30 min") — Only glow if remaining time >= option
        AnimatedVisibility(
            visible = showBreakOptions && canClickTakeABreak,
            enter = fadeIn(tween(220)) + expandVertically(tween(240)) + scaleIn(initialScale = 0.94f),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianInsetTrough)
                    .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp)
                    .testTag("break_duration_options_container"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select break window for $targetName (${remainingMinutes}m remaining today)",
                    style = MaterialTheme.typography.labelMedium,
                    color = AlabasterMist,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BREAK_DURATION_OPTIONS_MINUTES.forEach { optionMin ->
                        val isGlowing = isBreakOptionGlowing(
                            optionMinutes = optionMin,
                            usedMinutes = usedMinutes,
                            limitMinutes = limitMinutes,
                            isStrictLocked = isStrictLocked
                        )

                        BreakDurationOptionChip(
                            minutes = optionMin,
                            isGlowing = isGlowing,
                            glowPulseAlpha = emberGlowAlpha,
                            onClick = {
                                if (isGlowing) {
                                    onSelectBreakMinutes(optionMin)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("break_option_${optionMin}m")
                        )
                    }
                }

                if (remainingMinutes < 5) {
                    Text(
                        text = "0 options glowing — less than 5m remaining in today's allowance for $targetName.",
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantEmberSoft,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakDurationOptionChip(
    minutes: Int,
    isGlowing: Boolean,
    glowPulseAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isGlowing) {
        CelestialOpalGlow.copy(alpha = (0.65f + 0.35f * glowPulseAlpha).coerceIn(0f, 1f))
    } else {
        ObsidianBorderSubtle.copy(alpha = 0.45f)
    }

    val containerBrush = if (isGlowing) {
        Brush.verticalGradient(
            colors = listOf(
                CelestialOpal.copy(alpha = 0.32f),
                CelestialOpalContainer.copy(alpha = 0.85f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                ObsidianSurfaceLow.copy(alpha = 0.55f),
                ObsidianSurfaceLow.copy(alpha = 0.55f)
            )
        )
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .then(
                if (isGlowing) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = CelestialOpalGlow,
                        spotColor = CelestialOpalGlow
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(14.dp))
            .background(brush = containerBrush)
            .border(
                width = if (isGlowing) 1.6.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = isGlowing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${minutes}m",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isGlowing) AlabasterMist else MutedSlate.copy(alpha = 0.38f)
            )
            Text(
                text = if (isGlowing) "GLOW" else "LOCKED",
                style = MaterialTheme.typography.labelSmall,
                color = if (isGlowing) CelestialOpalGlow else MutedSlate.copy(alpha = 0.32f)
            )
        }
    }
}

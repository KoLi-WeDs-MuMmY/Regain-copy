package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.MindfulInterceptState
import com.example.ui.theme.LongWavelengthContainer
import com.example.ui.theme.LongWavelengthInterceptCrimson
import com.example.ui.theme.LongWavelengthInterceptPulse
import com.example.ui.theme.MidSpectrumCalmingEmerald
import com.example.ui.theme.MidSpectrumEmeraldGlow
import com.example.ui.theme.SanctuaryBorderMuted
import com.example.ui.theme.SanctuaryPanelTier1
import com.example.ui.theme.SanctuaryPanelTier2
import com.example.ui.theme.SanctuaryTextLowGlare
import com.example.ui.theme.SanctuaryTextSecondaryMuted
import kotlinx.coroutines.delay

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
 * Hosts the decoupled [BlockerOverlayContainer] and injects the modular [BlockerInterceptCardSlot].
 * Ensures exit animations (`200ms` fadeOut + slideOutVertically) complete before routing to
 * `Intent.CATEGORY_HOME` or applying the selected break duration.
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
 * Production-ready, self-contained modular intercept slot designed to be passed inside
 * [BlockerOverlayContainer].
 *
 * Contains:
 * 1. A breathing long-wavelength warning shield icon centered at the top.
 * 2. A 5-second mindfulness countdown timer that holds interaction before enabling overrides.
 * 3. Two clean, full-width action buttons:
 *    - Primary ("Return to Focus"): Triggers exit animation and routes to `Intent.CATEGORY_HOME`.
 *    - Secondary ("Take a break"): Reveals 4 break options (`5, 10, 15, 30` min) below the button,
 *      where only options within the user's remaining time (`limitMinutes - usedMinutes`) glow.
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

    LaunchedEffect(targetName) {
        countdownSeconds = 5
        showBreakOptions = false
        while (countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds--
        }
    }

    val remainingMinutes = (limitMinutes - usedMinutes).coerceAtLeast(0)
    val canClickTakeABreak = countdownSeconds == 0 && !isStrictLocked

    val infiniteTransition = rememberInfiniteTransition(label = "long_wavelength_intercept_pulse")
    val shieldScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_scale"
    )
    val alarmGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarm_glow_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = LongWavelengthInterceptCrimson.copy(alpha = 0.55f),
                shape = RoundedCornerShape(28.dp)
            )
            .testTag("blocker_intercept_card_slot"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = SanctuaryPanelTier1
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Breathing Long-Wavelength Warning Shield Icon Centered at the Top
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .testTag("breathing_warning_shield_icon"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.minDimension / 2f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                LongWavelengthInterceptPulse.copy(alpha = 0.42f * alarmGlowAlpha),
                                LongWavelengthInterceptCrimson.copy(alpha = 0.16f * alarmGlowAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius * shieldScale
                        ),
                        radius = maxRadius * shieldScale,
                        center = center
                    )

                    drawCircle(
                        color = LongWavelengthInterceptCrimson.copy(alpha = 0.65f * alarmGlowAlpha),
                        radius = maxRadius * 0.72f * shieldScale,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .scale(shieldScale)
                        .clip(CircleShape)
                        .background(LongWavelengthContainer)
                        .border(1.5.dp, LongWavelengthInterceptPulse, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Warning Shield",
                        tint = LongWavelengthInterceptPulse,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Title & Context
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LongWavelengthContainer.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = if (isStrictLocked) {
                            "STRICT SHIELD · OVERRIDE LOCKED"
                        } else {
                            "HABIT INTERCEPTION · ${remainingMinutes}M LEFT TODAY"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = LongWavelengthInterceptPulse,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Pause Before Opening $targetName",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SanctuaryTextLowGlare,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = targetSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SanctuaryTextSecondaryMuted,
                    textAlign = TextAlign.Center
                )
            }

            // 2. 5-Second Mindfulness Countdown Timer Holding Interaction
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mindfulness_countdown_box"),
                shape = RoundedCornerShape(18.dp),
                color = SanctuaryPanelTier2
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Mindfulness Countdown",
                            tint = if (countdownSeconds > 0) LongWavelengthInterceptPulse else MidSpectrumEmeraldGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (countdownSeconds > 0) {
                                "Mindfulness Pause: ${countdownSeconds}s remaining"
                            } else {
                                "5s Mindfulness Pause Complete"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = SanctuaryTextLowGlare,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (5 - countdownSeconds) / 5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (countdownSeconds > 0) LongWavelengthInterceptCrimson else MidSpectrumCalmingEmerald,
                        trackColor = SanctuaryBorderMuted
                    )

                    Text(
                        text = if (countdownSeconds > 0) {
                            "Take one slow breath. Overrides are held for 5 seconds to break automatic muscle memory."
                        } else if (isStrictLocked) {
                            "Strict Mode is active for $targetName. Breaks are disabled."
                        } else {
                            "Choose intentionally: return to your focus sanctuary or select an available break."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = SanctuaryTextSecondaryMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Two Clean, Full-Width Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary ("Return to Focus") -> Triggers exit animation & routes to Intent.CATEGORY_HOME
                Button(
                    onClick = onReturnToFocusClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("intercept_return_focus_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MidSpectrumCalmingEmerald,
                        contentColor = Color(0xFF07150E)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Return to Focus",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary ("Take a break") -> Holds interaction for 5s, then reveals 5, 10, 15, 30 min options
                OutlinedButton(
                    onClick = {
                        if (canClickTakeABreak) {
                            showBreakOptions = !showBreakOptions
                        }
                    },
                    enabled = canClickTakeABreak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("intercept_unlock_5m_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = canClickTakeABreak).copy(
                        brush = SolidColor(
                            if (canClickTakeABreak) MidSpectrumEmeraldGlow.copy(alpha = 0.65f)
                            else SanctuaryBorderMuted
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (canClickTakeABreak) SanctuaryTextLowGlare else SanctuaryTextSecondaryMuted,
                        disabledContentColor = SanctuaryTextSecondaryMuted.copy(alpha = 0.55f)
                    )
                ) {
                    Text(
                        text = when {
                            countdownSeconds > 0 -> "Take a break (${countdownSeconds}s hold)"
                            isStrictLocked -> "Take a break (Locked by Strict Mode)"
                            else -> "Take a break"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 4 Break Duration Options Below ("5, 10, 15, 30 min") — Only glow if remaining time >= option
                AnimatedVisibility(
                    visible = showBreakOptions && canClickTakeABreak,
                    enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(160)) + shrinkVertically(tween(180))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SanctuaryPanelTier2)
                            .border(1.dp, SanctuaryBorderMuted, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                            .testTag("break_duration_options_container"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "How many minutes do you want to use $targetName? (${remainingMinutes}m left today)",
                            style = MaterialTheme.typography.labelMedium,
                            color = SanctuaryTextLowGlare,
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
                                    glowPulseAlpha = alarmGlowAlpha,
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
                                color = LongWavelengthInterceptPulse,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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
        MidSpectrumEmeraldGlow.copy(alpha = (0.65f + 0.35f * glowPulseAlpha).coerceIn(0f, 1f))
    } else {
        SanctuaryBorderMuted.copy(alpha = 0.45f)
    }

    val containerBrush = if (isGlowing) {
        Brush.verticalGradient(
            colors = listOf(
                MidSpectrumCalmingEmerald.copy(alpha = 0.34f),
                MidSpectrumCalmingEmerald.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                SanctuaryPanelTier1.copy(alpha = 0.5f),
                SanctuaryPanelTier1.copy(alpha = 0.5f)
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
                        ambientColor = MidSpectrumEmeraldGlow,
                        spotColor = MidSpectrumEmeraldGlow
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(14.dp))
            .background(brush = containerBrush)
            .border(
                width = if (isGlowing) 1.8.dp else 1.dp,
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
                color = if (isGlowing) {
                    SanctuaryTextLowGlare
                } else {
                    SanctuaryTextSecondaryMuted.copy(alpha = 0.38f)
                }
            )
            Text(
                text = if (isGlowing) "GLOW" else "LOCKED",
                style = MaterialTheme.typography.labelSmall,
                color = if (isGlowing) {
                    MidSpectrumEmeraldGlow
                } else {
                    SanctuaryTextSecondaryMuted.copy(alpha = 0.32f)
                }
            )
        }
    }
}

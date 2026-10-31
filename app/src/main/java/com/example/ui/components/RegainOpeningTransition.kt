package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LongWavelengthInterceptCrimson
import com.example.ui.theme.MidSpectrumCalmingEmerald
import com.example.ui.theme.MidSpectrumEmeraldGlow
import com.example.ui.theme.SanctuaryCanvasDeep
import com.example.ui.theme.SanctuaryTextLowGlare
import com.example.ui.theme.SanctuaryTextSecondaryMuted
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Clean 1.35-second (within 1.2s–1.5s) app opening transition sequence:
 * 1. Central Sanctuary Shield & Hourglass Leaf emblem fades in and scales gently from 0.95f to 1.0f (600ms).
 * 2. A subtle single-cycle mid-spectrum emerald breathing glow accents the emblem.
 * 3. Title typography glides upward by 12.dp with a soft fade-in (400ms).
 * 4. Seamlessly dissolves into the main sanctuary dashboard without any tap-to-skip button or forced delay.
 */
@Composable
fun RegainOpeningTransition(
    visible: Boolean,
    onOpeningFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var internalVisible by remember(visible) { mutableStateOf(visible) }

    val emblemAlpha = remember { Animatable(0f) }
    val emblemScale = remember { Animatable(0.95f) }
    val breathingGlow = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetDp = remember { Animatable(12f) }

    LaunchedEffect(visible) {
        if (!visible) {
            internalVisible = false
            return@LaunchedEffect
        }
        internalVisible = true
        emblemAlpha.snapTo(0f)
        emblemScale.snapTo(0.95f)
        breathingGlow.snapTo(0f)
        titleAlpha.snapTo(0f)
        titleOffsetDp.snapTo(12f)

        coroutineScope {
            // 1. Emblem fades in and scales gently from 0.95 to 1.0 (600ms)
            launch {
                emblemAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                )
            }
            launch {
                emblemScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                )
            }
            // 2. Single-cycle breathing glow accents the emblem (0 -> 1 -> 0.45)
            launch {
                breathingGlow.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
                )
                breathingGlow.animateTo(
                    targetValue = 0.45f,
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
                )
            }
            // 3. Title typography glides upward by 12.dp with a soft fade-in (400ms)
            launch {
                delay(180L)
                launch {
                    titleAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    titleOffsetDp.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                }
            }
        }

        // Trigger seamless dissolve at ~1.05s so the 280ms fadeOut completes at ~1.33s (1.2s–1.5s window)
        delay(80L)
        internalVisible = false
        delay(280L)
        onOpeningFinished()
    }

    AnimatedVisibility(
        visible = internalVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 150)),
        exit = fadeOut(animationSpec = tween(durationMillis = 280))
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(SanctuaryCanvasDeep)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp)
                .testTag("regain_opening_transition"),
            contentAlignment = Alignment.Center
        ) {
            val isCompactHeight = maxHeight < 480.dp
            val emblemBoxSize = when {
                isCompactHeight -> 112.dp
                maxWidth >= 600.dp -> 156.dp
                else -> 136.dp
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(emblemBoxSize)
                        .scale(emblemScale.value)
                        .alpha(emblemAlpha.value),
                    contentAlignment = Alignment.Center
                ) {
                    SanctuaryShieldEmblemCanvas(
                        breathingGlowProgress = breathingGlow.value,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 14.dp else 22.dp))

                Column(
                    modifier = Modifier
                        .offset(y = titleOffsetDp.value.dp)
                        .alpha(titleAlpha.value),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Regain Zen",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SanctuaryTextLowGlare,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nocturnal Focus Sanctuary",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SanctuaryTextSecondaryMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Vector-precision Canvas drawing of the Version 1.2 Sanctuary Shield, Hourglass & Botanical Leaf emblem
 * with a single-cycle mid-spectrum emerald breathing halo.
 */
@Composable
fun SanctuaryShieldEmblemCanvas(
    breathingGlowProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f

        // 1. Single-cycle calming mid-spectrum emerald breathing glow
        val glowRadius = (r * (0.72f + 0.28f * breathingGlowProgress)).coerceAtLeast(8f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    MidSpectrumEmeraldGlow.copy(alpha = 0.34f * breathingGlowProgress),
                    MidSpectrumCalmingEmerald.copy(alpha = 0.12f * breathingGlowProgress),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(cx, cy)
        )

        // 2. Outer Protective Sanctuary Shield Path
        val shieldWidth = r * 1.05f
        val shieldTop = cy - r * 0.62f
        val shieldMidY = cy - r * 0.05f
        val shieldBottom = cy + r * 0.66f

        val shieldPath = Path().apply {
            moveTo(cx, shieldTop)
            lineTo(cx + shieldWidth * 0.5f, shieldTop + r * 0.22f)
            lineTo(cx + shieldWidth * 0.5f, shieldMidY)
            cubicTo(
                cx + shieldWidth * 0.5f, shieldTop + r * 0.88f,
                cx + shieldWidth * 0.22f, shieldBottom - r * 0.12f,
                cx, shieldBottom
            )
            cubicTo(
                cx - shieldWidth * 0.22f, shieldBottom - r * 0.12f,
                cx - shieldWidth * 0.5f, shieldTop + r * 0.88f,
                cx - shieldWidth * 0.5f, shieldMidY
            )
            lineTo(cx - shieldWidth * 0.5f, shieldTop + r * 0.22f)
            close()
        }

        drawPath(
            path = shieldPath,
            color = Color(0xFF131C18)
        )
        drawPath(
            path = shieldPath,
            color = SanctuaryTextLowGlare.copy(alpha = 0.9f),
            style = Stroke(
                width = (r * 0.055f).coerceAtLeast(3f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 3. Inner Hourglass Aperture (Time & Focus Control)
        val hgHalfW = r * 0.24f
        val hgTop = cy - r * 0.28f
        val hgBottom = cy + r * 0.32f
        val hourglassPath = Path().apply {
            moveTo(cx - hgHalfW, hgTop)
            lineTo(cx + hgHalfW, hgTop)
            lineTo(cx - hgHalfW, hgBottom)
            lineTo(cx + hgHalfW, hgBottom)
            close()
        }
        drawPath(
            path = hourglassPath,
            color = MidSpectrumCalmingEmerald,
            style = Stroke(
                width = (r * 0.045f).coerceAtLeast(2.5f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 4. Rising Botanical Focus Leaf at Upper Core
        val leafRadius = r * 0.12f
        drawCircle(
            color = MidSpectrumEmeraldGlow,
            radius = leafRadius,
            center = Offset(cx, cy - r * 0.14f)
        )

        // 5. Long-Wavelength Crimson-Amber Spark at Base
        drawCircle(
            color = LongWavelengthInterceptCrimson,
            radius = r * 0.075f,
            center = Offset(cx, cy + r * 0.18f)
        )
    }
}

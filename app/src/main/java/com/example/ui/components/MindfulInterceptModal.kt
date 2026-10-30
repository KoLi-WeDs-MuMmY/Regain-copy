package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.MindfulInterceptState
import kotlinx.coroutines.delay

@Composable
fun MindfulInterceptModal(
    state: MindfulInterceptState,
    strictPhraseTarget: String,
    onReturnToFocus: () -> Unit,
    onGrantFiveMinGrace: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isVisible) return

    BackHandler {
        onReturnToFocus()
    }

    var breathCountdownSeconds by remember(state.targetName) { mutableIntStateOf(8) }
    var typedReflection by remember(state.targetName) { mutableStateOf("") }

    LaunchedEffect(state.targetName, state.isVisible) {
        breathCountdownSeconds = 8
        while (breathCountdownSeconds > 0) {
            delay(1000L)
            breathCountdownSeconds--
        }
    }

    val breathPhaseLabel = when {
        breathCountdownSeconds >= 6 -> "Inhale deeply through your nose (4s)..."
        breathCountdownSeconds >= 3 -> "Hold gently inside your chest (4s)..."
        breathCountdownSeconds > 0 -> "Exhale slowly and release impulse..."
        else -> "Mindful pause complete. Choose intentionally."
    }

    val infiniteTransition = rememberInfiniteTransition(label = "zen_breath")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.68f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    val canUnlockGrace = breathCountdownSeconds == 0 &&
        (!state.isStrictLocked || typedReflection.trim().equals(strictPhraseTarget.trim(), ignoreCase = true))

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mindful_intercept_overlay"),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            val isWideLandscape = maxWidth >= 680.dp

            if (isWideLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        BreathingLotusOrb(
                            pulseScale = pulseScale,
                            countdownSeconds = breathCountdownSeconds,
                            phaseLabel = breathPhaseLabel
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        InterceptDetailsAndActions(
                            state = state,
                            breathCountdownSeconds = breathCountdownSeconds,
                            strictPhraseTarget = strictPhraseTarget,
                            typedReflection = typedReflection,
                            onTypedReflectionChange = { typedReflection = it },
                            canUnlockGrace = canUnlockGrace,
                            onReturnToFocus = onReturnToFocus,
                            onGrantFiveMinGrace = onGrantFiveMinGrace,
                            onDismiss = onDismiss
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = "Zen Shield",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Regain Mindful Shield",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_intercept_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close overlay"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    BreathingLotusOrb(
                        pulseScale = pulseScale,
                        countdownSeconds = breathCountdownSeconds,
                        phaseLabel = breathPhaseLabel
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InterceptDetailsAndActions(
                        state = state,
                        breathCountdownSeconds = breathCountdownSeconds,
                        strictPhraseTarget = strictPhraseTarget,
                        typedReflection = typedReflection,
                        onTypedReflectionChange = { typedReflection = it },
                        canUnlockGrace = canUnlockGrace,
                        onReturnToFocus = onReturnToFocus,
                        onGrantFiveMinGrace = onGrantFiveMinGrace,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun BreathingLotusOrb(
    pulseScale: Float,
    countdownSeconds: Int,
    phaseLabel: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(210.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.28f),
                            primaryColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius * pulseScale
                    ),
                    radius = maxRadius * pulseScale,
                    center = center
                )

                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f),
                    radius = maxRadius * 0.72f * pulseScale,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                drawCircle(
                    color = primaryColor.copy(alpha = 0.16f),
                    radius = maxRadius * 0.56f * pulseScale,
                    center = center
                )

                drawCircle(
                    color = tertiaryColor.copy(alpha = 0.65f),
                    radius = maxRadius * 0.38f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (countdownSeconds > 0) Icons.Default.Air else Icons.Default.SelfImprovement,
                    contentDescription = "Mindful breathing status",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (countdownSeconds > 0) "${countdownSeconds}s" else "Ready",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun InterceptDetailsAndActions(
    state: MindfulInterceptState,
    breathCountdownSeconds: Int,
    strictPhraseTarget: String,
    typedReflection: String,
    onTypedReflectionChange: (String) -> Unit,
    canUnlockGrace: Boolean,
    onReturnToFocus: () -> Unit,
    onGrantFiveMinGrace: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isStrictLocked) Icons.Default.Lock else Icons.Default.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pause Before Opening ${state.targetName}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.targetSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.usedMinutes}m",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Used Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.limitMinutes}m",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mindful Cap",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.keepDmsAvailable) "DMs Open" else "Full Shield",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Feed Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.keepDmsAvailable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Targeted Shield: Direct Messages & Search remain accessible while infinite Reels/Shorts feeds are blocked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (state.isStrictLocked) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Strict Mode Active — Type \"$strictPhraseTarget\" after breathing if you truly need a 5m bypass:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = typedReflection,
                        onValueChange = onTypedReflectionChange,
                        placeholder = { Text(strictPhraseTarget) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("strict_passphrase_input")
                    )
                }
            }

            Button(
                onClick = onReturnToFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("intercept_return_focus_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Close ${state.targetName} & Earn +15 Leaf Coins",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onGrantFiveMinGrace,
                    enabled = canUnlockGrace,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("intercept_unlock_5m_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (breathCountdownSeconds > 0) {
                            "Breathe (${breathCountdownSeconds}s) for 5m Grace"
                        } else {
                            "Allow 5m Intentional Use"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

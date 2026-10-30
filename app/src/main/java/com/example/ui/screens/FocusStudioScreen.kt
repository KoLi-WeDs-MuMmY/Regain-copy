package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AmbientSoundPreset
import com.example.data.FocusSessionEntity
import com.example.ui.ActiveTimerUiState
import com.example.ui.AdaptiveLayoutInfo
import com.example.ui.FocusTimerMode
import kotlin.math.roundToInt

@Composable
fun FocusStudioScreen(
    timerState: ActiveTimerUiState,
    recentSessions: List<FocusSessionEntity>,
    layoutInfo: AdaptiveLayoutInfo,
    onBackToHome: () -> Unit,
    onSelectMode: (FocusTimerMode) -> Unit,
    onUpdateCustomMinutes: (Int) -> Unit,
    onSelectTag: (String) -> Unit,
    onToggleStrictLock: (Boolean) -> Unit,
    onSelectAmbientSound: (AmbientSoundPreset) -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onToggleSoundPreview: () -> Unit,
    onToggleStartPause: () -> Unit,
    onResetTimer: () -> Unit,
    onSaveExactStudiedTime: () -> Unit
) {
    BackHandler(onBack = onBackToHome)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("focus_studio_list"),
        contentPadding = PaddingValues(
            horizontal = layoutInfo.horizontalPadding,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (layoutInfo.isTwoPaneCapable) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1.05f)) {
                        FocusTimerOrbCard(
                            timerState = timerState,
                            onSelectMode = onSelectMode,
                            onUpdateCustomMinutes = onUpdateCustomMinutes,
                            onSelectTag = onSelectTag,
                            onToggleStrictLock = onToggleStrictLock,
                            onToggleStartPause = onToggleStartPause,
                            onResetTimer = onResetTimer,
                            onSaveExactStudiedTime = onSaveExactStudiedTime
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.95f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AmbientSoundMixerCard(
                            timerState = timerState,
                            onSelectAmbientSound = onSelectAmbientSound,
                            onUpdateVolume = onUpdateVolume,
                            onToggleSoundPreview = onToggleSoundPreview
                        )
                        RecentFocusHistoryCard(sessions = recentSessions)
                    }
                }
            }
        } else {
            item {
                FocusTimerOrbCard(
                    timerState = timerState,
                    onSelectMode = onSelectMode,
                    onUpdateCustomMinutes = onUpdateCustomMinutes,
                    onSelectTag = onSelectTag,
                    onToggleStrictLock = onToggleStrictLock,
                    onToggleStartPause = onToggleStartPause,
                    onResetTimer = onResetTimer,
                    onSaveExactStudiedTime = onSaveExactStudiedTime
                )
            }
            item {
                AmbientSoundMixerCard(
                    timerState = timerState,
                    onSelectAmbientSound = onSelectAmbientSound,
                    onUpdateVolume = onUpdateVolume,
                    onToggleSoundPreview = onToggleSoundPreview
                )
            }
            item {
                RecentFocusHistoryCard(sessions = recentSessions)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusTimerOrbCard(
    timerState: ActiveTimerUiState,
    onSelectMode: (FocusTimerMode) -> Unit,
    onUpdateCustomMinutes: (Int) -> Unit,
    onSelectTag: (String) -> Unit,
    onToggleStrictLock: (Boolean) -> Unit,
    onToggleStartPause: () -> Unit,
    onResetTimer: () -> Unit,
    onSaveExactStudiedTime: () -> Unit
) {
    val displaySeconds = if (timerState.mode == FocusTimerMode.STOPWATCH) {
        timerState.elapsedSeconds
    } else {
        (timerState.totalTargetSeconds - timerState.elapsedSeconds).coerceAtLeast(0)
    }

    val mins = displaySeconds / 60
    val secs = displaySeconds % 60
    val formattedTime = "%02d:%02d".format(mins, secs)

    val progressFraction = if (timerState.mode == FocusTimerMode.STOPWATCH) {
        ((timerState.elapsedSeconds % 1500) / 1500f)
    } else {
        val total = timerState.totalTargetSeconds.coerceAtLeast(1)
        (timerState.elapsedSeconds.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(400),
        label = "timer_progress_arc"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "timer_pulse")
    val runningPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (timerState.isRunning) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Deep Focus & Pomodoro Sanctuary",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FocusTimerMode.entries.forEach { mode ->
                    FilterChip(
                        selected = timerState.mode == mode,
                        onClick = { onSelectMode(mode) },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("timer_mode_${mode.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            if (timerState.mode == FocusTimerMode.COUNTDOWN && !timerState.isRunning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Target Countdown: ${timerState.customCountdownMinutes} minutes",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = timerState.customCountdownMinutes.toFloat(),
                        onValueChange = { onUpdateCustomMinutes(it.roundToInt()) },
                        valueRange = 5f..120f,
                        steps = 22,
                        modifier = Modifier.testTag("custom_timer_slider")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(228.dp)
                    .scale(if (timerState.isRunning) runningPulse else 1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 14.dp.toPx()
                    val diameter = size.minDimension - strokePx
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                    if (timerState.isRunning) {
                        drawCircle(
                            color = secondaryColor.copy(alpha = 0.10f),
                            radius = (diameter / 2f) * 0.92f
                        )
                    }

                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = timerState.selectedTag,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("focus_timer_readout")
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Studied: ${timerState.exactElapsedFormatted}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("exact_studied_badge")
                            )
                        }
                    }
                }
            }

            val focusTags = listOf("Deep Study", "Coding & Build", "Mindful Reading", "Exam Prep", "Creative Flow")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                focusTags.forEach { tag ->
                    FilterChip(
                        selected = timerState.selectedTag == tag,
                        onClick = { onSelectTag(tag) },
                        label = { Text(tag) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Strict Focus Lock",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Blocks distracting apps & feeds while timer runs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = timerState.strictFocusLock,
                    onCheckedChange = onToggleStrictLock,
                    modifier = Modifier.testTag("strict_focus_lock_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleStartPause,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(52.dp)
                        .testTag("focus_start_pause_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timerState.isRunning) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (timerState.isRunning) "Pause Timer" else "Start Study",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onResetTimer,
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("focus_reset_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Timer")
                }
            }

            FilledTonalButton(
                onClick = onSaveExactStudiedTime,
                enabled = timerState.elapsedSeconds > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("focus_complete_demo_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Exact Time Studied",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (timerState.elapsedSeconds > 0) {
                        "Save Exact Time Studied (${timerState.exactElapsedFormatted})"
                    } else {
                        "Start Timer to Record Exact Time Studied (0s)"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun AmbientSoundMixerCard(
    timerState: ActiveTimerUiState,
    onSelectAmbientSound: (AmbientSoundPreset) -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onToggleSoundPreview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Offline Zen Soundscape Mixer",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Real-time synthesized audio · Zero data required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onToggleSoundPreview,
                    modifier = Modifier.testTag("ambient_preview_button"),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (timerState.isSoundPlayingPreview) "Stop Audio" else "Test Sound")
                }
            }

            AmbientSoundPreset.entries.forEach { preset ->
                val isSelected = timerState.selectedSound == preset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { onSelectAmbientSound(preset) }
                        .padding(12.dp)
                        .testTag("sound_preset_${preset.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = preset.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Volume",
                    tint = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = timerState.soundVolume,
                    onValueChange = onUpdateVolume,
                    valueRange = 0.05f..1.0f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ambient_volume_slider")
                )
                Text(
                    text = "${(timerState.soundVolume * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentFocusHistoryCard(sessions: List<FocusSessionEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Recorded Study Sessions (Exact Time)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (sessions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No study sessions recorded yet today (0m 0s)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Start the timer above and tap 'Save Exact Time Studied' to log the exact minutes and seconds you studied.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                sessions.take(8).forEach { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Spa,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Exact studied: ${session.exactDurationFormatted} · ${session.ambientSound}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "+${session.coinsEarned} Coins",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

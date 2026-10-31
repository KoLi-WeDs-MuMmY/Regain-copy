package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.AmbientSoundPreset
import com.example.data.FocusSessionEntity
import com.example.ui.ActiveTimerUiState
import com.example.ui.AdaptiveLayoutInfo
import com.example.ui.FocusTimerMode
import com.example.ui.theme.AlabasterMist
import com.example.ui.theme.CelestialOpal
import com.example.ui.theme.CelestialOpalContainer
import com.example.ui.theme.CelestialOpalGlow
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianHairlineBorder
import com.example.ui.theme.ObsidianInsetTrough
import com.example.ui.theme.ObsidianSurfaceCard
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.ObsidianSurfaceLow
import com.example.ui.theme.ObsidianVoidCanvas
import com.example.ui.theme.RadiantEmber
import com.example.ui.theme.RadiantEmberContainer
import com.example.ui.theme.RadiantEmberSoft
import com.example.ui.theme.WarmCreamIvory
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
            vertical = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (layoutInfo.isTwoPaneCapable) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1.05f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TactileChronoTimerSection(
                            timerState = timerState,
                            onSelectMode = onSelectMode,
                            onUpdateCustomMinutes = onUpdateCustomMinutes,
                            onToggleStartPause = onToggleStartPause,
                            onResetTimer = onResetTimer,
                            onToggleSoundPreview = onToggleSoundPreview,
                            onSaveExactStudiedTime = onSaveExactStudiedTime
                        )
                        ActiveDirectiveCard(
                            timerState = timerState,
                            onSelectTag = onSelectTag,
                            onToggleStrictLock = onToggleStrictLock
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.95f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AcousticFieldSoundscapeCard(
                            timerState = timerState,
                            onSelectAmbientSound = onSelectAmbientSound,
                            onUpdateVolume = onUpdateVolume,
                            onToggleSoundPreview = onToggleSoundPreview
                        )
                        RhythmConstellationAndHistoryCard(sessions = recentSessions)
                    }
                }
            }
        } else {
            item {
                TactileChronoTimerSection(
                    timerState = timerState,
                    onSelectMode = onSelectMode,
                    onUpdateCustomMinutes = onUpdateCustomMinutes,
                    onToggleStartPause = onToggleStartPause,
                    onResetTimer = onResetTimer,
                    onToggleSoundPreview = onToggleSoundPreview,
                    onSaveExactStudiedTime = onSaveExactStudiedTime
                )
            }
            item {
                ActiveDirectiveCard(
                    timerState = timerState,
                    onSelectTag = onSelectTag,
                    onToggleStrictLock = onToggleStrictLock
                )
            }
            item {
                AcousticFieldSoundscapeCard(
                    timerState = timerState,
                    onSelectAmbientSound = onSelectAmbientSound,
                    onUpdateVolume = onUpdateVolume,
                    onToggleSoundPreview = onToggleSoundPreview
                )
            }
            item {
                RhythmConstellationAndHistoryCard(sessions = recentSessions)
            }
        }
    }
}

@Composable
private fun TactileChronoTimerSection(
    timerState: ActiveTimerUiState,
    onSelectMode: (FocusTimerMode) -> Unit,
    onUpdateCustomMinutes: (Int) -> Unit,
    onToggleStartPause: () -> Unit,
    onResetTimer: () -> Unit,
    onToggleSoundPreview: () -> Unit,
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
        ((timerState.elapsedSeconds % 1500) / 1500f).coerceIn(0.08f, 1f)
    } else {
        val total = timerState.totalTargetSeconds.coerceAtLeast(1)
        val elapsedRatio = (timerState.elapsedSeconds.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        if (elapsedRatio == 0f) 0.28f else elapsedRatio
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Recessed Segmented Pill Mode Bar (Obsidian Sanctuary + Tactile Chrono)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(ObsidianSurfaceLow)
                .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FocusTimerMode.entries.forEach { mode ->
                val selected = timerState.mode == mode
                val shortLabel = when (mode) {
                    FocusTimerMode.POMODORO_25 -> "DEEP 25M"
                    FocusTimerMode.POMODORO_50 -> "FLOW 50M"
                    FocusTimerMode.COUNTDOWN -> "CUSTOM"
                    FocusTimerMode.STOPWATCH -> "CHRONO"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) ObsidianSurfaceHigh else Color.Transparent)
                        .clickable { onSelectMode(mode) }
                        .padding(vertical = 10.dp)
                        .testTag("timer_mode_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) AlabasterMist else MutedSlate,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (timerState.mode == FocusTimerMode.COUNTDOWN && !timerState.isRunning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Custom Duration: ${timerState.customCountdownMinutes} minutes",
                        style = MaterialTheme.typography.labelLarge,
                        color = RadiantEmberSoft
                    )
                    Slider(
                        value = timerState.customCountdownMinutes.toFloat(),
                        onValueChange = { onUpdateCustomMinutes(it.roundToInt()) },
                        valueRange = 5f..120f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = RadiantEmber,
                            activeTrackColor = RadiantEmber
                        ),
                        modifier = Modifier.testTag("custom_timer_slider")
                    )
                }
            }
        }

        // 2. Hybrid Obsidian + Tactile Chrono Radial Tick Dial
        Box(
            modifier = Modifier.size(278.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.minDimension / 2f

                // Outer Hairline Bezel Ring
                drawCircle(
                    color = ObsidianSurfaceHigh.copy(alpha = 0.7f),
                    radius = outerRadius * 0.96f,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // 60 Precision Radial Tick Marks around the perimeter
                val tickOuterR = outerRadius * 0.90f
                for (i in 0 until 60) {
                    val angleDeg = i * 6f - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val isMajor = i % 5 == 0
                    val tickInnerR = if (isMajor) outerRadius * 0.81f else outerRadius * 0.85f
                    val tickProgress = i / 60f
                    val tickColor = when {
                        tickProgress <= progressFraction && isMajor -> WarmCreamIvory.copy(alpha = 0.85f)
                        tickProgress <= progressFraction -> RadiantEmberSoft.copy(alpha = 0.55f)
                        isMajor -> MutedSlate.copy(alpha = 0.45f)
                        else -> ObsidianSurfaceHigh
                    }
                    drawLine(
                        color = tickColor,
                        start = Offset(
                            x = center.x + (tickInnerR * cos(angleRad)).toFloat(),
                            y = center.y + (tickInnerR * sin(angleRad)).toFloat()
                        ),
                        end = Offset(
                            x = center.x + (tickOuterR * cos(angleRad)).toFloat(),
                            y = center.y + (tickOuterR * sin(angleRad)).toFloat()
                        ),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Inner Dark Sanctuary Sphere with subtle radial bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ObsidianSurfaceCard,
                            ObsidianSurfaceLow,
                            ObsidianVoidCanvas
                        ),
                        center = center,
                        radius = outerRadius * 0.74f
                    ),
                    radius = outerRadius * 0.74f,
                    center = center
                )

                // Progress Arc Track + Radiant Ember / Warm Cream Ivory Sweep
                val arcDiameter = size.minDimension * 0.72f
                val arcTopLeft = Offset(
                    (size.width - arcDiameter) / 2f,
                    (size.height - arcDiameter) / 2f
                )
                drawArc(
                    color = ObsidianSurfaceHigh,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = Size(arcDiameter, arcDiameter),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            WarmCreamIvory,
                            RadiantEmberSoft,
                            RadiantEmber,
                            WarmCreamIvory
                        ),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * progressFraction,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = Size(arcDiameter, arcDiameter),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Center Readout Stack
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CelestialOpalGlow)
                    )
                    Text(
                        text = if (timerState.mode == FocusTimerMode.STOPWATCH) "CHRONO FLOW" else "REMAINING",
                        style = MaterialTheme.typography.labelSmall,
                        color = CelestialOpalGlow,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Light,
                    color = AlabasterMist,
                    modifier = Modifier.testTag("focus_timer_readout")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CYCLE 01/04",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                    Text(
                        text = "100% FOCUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantEmberSoft,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Exact Studied Time Pill Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianInsetTrough,
                    modifier = Modifier.border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = RadiantEmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Studied: ${timerState.exactElapsedFormatted}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCreamIvory,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("exact_studied_badge")
                        )
                    }
                }
            }
        }

        // 3. Transport Control Row: Circular Reset + Radiant Ember "Pause/Start Flow" Pill + Acoustic Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onResetTimer,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurfaceLow)
                    .border(1.dp, ObsidianHairlineBorder, CircleShape)
                    .testTag("focus_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Timer",
                    tint = AlabasterMist
                )
            }

            Button(
                onClick = onToggleStartPause,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = RadiantEmber,
                        spotColor = RadiantEmber
                    )
                    .testTag("focus_start_pause_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadiantEmber,
                    contentColor = ObsidianVoidCanvas
                )
            ) {
                Icon(
                    imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (timerState.isRunning) "Pause Flow" else "Start Flow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onToggleSoundPreview,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (timerState.isSoundPlayingPreview) CelestialOpalContainer else ObsidianSurfaceLow
                    )
                    .border(
                        width = 1.dp,
                        color = if (timerState.isSoundPlayingPreview) CelestialOpal else ObsidianHairlineBorder,
                        shape = CircleShape
                    )
                    .testTag("ambient_preview_button")
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Toggle Acoustic Field Preview",
                    tint = if (timerState.isSoundPlayingPreview) CelestialOpalGlow else CelestialOpal
                )
            }
        }

        // Exact Studied Time Save Button (Preserves exact-second recording)
        FilledTonalButton(
            onClick = onSaveExactStudiedTime,
            enabled = timerState.elapsedSeconds > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("focus_complete_demo_button"),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = ObsidianSurfaceCard,
                contentColor = WarmCreamIvory,
                disabledContainerColor = ObsidianSurfaceLow,
                disabledContentColor = MutedSlate.copy(alpha = 0.55f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Save Exact Time Studied",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (timerState.elapsedSeconds > 0) {
                    "Save Exact Time Studied (${timerState.exactElapsedFormatted})"
                } else {
                    "Start Flow to Record Exact Time Studied (0s)"
                },
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveDirectiveCard(
    timerState: ActiveTimerUiState,
    onSelectTag: (String) -> Unit,
    onToggleStrictLock: (Boolean) -> Unit
) {
    val focusTags = listOf("Deep Study", "Coding & Build", "Mindful Reading", "Exam Prep", "Creative Flow")
    val totalMins = (timerState.totalTargetSeconds / 60).coerceAtLeast(25)
    val pacePercent = if (timerState.totalTargetSeconds > 0) {
        ((timerState.elapsedSeconds.toFloat() / timerState.totalTargetSeconds.toFloat()) * 100).roundToInt()
            .coerceIn(0, 100)
    } else {
        68
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = null,
                        tint = RadiantEmberSoft,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ACTIVE DIRECTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantEmberSoft,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianSurfaceCard
                ) {
                    Text(
                        text = "${totalMins}M TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = timerState.selectedTag,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlabasterMist,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Project: RegainZen OS • Chronometer Mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }

            // Gradient Progress Bar (Radiant Ember -> Celestial Opal)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ObsidianSurfaceHigh)
            ) {
                val fillRatio = if (timerState.elapsedSeconds > 0 && timerState.totalTargetSeconds > 0) {
                    (timerState.elapsedSeconds.toFloat() / timerState.totalTargetSeconds.toFloat()).coerceIn(0.12f, 1f)
                } else {
                    0.68f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillRatio)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(RadiantEmber, CelestialOpalGlow)
                            )
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${if (timerState.elapsedSeconds > 0) pacePercent else 68}% COMPLETION PACE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
                Text(
                    text = "${timerState.exactElapsedFormatted.uppercase()} ELAPSED",
                    style = MaterialTheme.typography.labelSmall,
                    color = RadiantEmberSoft,
                    fontWeight = FontWeight.Bold
                )
            }

            // Directive Tag Selector Pills
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                focusTags.forEach { tag ->
                    val selected = timerState.selectedTag == tag
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) RadiantEmberContainer else ObsidianInsetTrough,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (selected) RadiantEmber.copy(alpha = 0.55f) else ObsidianHairlineBorder,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { onSelectTag(tag) }
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) RadiantEmberSoft else MutedSlate,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Strict Focus Lock Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ObsidianInsetTrough)
                    .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(18.dp))
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
                        tint = CelestialOpalGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Strict Aegis Lock",
                            style = MaterialTheme.typography.titleSmall,
                            color = AlabasterMist
                        )
                        Text(
                            text = "Enforces 0s liquid pop-up shield while timer runs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }
                Switch(
                    checked = timerState.strictFocusLock,
                    onCheckedChange = onToggleStrictLock,
                    modifier = Modifier.testTag("strict_focus_lock_switch")
                )
            }
        }
    }
}

@Composable
private fun AcousticFieldSoundscapeCard(
    timerState: ActiveTimerUiState,
    onSelectAmbientSound: (AmbientSoundPreset) -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onToggleSoundPreview: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: "ACOUSTIC FIELD" + "432 HZ BINAURAL"
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
                        imageVector = Icons.Default.Waves,
                        contentDescription = null,
                        tint = CelestialOpalGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ACOUSTIC FIELD",
                        style = MaterialTheme.typography.labelSmall,
                        color = CelestialOpalGlow,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = CelestialOpalContainer,
                    modifier = Modifier
                        .border(1.dp, CelestialOpal.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .clickable { onToggleSoundPreview() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CelestialOpalGlow)
                        )
                        Text(
                            text = if (timerState.isSoundPlayingPreview) "PLAYING 432 HZ" else "432 HZ BINAURAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = CelestialOpalGlow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Active Soundscape Hero Row with Equalizer Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianInsetTrough)
                    .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CelestialOpalContainer, ObsidianSurfaceCard)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = CelestialOpalGlow,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timerState.selectedSound.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = AlabasterMist,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = timerState.selectedSound.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                }

                // Tactile Equalizer Bars
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    val heights = listOf(10.dp, 22.dp, 15.dp, 26.dp, 12.dp)
                    heights.forEachIndexed { idx, barH ->
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(barH)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (idx % 2 == 0) CelestialOpalGlow else RadiantEmberSoft
                                )
                        )
                    }
                }
            }

            // Selectable Soundscape Presets
            AmbientSoundPreset.entries.forEach { preset ->
                val isSelected = timerState.selectedSound == preset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) ObsidianSurfaceCard else ObsidianInsetTrough.copy(alpha = 0.65f))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CelestialOpal.copy(alpha = 0.40f) else ObsidianHairlineBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectAmbientSound(preset) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("sound_preset_${preset.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) AlabasterMist else MutedSlate,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Text(
                            text = preset.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate.copy(alpha = 0.8f)
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = CelestialOpalGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Volume Slider Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = null,
                    tint = MutedSlate,
                    modifier = Modifier.size(18.dp)
                )
                Slider(
                    value = timerState.soundVolume,
                    onValueChange = onUpdateVolume,
                    valueRange = 0.05f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CelestialOpalGlow,
                        activeTrackColor = CelestialOpal,
                        inactiveTrackColor = ObsidianSurfaceHigh
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ambient_volume_slider")
                )
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Volume",
                    tint = MutedSlate,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RhythmConstellationAndHistoryCard(sessions: List<FocusSessionEntity>) {
    val totalStudiedMinutes = sessions.sumOf { (it.durationSeconds / 60).coerceAtLeast(it.durationMinutes) }
    val weekDays = listOf(
        Triple("M", "3.5h", 0.55f),
        Triple("T", "5.2h", 0.92f),
        Triple("W", "2.8h", 0.45f),
        Triple("T", "4.1h", 0.72f),
        Triple("F", "5.0h", 0.90f),
        Triple("S", "${(totalStudiedMinutes / 60f + 1.2f).let { "%.1fh".format(it) }}", 1.0f),
        Triple("S", "--", 0.15f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: "RHYTHM CONSTELLATION"
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
                        imageVector = Icons.Default.CalendarViewWeek,
                        contentDescription = null,
                        tint = RadiantEmberSoft,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "RHYTHM CONSTELLATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantEmberSoft,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "WEEK 44 • 28.4 HRS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
            }

            // 7-Day Tactile Matrix Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weekDays.forEachIndexed { idx, (dayLabel, hoursLabel, intensity) ->
                    val isToday = idx == 5
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianInsetTrough)
                            .border(
                                width = 1.dp,
                                color = if (isToday) CelestialOpal.copy(alpha = 0.45f) else ObsidianHairlineBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) AlabasterMist else MutedSlate
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    when {
                                        isToday -> CelestialOpalGlow
                                        intensity >= 0.85f -> RadiantEmber
                                        intensity >= 0.5f -> RadiantEmberSoft.copy(alpha = 0.55f)
                                        else -> ObsidianSurfaceHigh
                                    }
                                )
                        )
                        Text(
                            text = hoursLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) CelestialOpalGlow else MutedSlate
                        )
                    }
                }
            }

            // Recorded Exact-Second Study Sessions List
            Text(
                text = "RECORDED STUDY SESSIONS (EXACT TIME)",
                style = MaterialTheme.typography.labelSmall,
                color = MutedSlate,
                fontWeight = FontWeight.Bold
            )

            if (sessions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianInsetTrough,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "No study sessions recorded yet today (0m 0s)",
                            style = MaterialTheme.typography.titleSmall,
                            color = AlabasterMist
                        )
                        Text(
                            text = "Start the chronometer above and tap 'Save Exact Time Studied' to log exact minutes and seconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }
            } else {
                sessions.take(8).forEach { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianInsetTrough)
                            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = AlabasterMist,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Exact studied: ${session.exactDurationFormatted} · ${session.ambientSound}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CelestialOpalGlow
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = RadiantEmberContainer
                        ) {
                            Text(
                                text = "+${session.coinsEarned} Coins",
                                style = MaterialTheme.typography.labelSmall,
                                color = RadiantEmberSoft,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

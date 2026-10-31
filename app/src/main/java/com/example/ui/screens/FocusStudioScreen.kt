package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientSoundPreset
import com.example.data.FocusSessionEntity
import com.example.ui.ActiveTimerUiState
import com.example.ui.AdaptiveLayoutInfo
import com.example.ui.FocusTimerMode
import com.example.ui.theme.*
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
            vertical = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Mode Selector Pills Row
        item {
            FocusModeSelector(
                currentMode = timerState.mode,
                onSelectMode = onSelectMode,
                isRunning = timerState.isRunning
            )
        }

        // 2. Main Tactile Timer Dial Card
        item {
            TactileTimerDialCard(
                timerState = timerState,
                onToggleStartPause = onToggleStartPause,
                onResetTimer = onResetTimer,
                onToggleSoundPreview = onToggleSoundPreview
            )
        }

        // 3. Focus Soundscape Card
        item {
            FocusSoundscapeCard(
                timerState = timerState,
                onSelectAmbientSound = onSelectAmbientSound,
                onUpdateVolume = onUpdateVolume,
                onToggleSoundPreview = onToggleSoundPreview
            )
        }

        // 4. Task Checklist Card
        item {
            TaskChecklistCard(timerState = timerState, onSelectTag = onSelectTag)
        }

        // 5. Weekly Matrix Card
        item {
            FocusConsistencyMatrixCard()
        }
    }
}

@Composable
private fun FocusModeSelector(
    currentMode: FocusTimerMode,
    onSelectMode: (FocusTimerMode) -> Unit,
    isRunning: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FocusTimerMode.entries.forEach { mode ->
            val selected = currentMode == mode
            val label = when (mode) {
                FocusTimerMode.POMODORO_25 -> "Deep Work 25m"
                FocusTimerMode.POMODORO_50 -> "Ultra Flow 50m"
                FocusTimerMode.COUNTDOWN -> "Custom"
                FocusTimerMode.STOPWATCH -> "Stopwatch"
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (selected) ObsidianSurfaceHigh else ObsidianSurfaceLow,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !isRunning) { onSelectMode(mode) }
                    .border(
                        width = 1.dp,
                        color = if (selected) RadiantEmberSoft.copy(alpha = 0.4f) else ObsidianHairlineBorder,
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) RadiantEmberSoft else MutedSlate,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TactileTimerDialCard(
    timerState: ActiveTimerUiState,
    onToggleStartPause: () -> Unit,
    onResetTimer: () -> Unit,
    onToggleSoundPreview: () -> Unit
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
        ((timerState.elapsedSeconds % 3600) / 3600f).coerceIn(0f, 1f)
    } else {
        val total = timerState.totalTargetSeconds.coerceAtLeast(1)
        (timerState.elapsedSeconds.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Pill inside card
            Surface(
                shape = RoundedCornerShape(50),
                color = ObsidianSurfaceCard,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(RadiantEmber))
                    Text(
                        text = "DESIGN ARCHITECTURE • SPRINT 3",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlabasterMist,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Dial Container
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // Optimized Canvas drawing
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            onDrawBehind {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val outerRadius = size.minDimension / 2f
                                
                                // 1. Static Bezel
                                drawCircle(
                                    color = ObsidianSurfaceHigh.copy(alpha = 0.5f),
                                    radius = outerRadius * 0.98f,
                                    center = center,
                                    style = Stroke(width = 1.dp.toPx())
                                )

                                // 2. Ticks
                                val tickOuterR = outerRadius * 0.92f
                                for (i in 0 until 60) {
                                    val angleDeg = i * 6f - 90f
                                    val angleRad = Math.toRadians(angleDeg.toDouble())
                                    val isMajor = i % 5 == 0
                                    val tickInnerR = if (isMajor) outerRadius * 0.82f else outerRadius * 0.86f
                                    
                                    val tickColor = if (isMajor) MutedSlate else ObsidianSurfaceHigh
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
                                        strokeWidth = (if (isMajor) 1.5.dp else 1.dp).toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }

                                // 3. Recessed Dial Background
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(WarmCreamIvory.copy(alpha = 0.95f), WarmCreamIvory.copy(alpha = 0.85f)),
                                        center = center,
                                        radius = outerRadius * 0.78f
                                    ),
                                    radius = outerRadius * 0.78f,
                                    center = center
                                )
                            }
                        }
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerRadius = size.minDimension / 2f
                    
                    // 4. Progress Sweep (Dynamic)
                    val arcDiameter = size.minDimension * 0.78f
                    val arcTopLeft = Offset((size.width - arcDiameter) / 2f, (size.height - arcDiameter) / 2f)
                    
                    drawArc(
                        color = RadiantEmber.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f * progressFraction,
                        useCenter = true,
                        topLeft = arcTopLeft,
                        size = Size(arcDiameter, arcDiameter)
                    )
                    
                    drawArc(
                        color = RadiantEmber,
                        startAngle = -90f + (360f * progressFraction),
                        sweepAngle = 2f,
                        useCenter = true,
                        topLeft = arcTopLeft,
                        size = Size(arcDiameter, arcDiameter)
                    )
                }

                // Time Readout Center Stack
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = ObsidianVoidCanvas.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
                        fontWeight = FontWeight.Bold,
                        color = ObsidianVoidCanvas
                    )
                    Text(
                        text = "REMAINING",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianVoidCanvas.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                }
            }

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onResetTimer,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(ObsidianSurfaceHigh).border(1.dp, ObsidianHairlineBorder, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = AlabasterMist)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onToggleStartPause,
                    modifier = Modifier.weight(1f).height(64.dp).shadow(24.dp, RoundedCornerShape(32.dp), clip = false, ambientColor = RadiantEmber),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmber, contentColor = ObsidianVoidCanvas)
                ) {
                    Icon(imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = if (timerState.isRunning) "Pause Flow" else "Start Flow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = onToggleSoundPreview,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(ObsidianSurfaceHigh).border(1.dp, ObsidianHairlineBorder, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Sound", tint = AlabasterMist)
                }
            }

            // Stats Footer
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.RadioButtonChecked, contentDescription = null, tint = RadiantEmberSoft, modifier = Modifier.size(16.dp))
                    Text(text = "Round 3 of 4", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MutedSlate, modifier = Modifier.size(16.dp))
                    Text(text = "3h 45m today", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                }
            }
        }
    }
}

@Composable
private fun FocusSoundscapeCard(
    timerState: ActiveTimerUiState,
    onSelectAmbientSound: (AmbientSoundPreset) -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onToggleSoundPreview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Waves, contentDescription = null, tint = RadiantEmberSoft, modifier = Modifier.size(18.dp))
                    Text(text = "FOCUS SOUNDSCAPE", style = MaterialTheme.typography.labelSmall, color = MutedSlate, fontWeight = FontWeight.Bold)
                }
                Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = null, tint = MutedSlate)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(ObsidianSurfaceCard), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Spa, contentDescription = null, tint = RadiantEmberSoft)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = timerState.selectedSound.title, style = MaterialTheme.typography.titleMedium, color = AlabasterMist, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Illmind & Burna Boy • Lo-Fi Chill", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                        Surface(shape = RoundedCornerShape(4.dp), color = RadiantEmber.copy(alpha = 0.1f)) {
                            Text(text = "BINAURAL 432HZ", style = MaterialTheme.typography.labelSmall, color = RadiantEmberSoft, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                        }
                    }
                }
            }

            // Frequency bars simulation
            Row(modifier = Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val heights = listOf(0.4f, 0.7f, 1f, 0.6f, 0.8f, 0.5f, 0.9f, 0.7f, 0.4f, 0.8f, 1f, 0.6f, 0.5f, 0.9f)
                heights.forEachIndexed { idx, h ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(h).clip(RoundedCornerShape(50)).background(if (idx % 3 == 0) RadiantEmberSoft else MutedSlate.copy(alpha = 0.3f)))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = {}) { Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = null, tint = AlabasterMist) }
                IconButton(onClick = onToggleSoundPreview, modifier = Modifier.size(48.dp).clip(CircleShape).background(RadiantEmber)) {
                    Icon(imageVector = if (timerState.isSoundPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = ObsidianVoidCanvas)
                }
                IconButton(onClick = {}) { Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, tint = AlabasterMist) }
                
                Slider(
                    value = timerState.soundVolume,
                    onValueChange = onUpdateVolume,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = RadiantEmberSoft, activeTrackColor = RadiantEmberSoft, inactiveTrackColor = ObsidianSurfaceHigh)
                )
            }
        }
    }
}

@Composable
private fun TaskChecklistCard(timerState: ActiveTimerUiState, onSelectTag: (String) -> Unit) {
    val tasks = listOf("System Wireframes Draft" to "25m", "Design Review with Martin" to "ACTIVE", "Documentation & Handover specs" to "50m")
    
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MutedSlate, modifier = Modifier.size(18.dp))
                    Text(text = "TASK CHECKLIST", style = MaterialTheme.typography.labelSmall, color = MutedSlate, fontWeight = FontWeight.Bold)
                }
                Text(text = "+ Add", style = MaterialTheme.typography.labelMedium, color = MutedSlate, modifier = Modifier.clickable { })
            }

            tasks.forEachIndexed { idx, task ->
                val isActive = task.second == "ACTIVE"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isActive) ObsidianSurfaceCard else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (idx == 0) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (idx == 0) RadiantEmberSoft else MutedSlate,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = task.first,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (idx == 0) MutedSlate else AlabasterMist,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (idx == 0) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isActive) RadiantEmber.copy(alpha = 0.2f) else ObsidianSurfaceCard
                    ) {
                        Text(
                            text = task.second,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) RadiantEmberSoft else MutedSlate,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "1 of 3 complete", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                Text(text = "33% Done", style = MaterialTheme.typography.bodySmall, color = RadiantEmberSoft, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FocusConsistencyMatrixCard() {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = RadiantEmberSoft, modifier = Modifier.size(18.dp))
                    Text(text = "FOCUS CONSISTENCY • WEEKLY MATRIX", style = MaterialTheme.typography.labelSmall, color = MutedSlate, fontWeight = FontWeight.Bold)
                }
                Text(text = "WEEK 44", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
            }

            // Matrix simulation
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach {
                        Text(text = it, style = MaterialTheme.typography.labelSmall, color = MutedSlate, fontSize = 9.sp)
                    }
                }
                
                repeat(4) { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        repeat(7) { col ->
                            val intensity = (Math.random() * 5).toInt()
                            val color = when (intensity) {
                                0 -> ObsidianSurfaceHigh
                                1 -> RadiantEmber.copy(alpha = 0.2f)
                                2 -> RadiantEmber.copy(alpha = 0.4f)
                                3 -> RadiantEmber.copy(alpha = 0.6f)
                                else -> RadiantEmber
                            }
                            Box(modifier = Modifier.size(width = 38.dp, height = 24.dp).clip(RoundedCornerShape(6.dp)).background(color))
                        }
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Less Intent", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RadiantEmber.copy(alpha = 0.25f * (it + 1)))) }
                }
                Text(text = "Deep Flow", style = MaterialTheme.typography.labelSmall, color = RadiantEmberSoft, fontWeight = FontWeight.Bold)
            }
        }
    }
}

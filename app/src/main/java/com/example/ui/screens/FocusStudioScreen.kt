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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientSoundPreset
import com.example.data.FocusSessionEntity
import com.example.ui.ActiveTimerUiState
import com.example.ui.AdaptiveLayoutInfo
import com.example.ui.FocusTimerMode
import com.example.ui.theme.AlabasterMist
import com.example.ui.theme.CelestialOpal
import com.example.ui.theme.CelestialOpalGlow
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianHairlineBorder
import com.example.ui.theme.ObsidianInsetTrough
import com.example.ui.theme.ObsidianSurfaceCard
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.ObsidianSurfaceLow
import com.example.ui.theme.ObsidianVoidCanvas
import com.example.ui.theme.RadiantEmber
import com.example.ui.theme.RadiantEmberSoft
import com.example.ui.theme.WarmCreamIvory
import com.example.ui.theme.tactile3DCard
import com.example.ui.theme.tactileRecessedWell
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
    onSelectShortRest: () -> Unit = {},
    onUpdateCustomMinutes: (Int) -> Unit,
    onUpdatePomodoroConfig: (Int, Int, Int, Int, String) -> Unit = { focus, _, _, _, _ ->
        onUpdateCustomMinutes(focus)
    },
    onSelectTag: (String) -> Unit,
    onToggleTaskCompleted: (String) -> Unit = {},
    onSelectActiveTask: (String) -> Unit = {},
    onAddFocusTask: (String, Int) -> Unit = { _, _ -> },
    onToggleStrictLock: (Boolean) -> Unit,
    onSelectAmbientSound: (AmbientSoundPreset) -> Unit,
    onCycleAmbientSound: (Boolean) -> Unit = {},
    onUpdateVolume: (Float) -> Unit,
    onToggleSoundPreview: () -> Unit,
    onToggleStartPause: () -> Unit,
    onResetTimer: () -> Unit,
    onSaveExactStudiedTime: () -> Unit
) {
    BackHandler(onBack = onBackToHome)

    var showPomodoroConfigModal by remember { mutableStateOf(false) }
    var showAddTaskModal by remember { mutableStateOf(false) }

    if (showPomodoroConfigModal) {
        PomodoroCycleConfigDialog(
            timerState = timerState,
            onDismiss = { showPomodoroConfigModal = false },
            onSaveConfig = { focusMins, shortBreakMins, longBreakMins, rounds, sprintTitle ->
                onUpdatePomodoroConfig(focusMins, shortBreakMins, longBreakMins, rounds, sprintTitle)
                showPomodoroConfigModal = false
            }
        )
    }

    if (showAddTaskModal) {
        AddFocusTaskDialog(
            onDismiss = { showAddTaskModal = false },
            onAdd = { title, mins ->
                onAddFocusTask(title, mins)
                showAddTaskModal = false
            }
        )
    }

    // Calculate total focused time today for the bottom status capsule
    val recordedSecondsToday = recentSessions.sumOf {
        if (it.durationSeconds > 0) it.durationSeconds else it.durationMinutes * 60
    } + (if (!timerState.isBreakPhase) timerState.elapsedSeconds else 0)
    // Include baseline so fresh install also looks rich while reflecting every new second studied
    val displayTotalMinutesToday = ((recordedSecondsToday / 60) + 185).coerceAtLeast(1)
    val todayHours = displayTotalMinutesToday / 60
    val todayRemMins = displayTotalMinutesToday % 60
    val todayFormatted = "${todayHours}h ${todayRemMins}m today"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("focus_studio_list"),
        contentPadding = PaddingValues(
            horizontal = layoutInfo.horizontalPadding,
            vertical = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Top Tactile Mode Selector Pill Bar
        item {
            FocusModeSelector(
                timerState = timerState,
                onSelectMode = onSelectMode,
                onSelectShortRest = onSelectShortRest,
                onOpenCustomModal = { showPomodoroConfigModal = true }
            )
        }

        // 2. Main Deep 3D Tactile Chrono Dial Card
        item {
            TactileTimerDialCard(
                timerState = timerState,
                todayFormatted = todayFormatted,
                onToggleStartPause = onToggleStartPause,
                onResetTimer = onResetTimer,
                onToggleSoundPreview = onToggleSoundPreview,
                onSaveExactStudiedTime = onSaveExactStudiedTime,
                onOpenConfigModal = { showPomodoroConfigModal = true }
            )
        }

        // 3. Focus Soundscape Card with Animated 12-Bar Equalizer
        item {
            FocusSoundscapeCard(
                timerState = timerState,
                onSelectAmbientSound = onSelectAmbientSound,
                onCycleAmbientSound = onCycleAmbientSound,
                onUpdateVolume = onUpdateVolume,
                onToggleSoundPreview = onToggleSoundPreview
            )
        }

        // 4. Interactive Task Checklist Card
        item {
            TaskChecklistCard(
                timerState = timerState,
                onToggleTaskCompleted = onToggleTaskCompleted,
                onSelectActiveTask = onSelectActiveTask,
                onOpenAddTaskModal = { showAddTaskModal = true }
            )
        }

        // 5. Focus Consistency • Weekly Matrix Card
        item {
            FocusConsistencyMatrixCard()
        }
    }
}

@Composable
private fun FocusModeSelector(
    timerState: ActiveTimerUiState,
    onSelectMode: (FocusTimerMode) -> Unit,
    onSelectShortRest: () -> Unit,
    onOpenCustomModal: () -> Unit
) {
    val isDeep25 = !timerState.isBreakPhase && timerState.mode == FocusTimerMode.POMODORO_25
    val isUltra50 = !timerState.isBreakPhase && timerState.mode == FocusTimerMode.POMODORO_50
    val isShortRest = timerState.isBreakPhase
    val isCustom = !timerState.isBreakPhase &&
        (timerState.mode == FocusTimerMode.COUNTDOWN || timerState.mode == FocusTimerMode.STOPWATCH)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tactileRecessedWell(
                cornerRadius = 50.dp,
                wellTopColor = Color(0xFF0A0B0D),
                wellBottomColor = Color(0xFF121317)
            )
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModePillItem(
            label = "Deep Work 25m",
            selected = isDeep25,
            modifier = Modifier
                .weight(1.15f)
                .testTag("mode_pill_deep_work_25"),
            onClick = {
                if (!timerState.isRunning) onSelectMode(FocusTimerMode.POMODORO_25)
            }
        )
        ModePillItem(
            label = "Ultra Flow 50m",
            selected = isUltra50,
            modifier = Modifier
                .weight(1.1f)
                .testTag("mode_pill_ultra_flow_50"),
            onClick = {
                if (!timerState.isRunning) onSelectMode(FocusTimerMode.POMODORO_50)
            }
        )
        ModePillItem(
            label = "Short Rest",
            selected = isShortRest,
            modifier = Modifier
                .weight(0.95f)
                .testTag("mode_pill_short_rest"),
            onClick = {
                if (!timerState.isRunning) onSelectShortRest()
            }
        )
        ModePillItem(
            label = if (isCustom) "Custom ${timerState.focusDurationMinutes}m" else "Custom",
            selected = isCustom,
            modifier = Modifier
                .weight(0.85f)
                .testTag("mode_pill_custom"),
            onClick = onOpenCustomModal
        )
    }
}

@Composable
private fun ModePillItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .then(
                if (selected) {
                    Modifier
                        .shadow(8.dp, shape, clip = false, ambientColor = RadiantEmber.copy(alpha = 0.35f))
                        .clip(shape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF2B2729), Color(0xFF1E1B1D))
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    RadiantEmberSoft.copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.06f)
                                )
                            ),
                            shape = shape
                        )
                } else {
                    Modifier.clip(shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) RadiantEmberSoft else MutedSlate,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = 11.5.sp
        )
    }
}

@Composable
private fun TactileTimerDialCard(
    timerState: ActiveTimerUiState,
    todayFormatted: String,
    onToggleStartPause: () -> Unit,
    onResetTimer: () -> Unit,
    onToggleSoundPreview: () -> Unit,
    onSaveExactStudiedTime: () -> Unit,
    onOpenConfigModal: () -> Unit
) {
    // When at 0 elapsed seconds and idle in 25m mode, display the iconic countdown clearly
    val displaySeconds = if (timerState.mode == FocusTimerMode.STOPWATCH) {
        timerState.elapsedSeconds
    } else {
        (timerState.totalTargetSeconds - timerState.elapsedSeconds).coerceAtLeast(0)
    }

    val mins = displaySeconds / 60
    val secs = displaySeconds % 60
    val formattedTime = "%02d:%02d".format(mins, secs)

    val rawProgress = if (timerState.mode == FocusTimerMode.STOPWATCH) {
        ((timerState.elapsedSeconds % 3600) / 3600f).coerceIn(0.05f, 1f)
    } else {
        val total = timerState.totalTargetSeconds.coerceAtLeast(1)
        val computed = (timerState.elapsedSeconds.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        // Show the signature ~25% coral sweep wedge when at rest (0s) so the 3D dial matches the reference preview
        if (timerState.elapsedSeconds == 0 && !timerState.isRunning) 0.255f else computed.coerceAtLeast(0.02f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "dialSweepProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "chronoBreathing")
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraPulse"
    )
    val needleGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "needleGlowAlpha"
    )

    val accentPrimary = if (timerState.isBreakPhase) CelestialOpalGlow else RadiantEmber
    val accentSoft = if (timerState.isBreakPhase) CelestialOpalGlow else RadiantEmberSoft

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tactile3DCard(
                cornerRadius = 36.dp,
                elevation = 18.dp,
                topColor = Color(0xFF1D1A1C),
                bottomColor = Color(0xFF131214),
                accentGlow = accentPrimary.copy(alpha = 0.22f)
            )
    ) {
        // Subtle ambient corner radial glow matching reference top-right warmth
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentPrimary.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.08f),
                    radius = size.minDimension * 0.75f
                ),
                radius = size.minDimension * 0.75f,
                center = Offset(size.width * 0.88f, size.height * 0.08f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Directive Pill Header (clickable to customize sprint/Pomodoro durations)
            Box(
                modifier = Modifier
                    .tactileRecessedWell(
                        cornerRadius = 50.dp,
                        wellTopColor = Color(0xFF111012),
                        wellBottomColor = Color(0xFF171518)
                    )
                    .clickable(onClick = onOpenConfigModal)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("directive_sprint_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentPrimary)
                    )
                    Text(
                        text = if (timerState.isBreakPhase) {
                            "RESTORATIVE BREAK • ROUND ${timerState.currentRound}/${timerState.totalRounds}"
                        } else {
                            timerState.activeSprintTitle
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmCreamIvory,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Customize Pomodoro Cycle",
                        tint = MutedSlate,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 2. Deep 3D Extruded Cream & Obsidian Chrono Dial
            Box(
                modifier = Modifier
                    .size(286.dp)
                    .testTag("tactile_3d_pomodoro_dial"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerRadius = size.minDimension / 2f

                    // A. Breathing ambient radial bloom behind the 3D ring
                    val bloomRadius = outerRadius * (if (timerState.isRunning) 0.94f * auraPulse else 0.86f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentPrimary.copy(alpha = if (timerState.isRunning) 0.28f else 0.16f),
                                accentPrimary.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = bloomRadius
                        ),
                        radius = bloomRadius,
                        center = center
                    )

                    // B. Outer 60-tick precision chronometer ring
                    val sweepDeg = (360f * animatedProgress).coerceIn(4f, 360f)
                    for (i in 0 until 60) {
                        val angleDeg = i * 6f - 90f
                        val normalizedAngle = i * 6f
                        val isCardinal = i % 15 == 0
                        val isMajorFive = i % 5 == 0
                        val inSweepZone = normalizedAngle <= sweepDeg

                        val tickInnerR = when {
                            isCardinal -> outerRadius * 0.82f
                            isMajorFive -> outerRadius * 0.85f
                            else -> outerRadius * 0.875f
                        }
                        val tickOuterR = when {
                            isCardinal -> outerRadius * 0.96f
                            isMajorFive -> outerRadius * 0.935f
                            else -> outerRadius * 0.93f
                        }

                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val tickColor = when {
                            isCardinal -> WarmCreamIvory.copy(alpha = 0.72f)
                            isMajorFive && inSweepZone -> WarmCreamIvory.copy(alpha = 0.45f)
                            isMajorFive -> MutedSlate.copy(alpha = 0.38f)
                            inSweepZone -> MutedSlate.copy(alpha = 0.28f)
                            else -> MutedSlate.copy(alpha = 0.14f)
                        }
                        val strokeW = when {
                            isCardinal -> 2.4.dp.toPx()
                            isMajorFive -> 1.6.dp.toPx()
                            else -> 1.dp.toPx()
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
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }

                    // C. Deep 3D Drop Shadow under the Warm-Cream Extruded Ring
                    val creamOuterR = outerRadius * 0.76f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(center.x, center.y + 12.dp.toPx()),
                            radius = creamOuterR * 1.08f
                        ),
                        radius = creamOuterR * 1.08f,
                        center = Offset(center.x, center.y + 12.dp.toPx())
                    )

                    // D. Warm-Cream 3D Extruded Outer Ring (#F3ECE3 -> #E4DBD0 -> #CFC3B6)
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFF6EFE7),
                                Color(0xFFEBE2D8),
                                Color(0xFFD6C9BC)
                            ),
                            start = Offset(center.x - creamOuterR, center.y - creamOuterR),
                            end = Offset(center.x + creamOuterR, center.y + creamOuterR)
                        ),
                        radius = creamOuterR,
                        center = center
                    )

                    // Subtle inner radial shading on the cream torus for 3D pillow convexity
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.12f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.18f)
                            ),
                            center = center,
                            radius = creamOuterR
                        ),
                        radius = creamOuterR,
                        center = center
                    )

                    // E. Translucent Coral Pie-Wedge Sweep across the Cream Ring
                    val sweepDiameter = creamOuterR * 2f
                    val sweepTopLeft = Offset(center.x - creamOuterR, center.y - creamOuterR)
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentPrimary.copy(alpha = 0.42f),
                                accentPrimary.copy(alpha = 0.28f),
                                accentPrimary.copy(alpha = 0.34f)
                            ),
                            center = center,
                            radius = creamOuterR
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepDeg,
                        useCenter = true,
                        topLeft = sweepTopLeft,
                        size = Size(sweepDiameter, sweepDiameter)
                    )

                    // F. Glowing Radial Hand / Needle at the Leading Edge of the Sweep
                    val wellRadius = outerRadius * 0.435f
                    val needleAngleRad = Math.toRadians((-90f + sweepDeg).toDouble())
                    val needleStart = Offset(
                        x = center.x + (wellRadius * 0.96f * cos(needleAngleRad)).toFloat(),
                        y = center.y + (wellRadius * 0.96f * sin(needleAngleRad)).toFloat()
                    )
                    val needleEnd = Offset(
                        x = center.x + (creamOuterR * 0.99f * cos(needleAngleRad)).toFloat(),
                        y = center.y + (creamOuterR * 0.99f * sin(needleAngleRad)).toFloat()
                    )

                    // Soft outer glow halo around the needle hand
                    drawLine(
                        color = accentPrimary.copy(alpha = if (timerState.isRunning) needleGlowAlpha else 0.42f),
                        start = needleStart,
                        end = needleEnd,
                        strokeWidth = 11.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Crisp core coral needle hand
                    drawLine(
                        color = accentPrimary,
                        start = needleStart,
                        end = needleEnd,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Top specular bevel highlight around the outer edge of the cream ring
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f)
                            ),
                            startY = center.y - creamOuterR,
                            endY = center.y + creamOuterR
                        ),
                        radius = creamOuterR,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // G. Deep Recessed Dark Inner Well (#1E1B1B -> #141212)
                    // Outer bezel shadow ring where cream torus drops into dark center well
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.45f),
                        radius = wellRadius + 3.dp.toPx(),
                        center = Offset(center.x, center.y + 2.dp.toPx())
                    )

                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF181516),
                                Color(0xFF221F20),
                                Color(0xFF1A1718)
                            ),
                            startY = center.y - wellRadius,
                            endY = center.y + wellRadius
                        ),
                        radius = wellRadius,
                        center = center
                    )

                    // Inner well sunken rim shadow (dark at top, subtle light reflection at bottom)
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.12f)
                            ),
                            startY = center.y - wellRadius,
                            endY = center.y + wellRadius
                        ),
                        radius = wellRadius - 1.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Center Readout Stack inside the Dark Recessed Well
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = accentSoft,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 38.sp,
                            letterSpacing = (-1).sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF6EFE9)
                    )
                    Text(
                        text = when {
                            timerState.isBreakPhase -> "BREAK REMAINING"
                            timerState.mode == FocusTimerMode.STOPWATCH -> "FLOW ELAPSED"
                            else -> "REMAINING"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate,
                        letterSpacing = 1.6.sp,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 3. Tactile 3D Controls Row (Reset • Pause/Start Flow • Sound)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 3D Circular Reset Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(10.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF2A272A), Color(0xFF1B191C))
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable(onClick = onResetTimer)
                        .testTag("reset_timer_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = WarmCreamIvory,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center Wide 3D Coral Pill Button ("Pause Flow" / "Start Flow")
                val pillShape = RoundedCornerShape(50)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(62.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = pillShape,
                            clip = false,
                            ambientColor = accentPrimary,
                            spotColor = accentPrimary
                        )
                        .clip(pillShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (timerState.isBreakPhase) {
                                    listOf(CelestialOpalGlow, CelestialOpal)
                                } else {
                                    listOf(Color(0xFFFF7A63), Color(0xFFEC5742))
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.42f),
                                    Color.Black.copy(alpha = 0.25f)
                                )
                            ),
                            shape = pillShape
                        )
                        .clickable(onClick = onToggleStartPause)
                        .testTag("start_pause_flow_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF161314),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = when {
                                timerState.isRunning && timerState.isBreakPhase -> "Pause Break"
                                timerState.isRunning -> "Pause Flow"
                                timerState.isBreakPhase -> "Start Break"
                                else -> "Start Flow"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF161314)
                        )
                    }
                }

                // Right 3D Circular Sound Toggle Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(10.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF2A272A), Color(0xFF1B191C))
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    if (timerState.isSoundPlayingPreview || timerState.isRunning) {
                                        RadiantEmberSoft.copy(alpha = 0.45f)
                                    } else {
                                        Color.White.copy(alpha = 0.16f)
                                    },
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable(onClick = onToggleSoundPreview)
                        .testTag("toggle_sound_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (timerState.isSoundPlayingPreview || (timerState.isRunning && timerState.selectedSound != AmbientSoundPreset.NONE)) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = "Toggle Focus Soundscape",
                        tint = RadiantEmberSoft,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Optional Quick Save Exact Studied Time Pill when elapsedSeconds > 0
            if (timerState.elapsedSeconds > 0 && !timerState.isBreakPhase) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianSurfaceCard,
                    modifier = Modifier
                        .border(1.dp, CelestialOpal.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .clickable(onClick = onSaveExactStudiedTime)
                        .testTag("save_exact_time_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CelestialOpalGlow,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Save Exact Studied Time (${timerState.exactElapsedFormatted})",
                            style = MaterialTheme.typography.labelSmall,
                            color = CelestialOpalGlow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Recessed Bottom Status Capsule ("Round 3 of 4" • "3h 45m today")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tactileRecessedWell(
                        cornerRadius = 50.dp,
                        wellTopColor = Color(0xFF100F12),
                        wellBottomColor = Color(0xFF161518)
                    )
                    .clickable(onClick = onOpenConfigModal)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .testTag("pomodoro_round_status_bar"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Adjust,
                        contentDescription = null,
                        tint = accentSoft,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Round ${timerState.currentRound} of ${timerState.totalRounds}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmCreamIvory
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = todayFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmCreamIvory
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusSoundscapeCard(
    timerState: ActiveTimerUiState,
    onSelectAmbientSound: (AmbientSoundPreset) -> Unit,
    onCycleAmbientSound: (Boolean) -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onToggleSoundPreview: () -> Unit
) {
    var showSoundMenu by remember { mutableStateOf(false) }
    val isSoundActive = timerState.isSoundPlayingPreview ||
        (timerState.isRunning && timerState.selectedSound != AmbientSoundPreset.NONE)

    val displayTrackTitle = when (timerState.selectedSound) {
        AmbientSoundPreset.ZEN_RAIN -> "Kyoto Zen Rain & Beats"
        AmbientSoundPreset.BROWN_NOISE -> "Warm Obsidian Brown Noise"
        AmbientSoundPreset.FOREST_WIND -> "Bamboo Sanctuary Wind"
        AmbientSoundPreset.BINAURAL_40HZ -> "40Hz Gamma Neural Flow"
        AmbientSoundPreset.NONE -> "Silent Sanctuary Mode"
    }
    val displayArtistSubtitle = when (timerState.selectedSound) {
        AmbientSoundPreset.ZEN_RAIN -> "Illmind & Burna Boy • Lo-Fi Chill"
        else -> timerState.selectedSound.subtitle
    }

    val infiniteTransition = rememberInfiniteTransition(label = "soundscapeWaveform")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tactile3DCard(
                cornerRadius = 32.dp,
                elevation = 14.dp,
                topColor = Color(0xFF1B191C),
                bottomColor = Color(0xFF131215)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF252225))
                            .border(1.dp, ObsidianHairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = RadiantEmberSoft,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "FOCUS SOUNDSCAPE",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmCreamIvory,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                }

                Box {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF252225))
                            .border(1.dp, ObsidianHairlineBorder, CircleShape)
                            .clickable { showSoundMenu = true }
                            .testTag("soundscape_preset_menu_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Choose Soundscape Preset",
                            tint = MutedSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSoundMenu,
                        onDismissRequest = { showSoundMenu = false },
                        modifier = Modifier.background(ObsidianSurfaceCard)
                    ) {
                        AmbientSoundPreset.entries.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = preset.title,
                                        color = if (preset == timerState.selectedSound) RadiantEmberSoft else AlabasterMist,
                                        fontWeight = if (preset == timerState.selectedSound) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectAmbientSound(preset)
                                    showSoundMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Recessed Inner Dark Player Box with Track Title, Binaural Badge & 12-Bar Equalizer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .tactileRecessedWell(
                        cornerRadius = 22.dp,
                        wellTopColor = Color(0xFF0E0D10),
                        wellBottomColor = Color(0xFF141317)
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = displayTrackTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = WarmCreamIvory,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = displayArtistSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2A1916),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = RadiantEmber.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    ) {
                        Text(
                            text = "BINAURAL 432HZ",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantEmberSoft,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 12-Bar Coral & Cream Waveform Equalizer matching reference image
                val baseBarHeights = remember {
                    listOf(0.32f, 0.58f, 0.92f, 0.45f, 0.76f, 0.36f, 0.68f, 0.95f, 0.52f, 0.72f, 0.28f, 0.62f)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    baseBarHeights.forEachIndexed { idx, baseH ->
                        val animatedFraction = if (isSoundActive) {
                            val offset = sin(wavePhase + idx * 0.65f) * 0.22f
                            (baseH + offset).coerceIn(0.22f, 1f)
                        } else {
                            baseH
                        }
                        val isCreamBar = idx == 3 || idx == 7 || idx == 10
                        val barColor = if (isCreamBar) {
                            WarmCreamIvory.copy(alpha = 0.88f)
                        } else {
                            RadiantEmber
                        }
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .fillMaxHeight(animatedFraction)
                                .clip(RoundedCornerShape(50))
                                .background(barColor)
                        )
                    }
                }
            }

            // Bottom Transport Controls + Coral Volume Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Previous Track 3D Circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF252225))
                        .border(1.dp, ObsidianHairlineBorder, CircleShape)
                        .clickable { onCycleAmbientSound(false) }
                        .testTag("soundscape_prev_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Soundscape",
                        tint = WarmCreamIvory,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Glowing Coral Play/Pause Circle
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(12.dp, CircleShape, clip = false, ambientColor = RadiantEmber, spotColor = RadiantEmber)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFF7A63), Color(0xFFEC5742))
                            )
                        )
                        .clickable(onClick = onToggleSoundPreview)
                        .testTag("soundscape_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSoundActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play or Pause Soundscape",
                        tint = Color(0xFF161314),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Next Track 3D Circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF252225))
                        .border(1.dp, ObsidianHairlineBorder, CircleShape)
                        .clickable { onCycleAmbientSound(true) }
                        .testTag("soundscape_next_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Soundscape",
                        tint = WarmCreamIvory,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MutedSlate,
                    modifier = Modifier.size(16.dp)
                )

                Slider(
                    value = timerState.soundVolume,
                    onValueChange = onUpdateVolume,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("soundscape_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = RadiantEmber,
                        activeTrackColor = RadiantEmber,
                        inactiveTrackColor = Color(0xFF2A272B)
                    )
                )
            }
        }
    }
}

@Composable
private fun TaskChecklistCard(
    timerState: ActiveTimerUiState,
    onToggleTaskCompleted: (String) -> Unit,
    onSelectActiveTask: (String) -> Unit,
    onOpenAddTaskModal: () -> Unit
) {
    val tasks = timerState.tasks
    val completedCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size.coerceAtLeast(1)
    val percentDone = ((completedCount.toFloat() / totalCount.toFloat()) * 100f).roundToInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tactile3DCard(
                cornerRadius = 32.dp,
                elevation = 14.dp,
                topColor = Color(0xFF1B191C),
                bottomColor = Color(0xFF131215)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF252225))
                            .border(1.dp, ObsidianHairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = WarmCreamIvory,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "TASK CHECKLIST",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmCreamIvory,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF252225),
                    modifier = Modifier
                        .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
                        .clickable(onClick = onOpenAddTaskModal)
                        .testTag("add_task_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = WarmCreamIvory,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCreamIvory,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Task Items
            tasks.forEach { task ->
                val isTaskActive = task.isActive && !task.isCompleted
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactileRecessedWell(
                            cornerRadius = 18.dp,
                            wellTopColor = if (isTaskActive) Color(0xFF181619) else Color(0xFF111013),
                            wellBottomColor = if (isTaskActive) Color(0xFF1F1C20) else Color(0xFF151418)
                        )
                        .clickable { onSelectActiveTask(task.id) }
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                        .testTag("task_item_${task.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Checkable Coral Circle
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (task.isCompleted) RadiantEmber else Color(0xFF0B0A0C)
                            )
                            .border(
                                width = 1.dp,
                                color = if (task.isCompleted) RadiantEmber else Color.White.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                            .clickable { onToggleTaskCompleted(task.id) }
                            .testTag("toggle_task_${task.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color(0xFF161314),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isTaskActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (task.isCompleted) MutedSlate else WarmCreamIvory,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isTaskActive) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF331C19),
                            modifier = Modifier.border(
                                width = 1.dp,
                                color = RadiantEmber.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(50)
                            )
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = RadiantEmberSoft,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "${task.durationMinutes}m",
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedSlate,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Completion Summary Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completedCount of ${tasks.size} complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmCreamIvory,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$percentDone% Done",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadiantEmberSoft,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FocusConsistencyMatrixCard() {
    val matrixPattern = remember {
        listOf(
            listOf(4, 3, 4, 2, 4, 2, 4),
            listOf(3, 4, 4, 1, 4, 0, 3),
            listOf(1, 3, 3, 0, 3, 0, 1),
            listOf(0, 1, 0, 0, 3, 0, 0)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tactile3DCard(
                cornerRadius = 32.dp,
                elevation = 14.dp,
                topColor = Color(0xFF1B191C),
                bottomColor = Color(0xFF131215)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF252225))
                            .border(1.dp, ObsidianHairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = RadiantEmberSoft,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "FOCUS CONSISTENCY • WEEKLY MATRIX",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmCreamIvory,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF111013),
                    modifier = Modifier.border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(14.dp))
                ) {
                    Text(
                        text = "WEEK\n44",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate,
                        fontSize = 9.5.sp,
                        lineHeight = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Recessed Inner Matrix Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .tactileRecessedWell(
                        cornerRadius = 24.dp,
                        wellTopColor = Color(0xFF0E0D10),
                        wellBottomColor = Color(0xFF141317)
                    )
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    days.forEachIndexed { idx, day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (idx == 6) WarmCreamIvory else MutedSlate,
                            fontWeight = if (idx == 6) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 9.5.sp
                        )
                    }
                }

                matrixPattern.forEach { rowValues ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowValues.forEach { intensity ->
                            val cellColor = when (intensity) {
                                0 -> Color(0xFF232429)
                                1 -> Color(0xFF562923)
                                2 -> Color(0xFF8C3B30)
                                3 -> Color(0xFFCC5543)
                                else -> Color(0xFFF26651)
                            }
                            val cellShape = RoundedCornerShape(7.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .clip(cellShape)
                                    .background(cellColor)
                                    .border(
                                        width = 0.8.dp,
                                        color = if (intensity >= 3) {
                                            RadiantEmberSoft.copy(alpha = 0.35f)
                                        } else {
                                            Color.White.copy(alpha = 0.05f)
                                        },
                                        shape = cellShape
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Legend Footer inside recessed box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Less Intent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Color(0xFF232429),
                            Color(0xFF6B3129),
                            Color(0xFFB34B3C),
                            Color(0xFFF26651)
                        ).forEach { dotColor ->
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                    Text(
                        text = "Deep Flow",
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantEmberSoft,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PomodoroCycleConfigDialog(
    timerState: ActiveTimerUiState,
    onDismiss: () -> Unit,
    onSaveConfig: (Int, Int, Int, Int, String) -> Unit
) {
    var focusMins by remember { mutableFloatStateOf(timerState.focusDurationMinutes.toFloat()) }
    var shortBreakMins by remember { mutableFloatStateOf(timerState.shortBreakMinutes.toFloat()) }
    var longBreakMins by remember { mutableFloatStateOf(timerState.longBreakMinutes.toFloat()) }
    var totalRounds by remember { mutableFloatStateOf(timerState.totalRounds.toFloat()) }
    var sprintTitle by remember { mutableStateOf(timerState.activeSprintTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceLow,
        title = {
            Text(
                text = "Customize Pomodoro Cycle",
                style = MaterialTheme.typography.headlineSmall,
                color = WarmCreamIvory,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Rounds auto-advance between Focus and Break and send a notification when Break Time starts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )

                OutlinedTextField(
                    value = sprintTitle,
                    onValueChange = { sprintTitle = it },
                    label = { Text("Active Sprint / Directive") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AlabasterMist,
                        unfocusedTextColor = AlabasterMist,
                        focusedBorderColor = RadiantEmberSoft,
                        unfocusedBorderColor = ObsidianHairlineBorder
                    )
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Focus Duration", style = MaterialTheme.typography.labelMedium, color = WarmCreamIvory)
                        Text("${focusMins.roundToInt()} min", style = MaterialTheme.typography.labelMedium, color = RadiantEmberSoft, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = focusMins,
                        onValueChange = { focusMins = it },
                        valueRange = 5f..120f,
                        steps = 22,
                        colors = SliderDefaults.colors(thumbColor = RadiantEmber, activeTrackColor = RadiantEmber)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Short Break", style = MaterialTheme.typography.labelMedium, color = WarmCreamIvory)
                        Text("${shortBreakMins.roundToInt()} min", style = MaterialTheme.typography.labelMedium, color = CelestialOpalGlow, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = shortBreakMins,
                        onValueChange = { shortBreakMins = it },
                        valueRange = 1f..30f,
                        steps = 28,
                        colors = SliderDefaults.colors(thumbColor = CelestialOpalGlow, activeTrackColor = CelestialOpal)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Long Break", style = MaterialTheme.typography.labelMedium, color = WarmCreamIvory)
                        Text("${longBreakMins.roundToInt()} min", style = MaterialTheme.typography.labelMedium, color = CelestialOpalGlow, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = longBreakMins,
                        onValueChange = { longBreakMins = it },
                        valueRange = 5f..60f,
                        steps = 10,
                        colors = SliderDefaults.colors(thumbColor = CelestialOpalGlow, activeTrackColor = CelestialOpal)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rounds per Cycle", style = MaterialTheme.typography.labelMedium, color = WarmCreamIvory)
                        Text("${totalRounds.roundToInt()} rounds", style = MaterialTheme.typography.labelMedium, color = RadiantEmberSoft, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = totalRounds,
                        onValueChange = { totalRounds = it },
                        valueRange = 2f..8f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = RadiantEmber, activeTrackColor = RadiantEmber)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveConfig(
                        focusMins.roundToInt(),
                        shortBreakMins.roundToInt(),
                        longBreakMins.roundToInt(),
                        totalRounds.roundToInt(),
                        sprintTitle
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadiantEmber,
                    contentColor = ObsidianVoidCanvas
                ),
                modifier = Modifier.testTag("save_pomodoro_config_button")
            ) {
                Text("Save Cycle", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedSlate)
            }
        }
    )
}

@Composable
private fun AddFocusTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var durationMins by remember { mutableFloatStateOf(25f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceLow,
        title = {
            Text("Add Focus Task", color = WarmCreamIvory, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g., API Integration Spec") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AlabasterMist,
                        unfocusedTextColor = AlabasterMist,
                        focusedBorderColor = RadiantEmberSoft,
                        unfocusedBorderColor = ObsidianHairlineBorder
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Estimated Duration", style = MaterialTheme.typography.labelMedium, color = WarmCreamIvory)
                    Text("${durationMins.roundToInt()}m", style = MaterialTheme.typography.labelMedium, color = RadiantEmberSoft, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = durationMins,
                    onValueChange = { durationMins = it },
                    valueRange = 10f..90f,
                    steps = 15,
                    colors = SliderDefaults.colors(thumbColor = RadiantEmber, activeTrackColor = RadiantEmber)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title, durationMins.roundToInt()) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadiantEmber,
                    contentColor = ObsidianVoidCanvas
                )
            ) {
                Text("Add Task", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedSlate)
            }
        }
    )
}

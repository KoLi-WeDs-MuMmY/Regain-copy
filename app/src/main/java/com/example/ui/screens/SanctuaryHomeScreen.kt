package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DailyQuestEntity
import com.example.ui.AdaptiveLayoutInfo
import com.example.ui.RegainTab
import com.example.ui.RegainUiState
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
import kotlin.math.roundToInt

@Composable
fun SanctuaryHomeScreen(
    uiState: RegainUiState,
    layoutInfo: AdaptiveLayoutInfo,
    onNavigateTab: (RegainTab) -> Unit,
    onStartQuickFocus: () -> Unit,
    onPreviewReelsIntercept: () -> Unit,
    onOpenPermissionsSetup: () -> Unit,
    onClaimQuest: (DailyQuestEntity) -> Unit,
    onUpdateDailyGoal: (Int) -> Unit
) {
    var showEditGoalDialog by remember { mutableStateOf(false) }

    if (showEditGoalDialog) {
        EditMindfulGoalDialog(
            currentGoalMinutes = uiState.userProgress.mindfulGoalMinutes,
            onDismiss = { showEditGoalDialog = false },
            onSave = { newGoal ->
                onUpdateDailyGoal(newGoal)
                showEditGoalDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sanctuary_home_list"),
        contentPadding = PaddingValues(
            horizontal = layoutInfo.horizontalPadding,
            vertical = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Twilight Sanctuary Hero Ring Gauge
        item {
            TwilightSanctuaryHeroSection(
                uiState = uiState,
                onEditGoalClick = { showEditGoalDialog = true }
            )
        }

        // 2. Daily Quests Section

        // 4. Ritual / Daily Quests Section
        item {
            RitualDailyQuestsSection(
                quests = uiState.dailyQuests,
                isTwoColumn = layoutInfo.gridColumns >= 2,
                onClaimQuest = onClaimQuest
            )
        }

        // 5. Full-Width Radiant Ember Pill CTA ("Begin Meditative Flow")
        item {
            BeginMeditativeFlowCta(
                isTimerRunning = uiState.timerState.isRunning,
                exactElapsedFormatted = uiState.timerState.exactElapsedFormatted,
                onStartQuickFocus = onStartQuickFocus
            )
        }
    }
}

@Composable
private fun TwilightSanctuaryHeroSection(
    uiState: RegainUiState,
    onEditGoalClick: () -> Unit
) {
    val reclaimedMins = (uiState.userProgress.savedMinutesToday + uiState.totalFocusedMinutesToday)
        .coerceAtLeast(14)
    val goalMin = uiState.userProgress.mindfulGoalMinutes.coerceAtLeast(30)
    val ratio = (reclaimedMins.toFloat() / goalMin.toFloat()).coerceIn(0.18f, 1f)

    val recHours = reclaimedMins / 60
    val recRemMins = reclaimedMins % 60
    val reclaimedFormatted = if (recHours > 0) "${recHours}h ${recRemMins}m" else "${recRemMins}m"

    val goalHours = goalMin / 60
    val goalRemMins = goalMin % 60
    val goalFormatted = "${goalHours}h ${goalRemMins}m"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Stack: "STATE OF MIND" / "Twilight Sanctuary"
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "STATE OF MIND",
                style = MaterialTheme.typography.labelSmall,
                color = RadiantEmberSoft.copy(alpha = 0.78f),
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Twilight Sanctuary",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AlabasterMist,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = onEditGoalClick,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("edit_daily_goal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit daily screen time budget",
                        tint = MutedSlate,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Circular Radiant Ember Reclaimed Gauge
        Box(
            modifier = Modifier
                .size(236.dp)
                .clickable { onEditGoalClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        onDrawBehind {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxRadius = size.minDimension / 2f

                            // Ambient Radiant Ember radial bloom behind the ring
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        RadiantEmber.copy(alpha = 0.18f),
                                        RadiantEmberContainer.copy(alpha = 0.10f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = maxRadius
                                ),
                                radius = maxRadius,
                                center = center
                            )

                            val strokePx = 6.dp.toPx()
                            val ringDiameter = size.minDimension * 0.80f
                            val topLeft = Offset(
                                (size.width - ringDiameter) / 2f,
                                (size.height - ringDiameter) / 2f
                            )

                            // Subtle Dark Track Ring
                            drawArc(
                                color = ObsidianSurfaceHigh.copy(alpha = 0.85f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(ringDiameter, ringDiameter),
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val strokePx = 6.dp.toPx()
                val ringDiameter = size.minDimension * 0.80f
                val topLeft = Offset(
                    (size.width - ringDiameter) / 2f,
                    (size.height - ringDiameter) / 2f
                )

                // Soft Outer Glow Arc
                drawArc(
                    color = RadiantEmber.copy(alpha = 0.28f),
                    startAngle = -90f,
                    sweepAngle = 360f * ratio,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(ringDiameter, ringDiameter),
                    style = Stroke(width = strokePx * 2.5f, cap = StrokeCap.Round)
                )

                // Crisp Radiant Ember Progress Arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            RadiantEmber,
                            RadiantEmberSoft,
                            RadiantEmber
                        ),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * ratio,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(ringDiameter, ringDiameter),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "RECLAIMED",
                    style = MaterialTheme.typography.labelSmall,
                    color = RadiantEmberSoft,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = reclaimedFormatted,
                    style = MaterialTheme.typography.displayMedium,
                    color = AlabasterMist,
                    fontWeight = FontWeight.Normal
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "of $goalFormatted daily cap",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                }
            }
        }

        // Celestial Opal "Cognitive load balanced" Status Pill
        Surface(
            shape = RoundedCornerShape(50),
            color = ObsidianSurfaceLow,
            modifier = Modifier.border(
                width = 1.dp,
                color = CelestialOpal.copy(alpha = 0.28f),
                shape = RoundedCornerShape(50)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(CelestialOpalGlow)
                )
                Text(
                    text = "Cognitive load balanced",
                    style = MaterialTheme.typography.labelMedium,
                    color = CelestialOpalGlow,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RitualDailyQuestsSection(
    quests: List<DailyQuestEntity>,
    isTwoColumn: Boolean,
    onClaimQuest: (DailyQuestEntity) -> Unit
) {
    val completedCount = quests.count { it.isCompleted }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "RITUAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
                Text(
                    text = "Daily Quests",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlabasterMist,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "$completedCount OF ${quests.size} DONE",
                style = MaterialTheme.typography.labelSmall,
                color = CelestialOpalGlow,
                fontWeight = FontWeight.Bold
            )
        }

        if (isTwoColumn) {
            val chunked = quests.chunked(2)
            chunked.forEachIndexed { chunkIdx, rowQuests ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowQuests.forEachIndexed { idx, quest ->
                        Box(modifier = Modifier.weight(1f)) {
                            ObsidianQuestCard(
                                quest = quest,
                                isPrimaryAlignment = (chunkIdx == 0 && idx == 0),
                                onClaimQuest = onClaimQuest
                            )
                        }
                    }
                    if (rowQuests.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            quests.forEachIndexed { idx, quest ->
                ObsidianQuestCard(
                    quest = quest,
                    isPrimaryAlignment = idx == 0,
                    onClaimQuest = onClaimQuest
                )
            }
        }
    }
}

@Composable
private fun ObsidianQuestCard(
    quest: DailyQuestEntity,
    isPrimaryAlignment: Boolean,
    onClaimQuest: (DailyQuestEntity) -> Unit
) {
    val questIcon = when {
        quest.isCompleted -> Icons.Default.Check
        quest.id.contains("breath") -> Icons.Default.SelfImprovement
        quest.id.contains("shield") -> Icons.Default.DarkMode
        else -> Icons.Default.Timer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (quest.isCompleted) CelestialOpal.copy(alpha = 0.25f) else ObsidianHairlineBorder,
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimaryAlignment) ObsidianSurfaceCard else ObsidianSurfaceLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (quest.isCompleted) CelestialOpalContainer else ObsidianInsetTrough
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = questIcon,
                    contentDescription = null,
                    tint = if (quest.isCompleted) CelestialOpalGlow else MutedSlate,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (isPrimaryAlignment) {
                    Text(
                        text = "PRIMARY ALIGNMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = CelestialOpalGlow,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AlabasterMist,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = quest.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }

            if (quest.isClaimed) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianInsetTrough
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = RadiantEmberSoft,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "+${quest.rewardCoins} XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = AlabasterMist,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (quest.isCompleted) {
                Button(
                    onClick = { onClaimQuest(quest) },
                    modifier = Modifier.testTag("claim_quest_${quest.id}"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadiantEmber,
                        contentColor = ObsidianVoidCanvas
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+${quest.rewardCoins} XP",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Pending",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSlate
                )
            }
        }
    }
}

@Composable
private fun BeginMeditativeFlowCta(
    isTimerRunning: Boolean,
    exactElapsedFormatted: String,
    onStartQuickFocus: () -> Unit
) {
    Button(
        onClick = onStartQuickFocus,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(50),
                ambientColor = RadiantEmber,
                spotColor = RadiantEmber
            )
            .testTag("hero_start_focus_button"),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = RadiantEmber,
            contentColor = ObsidianVoidCanvas
        )
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isTimerRunning) {
                "Resume Meditative Flow ($exactElapsedFormatted)"
            } else {
                "Begin Meditative Flow"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EditMindfulGoalDialog(
    currentGoalMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var sliderVal by remember { mutableFloatStateOf(currentGoalMinutes.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceLow,
        title = {
            Text(
                text = "Daily Mindful Screen Budget",
                style = MaterialTheme.typography.headlineSmall,
                color = AlabasterMist
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val mins = sliderVal.roundToInt()
                Text(
                    text = "Target Cap: ${mins / 60}h ${mins % 60}m ($mins minutes/day)",
                    style = MaterialTheme.typography.titleMedium,
                    color = RadiantEmberSoft
                )
                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    valueRange = 45f..300f,
                    steps = 16,
                    modifier = Modifier.testTag("daily_goal_slider")
                )
                Text(
                    text = "Rega calibrates your Twilight Sanctuary ring and Aegis shield intensity around this daily threshold.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(sliderVal.roundToInt()) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadiantEmber,
                    contentColor = ObsidianVoidCanvas
                ),
                modifier = Modifier.testTag("save_daily_goal_button")
            ) {
                Text("Save Cap", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedSlate)
            }
        }
    )
}

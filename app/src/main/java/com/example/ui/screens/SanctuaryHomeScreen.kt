package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DailyQuestEntity
import com.example.ui.AdaptiveLayoutInfo
import com.example.ui.RegainTab
import com.example.ui.RegainUiState
import kotlin.math.roundToInt

@Composable
fun SanctuaryHomeScreen(
    uiState: RegainUiState,
    layoutInfo: AdaptiveLayoutInfo,
    onNavigateTab: (RegainTab) -> Unit,
    onStartQuickFocus: () -> Unit,
    onPreviewReelsIntercept: () -> Unit,
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
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ZenSanctuaryHeroBanner(
                uiState = uiState,
                isWide = layoutInfo.isTwoPaneCapable,
                onStartQuickFocus = onStartQuickFocus,
                onPreviewReelsIntercept = onPreviewReelsIntercept
            )
        }

        if (layoutInfo.isTwoPaneCapable) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ScreenTimeBudgetCard(
                            uiState = uiState,
                            onEditGoalClick = { showEditGoalDialog = true },
                            onManageShieldsClick = { onNavigateTab(RegainTab.SHIELD) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        RegaCompanionAndActionsColumn(
                            uiState = uiState,
                            onNavigateTab = onNavigateTab,
                            onPreviewReelsIntercept = onPreviewReelsIntercept
                        )
                    }
                }
            }
        } else {
            item {
                ScreenTimeBudgetCard(
                    uiState = uiState,
                    onEditGoalClick = { showEditGoalDialog = true },
                    onManageShieldsClick = { onNavigateTab(RegainTab.SHIELD) }
                )
            }
            item {
                RegaCompanionAndActionsColumn(
                    uiState = uiState,
                    onNavigateTab = onNavigateTab,
                    onPreviewReelsIntercept = onPreviewReelsIntercept
                )
            }
        }

        item {
            DailyDetoxQuestsSection(
                quests = uiState.dailyQuests,
                isTwoColumn = layoutInfo.gridColumns >= 2,
                onClaimQuest = onClaimQuest
            )
        }
    }
}

@Composable
private fun ZenSanctuaryHeroBanner(
    uiState: RegainUiState,
    isWide: Boolean,
    onStartQuickFocus: () -> Unit,
    onPreviewReelsIntercept: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.ic_zen_sanctuary),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(28.dp)),
                alpha = 0.35f
            )

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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_rega_mascot),
                            contentDescription = "Rega Zen Spirit Mascot",
                            modifier = Modifier
                                .size(if (isWide) 68.dp else 58.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Column {
                            Text(
                                text = "Regain Zen Sanctuary",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Lv.${uiState.userProgress.regaLevel} ${uiState.userProgress.regaStageName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPillBadge(
                            icon = Icons.Default.LocalFireDepartment,
                            text = "${uiState.userProgress.streakDays}d Streak",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        StatusPillBadge(
                            icon = Icons.Default.Spa,
                            text = "${uiState.userProgress.leafCoins} Coins",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartQuickFocus,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_start_focus_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.timerState.isRunning) "Focus Active" else "Start Focus Timer",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    FilledTonalButton(
                        onClick = onPreviewReelsIntercept,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_test_intercept_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Test Reels Shield",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPillBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ScreenTimeBudgetCard(
    uiState: RegainUiState,
    onEditGoalClick: () -> Unit,
    onManageShieldsClick: () -> Unit
) {
    val usedMin = uiState.totalScreenTimeMinutesToday
    val goalMin = uiState.userProgress.mindfulGoalMinutes.coerceAtLeast(30)
    val ratio = (usedMin.toFloat() / goalMin.toFloat()).coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(700),
        label = "screen_time_ring"
    )

    val hours = usedMin / 60
    val mins = usedMin % 60
    val goalHours = goalMin / 60
    val goalRemMins = goalMin % 60

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val warningColor = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Mindful Screen Budget",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Reclaimed ${uiState.userProgress.savedMinutesToday}m from doomscrolling today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = onEditGoalClick,
                    modifier = Modifier.testTag("edit_daily_goal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit daily screen time budget",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier.size(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 13.dp.toPx()
                        val diameter = size.minDimension - strokePx
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

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
                            color = if (ratio > 0.9f) warningColor else primaryColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedRatio,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${hours}h ${mins}m",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "of ${goalHours}h ${goalRemMins}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatRow(
                        label = "Reels/Shorts Saved",
                        value = "${uiState.totalShortFormInterceptsToday} blocks",
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    MetricStatRow(
                        label = "Deep Focus Done",
                        value = "${uiState.totalFocusedMinutesToday} mins",
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                    MetricStatRow(
                        label = "Phone Pickups",
                        value = "${uiState.totalPickupsToday} times",
                        accentColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            OutlinedButton(
                onClick = onManageShieldsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("home_manage_shields_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Customize App Limits & Reels Shield")
            }
        }
    }
}

@Composable
private fun MetricStatRow(
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RegaCompanionAndActionsColumn(
    uiState: RegainUiState,
    onNavigateTab: (RegainTab) -> Unit,
    onPreviewReelsIntercept: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTab(RegainTab.AI_COACH) }
                .testTag("home_ai_coach_shortcut_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AI Productivity Coach",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "${uiState.dailyAiRequestsRemaining}/1 Free Req Left",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.todayAiInsight?.coachHeadline
                            ?: "Tap to generate today's single-prompt personalized screen time insight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickFeatureCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timer,
                title = "Pomodoro & Rain",
                subtitle = "25/5 Timer + Offline Synth Audio",
                testTag = "quick_card_focus_studio",
                onClick = { onNavigateTab(RegainTab.FOCUS) }
            )
            QuickFeatureCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Groups,
                title = "Study Rooms",
                subtitle = "Focus with 48+ Zen peers",
                testTag = "quick_card_study_rooms",
                onClick = { onNavigateTab(RegainTab.ROOMS) }
            )
        }
    }
}

@Composable
private fun QuickFeatureCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyDetoxQuestsSection(
    quests: List<DailyQuestEntity>,
    isTwoColumn: Boolean,
    onClaimQuest: (DailyQuestEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daily Mindful Quests",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${quests.count { it.isCompleted }}/${quests.size} Completed",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (isTwoColumn) {
            val chunked = quests.chunked(2)
            chunked.forEach { rowQuests ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowQuests.forEach { quest ->
                        Box(modifier = Modifier.weight(1f)) {
                            QuestItemCard(quest = quest, onClaimQuest = onClaimQuest)
                        }
                    }
                    if (rowQuests.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            quests.forEach { quest ->
                QuestItemCard(quest = quest, onClaimQuest = onClaimQuest)
            }
        }
    }
}

@Composable
private fun QuestItemCard(
    quest: DailyQuestEntity,
    onClaimQuest: (DailyQuestEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = quest.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (quest.isClaimed) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Claimed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                } else if (quest.isCompleted) {
                    Button(
                        onClick = { onClaimQuest(quest) },
                        modifier = Modifier.testTag("claim_quest_${quest.id}"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Claim +${quest.rewardCoins}")
                    }
                } else {
                    Text(
                        text = "+${quest.rewardCoins} Coins",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val progressFraction = (quest.currentProgress.toFloat() / quest.targetProgress.coerceAtLeast(1)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
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
        title = {
            Text(
                text = "Daily Mindful Screen Time Budget",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val mins = sliderVal.roundToInt()
                Text(
                    text = "Target Limit: ${mins / 60}h ${mins % 60}m ($mins minutes/day)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    valueRange = 45f..300f,
                    steps = 16,
                    modifier = Modifier.testTag("daily_goal_slider")
                )
                Text(
                    text = "Rega will gently alert you and strengthen Reels/Shorts shields as you approach your daily budget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(sliderVal.roundToInt()) },
                modifier = Modifier.testTag("save_daily_goal_button")
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

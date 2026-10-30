package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppLimitEntity
import com.example.data.ScheduleBlockEntity
import com.example.data.ShortFormShieldEntity
import com.example.ui.AdaptiveLayoutInfo
import kotlin.math.roundToInt

@Composable
fun ShieldLimitsScreen(
    shortFormShields: List<ShortFormShieldEntity>,
    appLimits: List<AppLimitEntity>,
    schedules: List<ScheduleBlockEntity>,
    layoutInfo: AdaptiveLayoutInfo,
    onBackToHome: () -> Unit,
    onCycleShieldMode: (ShortFormShieldEntity) -> Unit,
    onToggleShieldEnabled: (ShortFormShieldEntity) -> Unit,
    onToggleShieldKeepDms: (ShortFormShieldEntity) -> Unit,
    onTestShieldIntercept: (ShortFormShieldEntity) -> Unit,
    onToggleAppLimit: (AppLimitEntity) -> Unit,
    onUpdateAppLimitMinutes: (AppLimitEntity, Int) -> Unit,
    onToggleAppStrict: (AppLimitEntity) -> Unit,
    onTestAppIntercept: (AppLimitEntity) -> Unit,
    onToggleSchedule: (ScheduleBlockEntity) -> Unit,
    onAddSchedule: (String, String, String, String, Boolean) -> Unit,
    onDeleteSchedule: (Long) -> Unit
) {
    BackHandler(onBack = onBackToHome)

    var showAddScheduleDialog by remember { mutableStateOf(false) }

    if (showAddScheduleDialog) {
        AddScheduleBlockDialog(
            onDismiss = { showAddScheduleDialog = false },
            onConfirm = { title, window, days, target, strict ->
                onAddSchedule(title, window, days, target, strict)
                showAddScheduleDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shield_limits_list"),
        contentPadding = PaddingValues(
            horizontal = layoutInfo.horizontalPadding,
            vertical = 16.dp
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
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ReelsAndShortsShieldSection(
                            shields = shortFormShields,
                            onCycleShieldMode = onCycleShieldMode,
                            onToggleShieldEnabled = onToggleShieldEnabled,
                            onToggleShieldKeepDms = onToggleShieldKeepDms,
                            onTestShieldIntercept = onTestShieldIntercept
                        )
                        SmartScheduleBlocksSection(
                            schedules = schedules,
                            onAddScheduleClick = { showAddScheduleDialog = true },
                            onToggleSchedule = onToggleSchedule,
                            onDeleteSchedule = onDeleteSchedule
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppLimitsAndStrictSection(
                            appLimits = appLimits,
                            onToggleAppLimit = onToggleAppLimit,
                            onUpdateAppLimitMinutes = onUpdateAppLimitMinutes,
                            onToggleAppStrict = onToggleAppStrict,
                            onTestAppIntercept = onTestAppIntercept
                        )
                    }
                }
            }
        } else {
            item {
                ReelsAndShortsShieldSection(
                    shields = shortFormShields,
                    onCycleShieldMode = onCycleShieldMode,
                    onToggleShieldEnabled = onToggleShieldEnabled,
                    onToggleShieldKeepDms = onToggleShieldKeepDms,
                    onTestShieldIntercept = onTestShieldIntercept
                )
            }
            item {
                AppLimitsAndStrictSection(
                    appLimits = appLimits,
                    onToggleAppLimit = onToggleAppLimit,
                    onUpdateAppLimitMinutes = onUpdateAppLimitMinutes,
                    onToggleAppStrict = onToggleAppStrict,
                    onTestAppIntercept = onTestAppIntercept
                )
            }
            item {
                SmartScheduleBlocksSection(
                    schedules = schedules,
                    onAddScheduleClick = { showAddScheduleDialog = true },
                    onToggleSchedule = onToggleSchedule,
                    onDeleteSchedule = onDeleteSchedule
                )
            }
        }
    }
}

@Composable
private fun ReelsAndShortsShieldSection(
    shields: List<ShortFormShieldEntity>,
    onCycleShieldMode: (ShortFormShieldEntity) -> Unit,
    onToggleShieldEnabled: (ShortFormShieldEntity) -> Unit,
    onToggleShieldKeepDms: (ShortFormShieldEntity) -> Unit,
    onTestShieldIntercept: (ShortFormShieldEntity) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SlowMotionVideo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Targeted Reels & Shorts Shield",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cut off infinite vertical feeds while keeping Direct Messages & Search open",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            shields.forEach { shield ->
                val modeBadgeText = when (shield.shieldMode) {
                    "BLOCK_COMPLETELY" -> "Block Feed Completely"
                    "MINDFUL_BREATH_PAUSE" -> "4-4-4 Breath Pause"
                    else -> "15m Daily Cap"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${shield.platformName} · ${shield.feedName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${shield.interceptsToday} scroll loops intercepted today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Switch(
                            checked = shield.isEnabled,
                            onCheckedChange = { onToggleShieldEnabled(shield) },
                            modifier = Modifier.testTag("shield_toggle_${shield.id}")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { onCycleShieldMode(shield) },
                            label = { Text(modeBadgeText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("shield_mode_chip_${shield.id}")
                        )

                        AssistChip(
                            onClick = { onToggleShieldKeepDms(shield) },
                            label = {
                                Text(if (shield.allowDirectMessages) "DMs Allowed" else "Full App Block")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = { onTestShieldIntercept(shield) },
                            modifier = Modifier.testTag("test_shield_intercept_${shield.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Preview Mindful Intercept",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLimitsAndStrictSection(
    appLimits: List<AppLimitEntity>,
    onToggleAppLimit: (AppLimitEntity) -> Unit,
    onUpdateAppLimitMinutes: (AppLimitEntity, Int) -> Unit,
    onToggleAppStrict: (AppLimitEntity) -> Unit,
    onTestAppIntercept: (AppLimitEntity) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "App Daily Limits & Strict Mode",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Apps locked with Strict Mode require a guided 4-4-4 breathing pause and reflection phrase before limits can be bypassed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            appLimits.forEach { app ->
                val ratio = (app.usedTodayMinutes.toFloat() / app.dailyLimitMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = app.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Used ${app.usedTodayMinutes}m of ${app.dailyLimitMinutes}m cap · ${app.pickupsToday} pickups",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = app.isEnabled,
                            onCheckedChange = { onToggleAppLimit(app) },
                            modifier = Modifier.testTag("app_limit_switch_${app.iconKey}")
                        )
                    }

                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (ratio >= 0.9f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Slider(
                        value = app.dailyLimitMinutes.toFloat(),
                        onValueChange = { onUpdateAppLimitMinutes(app, it.roundToInt()) },
                        valueRange = 10f..120f,
                        steps = 21,
                        modifier = Modifier.testTag("app_limit_slider_${app.iconKey}")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onToggleAppStrict(app) },
                            modifier = Modifier.testTag("app_strict_button_${app.iconKey}"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (app.isStrictLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (app.isStrictLocked) "Strict Locked" else "Standard Lock",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        FilledTonalButton(
                            onClick = { onTestAppIntercept(app) },
                            modifier = Modifier.testTag("app_test_intercept_${app.iconKey}"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Preview Intercept",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartScheduleBlocksSection(
    schedules: List<ScheduleBlockEntity>,
    onAddScheduleClick: () -> Unit,
    onToggleSchedule: (ScheduleBlockEntity) -> Unit,
    onDeleteSchedule: (Long) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Recurring Block Schedules",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Automated deep work, study, and bedtime routines",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddScheduleClick,
                    modifier = Modifier.testTag("add_schedule_button"),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New")
                }
            }

            schedules.forEach { schedule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = schedule.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${schedule.timeWindowLabel} · ${schedule.daysSummary}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = schedule.targetSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = schedule.isEnabled,
                            onCheckedChange = { onToggleSchedule(schedule) }
                        )
                        IconButton(onClick = { onDeleteSchedule(schedule.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete schedule",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddScheduleBlockDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var timeWindow by remember { mutableStateOf("14:00 – 17:30") }
    var days by remember { mutableStateOf("Mon – Fri") }
    var target by remember { mutableStateOf("Instagram Reels, YouTube Shorts & TikTok") }
    var isStrict by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Smart Block Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Routine Title") },
                    placeholder = { Text("Afternoon Exam Focus") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_title_input")
                )
                OutlinedTextField(
                    value = timeWindow,
                    onValueChange = { timeWindow = it },
                    label = { Text("Time Window") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it },
                    label = { Text("Active Days") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Blocked Feeds / Apps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isStrict,
                        onCheckedChange = { isStrict = it }
                    )
                    Text(
                        text = "Lock with Strict Mode during active hours",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.ifBlank { "Afternoon Focus Block" },
                        timeWindow,
                        days,
                        target,
                        isStrict
                    )
                },
                modifier = Modifier.testTag("confirm_add_schedule_button")
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

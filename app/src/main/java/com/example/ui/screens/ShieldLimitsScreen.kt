package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.blocker.BlockerPermissionStatus
import com.example.data.AppLimitEntity
import com.example.data.ScheduleBlockEntity
import com.example.data.SelectableDeviceApp
import com.example.data.ShortFormShieldEntity
import com.example.ui.AdaptiveLayoutInfo
import kotlin.math.roundToInt

@Composable
fun ShieldLimitsScreen(
    permissionStatus: BlockerPermissionStatus,
    discoverableApps: List<SelectableDeviceApp>,
    shortFormShields: List<ShortFormShieldEntity>,
    appLimits: List<AppLimitEntity>,
    schedules: List<ScheduleBlockEntity>,
    layoutInfo: AdaptiveLayoutInfo,
    onBackToHome: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onAddOrBlockApp: (SelectableDeviceApp, Boolean, Int, Boolean) -> Unit,
    onToggleBlockAppCompletely: (AppLimitEntity) -> Unit,
    onRemoveAppFromBlocker: (AppLimitEntity) -> Unit,
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
    var showChooseAppDialog by remember { mutableStateOf(false) }

    if (showAddScheduleDialog) {
        AddScheduleBlockDialog(
            onDismiss = { showAddScheduleDialog = false },
            onConfirm = { title, window, days, target, strict ->
                onAddSchedule(title, window, days, target, strict)
                showAddScheduleDialog = false
            }
        )
    }

    if (showChooseAppDialog) {
        ChooseAppToBlockDialog(
            discoverableApps = discoverableApps,
            existingPackageNames = appLimits.map { it.packageName }.toSet(),
            onDismiss = { showChooseAppDialog = false },
            onConfirmApp = { app, blockNow, dailyLimit, isStrict ->
                onAddOrBlockApp(app, blockNow, dailyLimit, isStrict)
                showChooseAppDialog = false
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
        item {
            PermissionsAndOverlayControlCard(
                permissionStatus = permissionStatus,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestUsagePermission = onRequestUsagePermission,
                onRequestAccessibilityPermission = onRequestAccessibilityPermission
            )
        }

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
                        AppLimitsAndStrictSection(
                            appLimits = appLimits,
                            onChooseAppToBlockClick = { showChooseAppDialog = true },
                            onToggleBlockAppCompletely = onToggleBlockAppCompletely,
                            onRemoveAppFromBlocker = onRemoveAppFromBlocker,
                            onToggleAppLimit = onToggleAppLimit,
                            onUpdateAppLimitMinutes = onUpdateAppLimitMinutes,
                            onToggleAppStrict = onToggleAppStrict,
                            onTestAppIntercept = onTestAppIntercept
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.95f),
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
                }
            }
        } else {
            item {
                AppLimitsAndStrictSection(
                    appLimits = appLimits,
                    onChooseAppToBlockClick = { showChooseAppDialog = true },
                    onToggleBlockAppCompletely = onToggleBlockAppCompletely,
                    onRemoveAppFromBlocker = onRemoveAppFromBlocker,
                    onToggleAppLimit = onToggleAppLimit,
                    onUpdateAppLimitMinutes = onUpdateAppLimitMinutes,
                    onToggleAppStrict = onToggleAppStrict,
                    onTestAppIntercept = onTestAppIntercept
                )
            }
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
fun PermissionsAndOverlayControlCard(
    permissionStatus: BlockerPermissionStatus,
    onRequestNotificationPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permissions_control_card"),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(
                            text = "App Blocker & Pop-Up Permissions",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Grant permissions so Regain Zen can block apps and display pop-ups & animations over Reels/Shorts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (permissionStatus.allGranted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    }
                ) {
                    Text(
                        text = "${permissionStatus.grantedCount}/4 Active",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (permissionStatus.allGranted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            PermissionControlRow(
                icon = Icons.Default.NotificationsActive,
                title = "1. Pop-Up Notifications & Alerts",
                subtitle = "Shows heads-up pop-up alerts when blocked apps or study timers finish",
                isGranted = permissionStatus.hasNotificationsPermission,
                buttonTag = "perm_btn_notifications",
                onGrantClick = onRequestNotificationPermission
            )

            PermissionControlRow(
                icon = Icons.Default.Layers,
                title = "2. Display Pop-Ups Over Other Apps",
                subtitle = "Displays animated breathing pop-ups on your screen to pull you out of blocked apps",
                isGranted = permissionStatus.hasOverlayPopUpPermission,
                buttonTag = "perm_btn_overlay",
                onGrantClick = onRequestOverlayPermission
            )

            PermissionControlRow(
                icon = Icons.Default.Security,
                title = "3. Accessibility App & Shorts Blocker",
                subtitle = "Detects when you open a blocked app, Reel, or Short and intercepts it immediately",
                isGranted = permissionStatus.hasAccessibilityBlockerEnabled,
                buttonTag = "perm_btn_accessibility",
                onGrantClick = onRequestAccessibilityPermission
            )

            PermissionControlRow(
                icon = Icons.Default.QueryStats,
                title = "4. Screen Time & Usage Access",
                subtitle = "Calculates accurate daily screen time and app pickups over time",
                isGranted = permissionStatus.hasUsageStatsPermission,
                buttonTag = "perm_btn_usage",
                onGrantClick = onRequestUsagePermission
            )
        }
    }
}

@Composable
private fun PermissionControlRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    buttonTag: String,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isGranted) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Allowed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Button(
                onClick = onGrantClick,
                modifier = Modifier.testTag(buttonTag),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Allow", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun AppLimitsAndStrictSection(
    appLimits: List<AppLimitEntity>,
    onChooseAppToBlockClick: () -> Unit,
    onToggleBlockAppCompletely: (AppLimitEntity) -> Unit,
    onRemoveAppFromBlocker: (AppLimitEntity) -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Blocker & Daily Limits",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose any app to block completely or enforce a daily time cap with pop-up animations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onChooseAppToBlockClick,
                    modifier = Modifier.testTag("choose_app_to_block_button"),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Block App")
                }
            }

            appLimits.forEach { app ->
                val ratio = (app.usedTodayMinutes.toFloat() / app.dailyLimitMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (app.isBlockedCompletely && app.isEnabled) {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            }
                        )
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
                                if (app.isBlockedCompletely && app.isEnabled) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.tertiary
                                    ) {
                                        Text(
                                            text = "BLOCKED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
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
                            }
                            Text(
                                text = if (app.isBlockedCompletely && app.isEnabled) {
                                    "Blocked completely · ${app.blockedCountToday} intercepts today"
                                } else {
                                    "Used ${app.usedTodayMinutes}m of ${app.dailyLimitMinutes}m cap · ${app.pickupsToday} pickups"
                                },
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
                        progress = { if (app.isBlockedCompletely && app.isEnabled) 1f else ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (app.isBlockedCompletely || ratio >= 0.9f) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onToggleBlockAppCompletely(app) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("app_block_now_button_${app.iconKey}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (app.isBlockedCompletely) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (app.isBlockedCompletely) "Unblock App" else "Block App Now",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        OutlinedButton(
                            onClick = { onToggleAppStrict(app) },
                            modifier = Modifier.testTag("app_strict_button_${app.iconKey}"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (app.isStrictLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (app.isStrictLocked) "Strict" else "Normal",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        FilledTonalButton(
                            onClick = { onTestAppIntercept(app) },
                            modifier = Modifier.testTag("app_test_intercept_${app.iconKey}"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Preview Pop-Up",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pop-Up",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        IconButton(
                            onClick = { onRemoveAppFromBlocker(app) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("app_delete_button_${app.iconKey}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove ${app.appName} from blocker",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChooseAppToBlockDialog(
    discoverableApps: List<SelectableDeviceApp>,
    existingPackageNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirmApp: (SelectableDeviceApp, Boolean, Int, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<SelectableDeviceApp?>(null) }
    var blockImmediately by remember { mutableStateOf(true) }
    var dailyLimitSlider by remember { mutableFloatStateOf(25f) }
    var strictLock by remember { mutableStateOf(true) }

    val filteredApps = remember(discoverableApps, searchQuery) {
        if (searchQuery.isBlank()) {
            discoverableApps
        } else {
            discoverableApps.filter {
                it.appName.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.category.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.packageName.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose App to Block",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (selectedApp == null && it.isNotBlank()) {
                            selectedApp = SelectableDeviceApp(
                                packageName = "",
                                appName = it.trim(),
                                category = "Custom Blocked App"
                            )
                        }
                    },
                    label = { Text("Search installed apps or type app name") },
                    placeholder = { Text("e.g., Instagram, YouTube, Roblox, Chrome...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("choose_app_search_input")
                )

                Text(
                    text = "Tap an app below to choose it:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = selectedApp?.packageName == app.packageName &&
                            selectedApp?.appName == app.appName
                        val alreadyInList = existingPackageNames.contains(app.packageName)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    selectedApp = app
                                    searchQuery = app.appName
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("selectable_app_${app.packageName}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${app.category} · ${app.packageName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (alreadyInList) {
                                Text(
                                    text = "Configured",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = blockImmediately,
                        onCheckedChange = { blockImmediately = it },
                        modifier = Modifier.testTag("block_immediately_checkbox")
                    )
                    Text(
                        text = "Block app completely right now (triggers pop-up animation on launch)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Daily Limit Cap: ${dailyLimitSlider.roundToInt()} mins/day",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = dailyLimitSlider,
                    onValueChange = { dailyLimitSlider = it },
                    valueRange = 5f..120f,
                    steps = 22
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = strictLock,
                        onCheckedChange = { strictLock = it }
                    )
                    Text(
                        text = "Enable Strict Mode (requires 4-4-4 breathing pause to unlock)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosen = selectedApp ?: SelectableDeviceApp(
                        packageName = "",
                        appName = searchQuery.ifBlank { "Custom Social App" },
                        category = "Blocked App"
                    )
                    onConfirmApp(chosen, blockImmediately, dailyLimitSlider.roundToInt(), strictLock)
                },
                modifier = Modifier.testTag("confirm_block_app_button")
            ) {
                Text(if (blockImmediately) "Block Chosen App" else "Save App Limit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

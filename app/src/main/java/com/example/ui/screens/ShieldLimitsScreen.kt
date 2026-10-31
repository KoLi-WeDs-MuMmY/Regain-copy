package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.blocker.BlockerPermissionStatus
import com.example.data.AppLimitEntity
import com.example.data.ScheduleBlockEntity
import com.example.data.SelectableDeviceApp
import com.example.data.ShortFormShieldEntity
import com.example.ui.AdaptiveLayoutInfo
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
    onRequestBatteryPermission: () -> Unit = {},
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

    val totalDeflections = (shortFormShields.sumOf { it.interceptsToday } +
        appLimits.sumOf { it.blockedCountToday }).coerceAtLeast(19)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shield_limits_list"),
        contentPadding = PaddingValues(
            horizontal = layoutInfo.horizontalPadding,
            vertical = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Aegis Grid Hero Radar Card
        item {
            AegisGridHeroRadarCard(totalDeflections = totalDeflections)
        }

        // 2. Impulse Radar Window Card
        item {
            ImpulseRadarWindowCard()
        }

        // 3. System Permissions & Background Blocker Control Card
        item {
            PermissionsAndOverlayControlCard(
                permissionStatus = permissionStatus,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestUsagePermission = onRequestUsagePermission,
                onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                onRequestBatteryPermission = onRequestBatteryPermission
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
                        MonitoredPortalsAppLimitsSection(
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
                        FrictionRitualZenBrakeCard(
                            onTestFrictionRitual = {
                                val firstApp = appLimits.firstOrNull()
                                if (firstApp != null) {
                                    onTestAppIntercept(firstApp)
                                } else {
                                    shortFormShields.firstOrNull()?.let(onTestShieldIntercept)
                                }
                            }
                        )
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
                MonitoredPortalsAppLimitsSection(
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
                FrictionRitualZenBrakeCard(
                    onTestFrictionRitual = {
                        val firstApp = appLimits.firstOrNull()
                        if (firstApp != null) {
                            onTestAppIntercept(firstApp)
                        } else {
                            shortFormShields.firstOrNull()?.let(onTestShieldIntercept)
                        }
                    }
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
private fun AegisGridHeroRadarCard(totalDeflections: Int) {
    var selectedPostureIndex by remember { mutableIntStateOf(0) }
    val postures = listOf("Zen Fortress", "Surgical", "Monk")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Status Pill: "AEGIS GRID ACTIVE • LATENCY 0ms"
            Surface(
                shape = RoundedCornerShape(50),
                color = ObsidianSurfaceCard,
                modifier = Modifier.border(1.dp, CelestialOpal.copy(alpha = 0.28f), RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(CelestialOpalGlow)
                    )
                    Text(
                        text = "AEGIS GRID ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CelestialOpalGlow,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• LATENCY 0ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                }
            }

            // Concentric Radar Ring with "ARMOR ON" Shield Core
            Box(
                modifier = Modifier.size(168.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.minDimension / 2f

                    // Ambient Opal Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CelestialOpal.copy(alpha = 0.22f),
                                RadiantEmber.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxR
                        ),
                        radius = maxR,
                        center = center
                    )

                    // Outer Radar Ring
                    drawCircle(
                        color = ObsidianSurfaceHigh,
                        radius = maxR * 0.92f,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    // Mid Radiant Ember Radar Arc
                    drawArc(
                        color = RadiantEmber.copy(alpha = 0.55f),
                        startAngle = -35f,
                        sweepAngle = 210f,
                        useCenter = false,
                        topLeft = Offset(center.x - maxR * 0.76f, center.y - maxR * 0.76f),
                        size = Size(maxR * 1.52f, maxR * 1.52f),
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Inner Celestial Opal Ring
                    drawCircle(
                        color = CelestialOpal.copy(alpha = 0.50f),
                        radius = maxR * 0.52f,
                        center = center,
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    // Orbiting Threat Deflection Dots
                    drawCircle(
                        color = RadiantEmber,
                        radius = 3.5.dp.toPx(),
                        center = Offset(center.x + maxR * 0.42f, center.y - maxR * 0.42f)
                    )
                    drawCircle(
                        color = CelestialOpalGlow,
                        radius = 3.dp.toPx(),
                        center = Offset(center.x - maxR * 0.54f, center.y + maxR * 0.28f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CelestialOpalGlow,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "ARMOR ON",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlabasterMist,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Deflection Count Readout
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$totalDeflections Intrusions\nDeflected",
                    style = MaterialTheme.typography.displaySmall,
                    color = AlabasterMist,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Neural loop defended against doomscrolling. Deep resonance uninterrupted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate,
                    textAlign = TextAlign.Center
                )
            }

            // 3-Mode Posture Selector ("Zen Fortress | Surgical | Monk")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianInsetTrough)
                    .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(20.dp))
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                postures.forEachIndexed { idx, label ->
                    val selected = selectedPostureIndex == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) ObsidianSurfaceCard else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (selected) CelestialOpal.copy(alpha = 0.35f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedPostureIndex = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) AlabasterMist else MutedSlate,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImpulseRadarWindowCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = RadiantEmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "IMPULSE RADAR WINDOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlabasterMist,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = RadiantEmberContainer
                ) {
                    Text(
                        text = "PEAK URGE ZONE",
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantEmberSoft,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Critical Vulnerability: 14:15 — 15:45",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadiantEmberSoft,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "88% shields engaged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }

            // 11-Segment Radar Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (i in 0 until 11) {
                    val isPeak = i in 4..6
                    val isPast = i < 4
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    isPeak -> RadiantEmber
                                    isPast -> CelestialOpal
                                    else -> ObsidianSurfaceHigh
                                }
                            )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("09:00", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                Text(
                    "14:30 (NOW)",
                    style = MaterialTheme.typography.labelSmall,
                    color = RadiantEmberSoft,
                    fontWeight = FontWeight.Bold
                )
                Text("21:00", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
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
    onRequestAccessibilityPermission: () -> Unit,
    onRequestBatteryPermission: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(24.dp))
            .testTag("permissions_control_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CelestialOpalContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CelestialOpalGlow
                        )
                    }
                    Column {
                        Text(
                            text = "Aegis System Permissions",
                            style = MaterialTheme.typography.titleMedium,
                            color = AlabasterMist,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Required for 0ms background detection & Black Liquid Pop-Up overlays",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (permissionStatus.allGranted) CelestialOpalContainer else RadiantEmberContainer
                ) {
                    Text(
                        text = "${permissionStatus.grantedCount}/${permissionStatus.totalCount} Active",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (permissionStatus.allGranted) CelestialOpalGlow else RadiantEmberSoft,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            PermissionControlRow(
                icon = Icons.Default.NotificationsActive,
                title = "1. Pop-Up Notifications & Alerts",
                subtitle = "Heads-up alerts when blocked apps or study timers finish",
                isGranted = permissionStatus.hasNotificationsPermission,
                buttonTag = "perm_btn_notifications",
                onGrantClick = onRequestNotificationPermission
            )

            PermissionControlRow(
                icon = Icons.Default.Layers,
                title = "2. Display Pop-Ups Over Other Apps",
                subtitle = "Renders the Black Liquid Circle Pop-Up over YouTube, Reels & Shorts",
                isGranted = permissionStatus.hasOverlayPopUpPermission,
                buttonTag = "perm_btn_overlay",
                onGrantClick = onRequestOverlayPermission
            )

            PermissionControlRow(
                icon = Icons.Default.Security,
                title = "3. Accessibility App & Shorts Blocker",
                subtitle = "Detects blocked app launches in 0ms and intercepts immediately",
                isGranted = permissionStatus.hasAccessibilityBlockerEnabled,
                buttonTag = "perm_btn_accessibility",
                onGrantClick = onRequestAccessibilityPermission
            )

            PermissionControlRow(
                icon = Icons.Default.QueryStats,
                title = "4. Screen Time & Usage Access",
                subtitle = "Monitors foreground app usage & daily allowance caps",
                isGranted = permissionStatus.hasUsageStatsPermission,
                buttonTag = "perm_btn_usage",
                onGrantClick = onRequestUsagePermission
            )

            PermissionControlRow(
                icon = Icons.Default.Schedule,
                title = "5. Unrestricted Background Execution",
                subtitle = "Prevents battery savers from pausing the background shield",
                isGranted = permissionStatus.hasUnrestrictedBatteryPermission,
                buttonTag = "perm_btn_battery",
                onGrantClick = onRequestBatteryPermission
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
            .background(ObsidianInsetTrough)
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(16.dp))
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
                tint = if (isGranted) CelestialOpalGlow else RadiantEmber,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AlabasterMist
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isGranted) {
            Surface(
                shape = RoundedCornerShape(50),
                color = CelestialOpalContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CelestialOpalGlow,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Allowed",
                        style = MaterialTheme.typography.labelSmall,
                        color = CelestialOpalGlow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Button(
                onClick = onGrantClick,
                modifier = Modifier.testTag(buttonTag),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadiantEmber,
                    contentColor = ObsidianVoidCanvas
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Allow", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MonitoredPortalsAppLimitsSection(
    appLimits: List<AppLimitEntity>,
    onChooseAppToBlockClick: () -> Unit,
    onToggleBlockAppCompletely: (AppLimitEntity) -> Unit,
    onRemoveAppFromBlocker: (AppLimitEntity) -> Unit,
    onToggleAppLimit: (AppLimitEntity) -> Unit,
    onUpdateAppLimitMinutes: (AppLimitEntity, Int) -> Unit,
    onToggleAppStrict: (AppLimitEntity) -> Unit,
    onTestAppIntercept: (AppLimitEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Section Header: "MONITORED PORTALS" + "• X INTERCEPTED"
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
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = CelestialOpalGlow,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "MONITORED PORTALS",
                    style = MaterialTheme.typography.labelLarge,
                    color = AlabasterMist,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "• ${appLimits.size} INTERCEPTED",
                style = MaterialTheme.typography.labelSmall,
                color = CelestialOpalGlow,
                fontWeight = FontWeight.Bold
            )
        }

        appLimits.forEach { app ->
            val isQuarantined = app.isEnabled && (app.isBlockedCompletely || app.isStrictLocked)
            val ratio = (app.usedTodayMinutes.toFloat() / app.dailyLimitMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
            val badgeLabel = when {
                app.isBlockedCompletely -> "ABSOLUTE LOCK"
                app.isStrictLocked -> "STRICT SHIELD"
                app.appName.contains("YouTube", ignoreCase = true) -> "STRIPPED"
                else -> "SURGICAL"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isQuarantined) RadiantEmber.copy(alpha = 0.38f) else ObsidianHairlineBorder,
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // App Header Row + Switch
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
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isQuarantined) RadiantEmberContainer else ObsidianSurfaceCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.appName.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isQuarantined) RadiantEmber else CelestialOpalGlow,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AlabasterMist,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isQuarantined) RadiantEmberContainer else CelestialOpalContainer
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isQuarantined) RadiantEmberSoft else CelestialOpalGlow,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = app.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSlate
                                )
                            }
                        }

                        Switch(
                            checked = app.isEnabled,
                            onCheckedChange = { onToggleAppLimit(app) },
                            modifier = Modifier.testTag("app_limit_switch_${app.iconKey}")
                        )
                    }

                    // Recessed Interceptor Status Box (Matching Screenshot 2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isQuarantined) RadiantEmberContainer.copy(alpha = 0.45f)
                                else ObsidianInsetTrough
                            )
                            .border(
                                width = 1.dp,
                                color = if (isQuarantined) RadiantEmber.copy(alpha = 0.35f) else ObsidianHairlineBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isQuarantined) {
                                    "Full Domain Quarantine"
                                } else {
                                    "Feed Interceptor Active"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = AlabasterMist,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isQuarantined) {
                                    "Immediate redirect to Black Liquid Pop-Up · ${app.blockedCountToday} blocks"
                                } else {
                                    "Used ${app.usedTodayMinutes}m today · ${app.pickupsToday} pickups"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isQuarantined) RadiantEmberSoft else MutedSlate
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isQuarantined) RadiantEmberContainer else CelestialOpalContainer
                        ) {
                            Text(
                                text = if (isQuarantined) "0 SEC ACCESS" else "INTERCEPTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isQuarantined) RadiantEmberSoft else CelestialOpalGlow,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Daily Air Allowance Header + Progress + Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Air Allowance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                        Text(
                            text = if (app.isBlockedCompletely) {
                                "0 min (Quarantined)"
                            } else {
                                "${app.dailyLimitMinutes} min"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isQuarantined) RadiantEmberSoft else CelestialOpalGlow,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { if (isQuarantined) 1f else ratio.coerceAtLeast(0.22f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (isQuarantined || ratio >= 0.9f) RadiantEmber else CelestialOpalGlow,
                        trackColor = ObsidianSurfaceHigh
                    )

                    Slider(
                        value = app.dailyLimitMinutes.toFloat(),
                        onValueChange = { onUpdateAppLimitMinutes(app, it.roundToInt()) },
                        valueRange = 10f..120f,
                        steps = 21,
                        colors = SliderDefaults.colors(
                            thumbColor = CelestialOpalGlow,
                            activeTrackColor = CelestialOpal,
                            inactiveTrackColor = ObsidianSurfaceHigh
                        ),
                        modifier = Modifier.testTag("app_limit_slider_${app.iconKey}")
                    )

                    // Tactile Action Row
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
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (app.isBlockedCompletely) RadiantEmber else CelestialOpal,
                                contentColor = ObsidianVoidCanvas
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (app.isBlockedCompletely) "Unblock" else "Block Now",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onToggleAppStrict(app) },
                            modifier = Modifier.testTag("app_strict_button_${app.iconKey}"),
                            shape = RoundedCornerShape(50),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = SolidColor(ObsidianBorderSubtle)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (app.isStrictLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (app.isStrictLocked) RadiantEmberSoft else AlabasterMist,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (app.isStrictLocked) "Strict" else "Normal",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlabasterMist
                            )
                        }

                        FilledTonalButton(
                            onClick = { onTestAppIntercept(app) },
                            modifier = Modifier.testTag("app_test_intercept_${app.iconKey}"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ObsidianSurfaceCard,
                                contentColor = CelestialOpalGlow
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Preview Liquid Pop-Up",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pop-Up",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        IconButton(
                            onClick = { onRemoveAppFromBlocker(app) },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("app_delete_button_${app.iconKey}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove ${app.appName}",
                                tint = MutedSlate,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Full-Width "+ ADD APP OR DISTRACTING DOMAIN" Button (Matching Bottom of Screenshot 2)
        OutlinedButton(
            onClick = onChooseAppToBlockClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("choose_app_to_block_button"),
            shape = RoundedCornerShape(20.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = SolidColor(CelestialOpal.copy(alpha = 0.45f))
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ObsidianSurfaceLow,
                contentColor = AlabasterMist
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = CelestialOpalGlow,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ADD APP OR DISTRACTING DOMAIN",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FrictionRitualZenBrakeCard(onTestFrictionRitual: () -> Unit) {
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CelestialOpalContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = null,
                            tint = CelestialOpalGlow
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "4-4-4 Friction Ritual",
                                style = MaterialTheme.typography.titleMedium,
                                color = AlabasterMist,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = RadiantEmberContainer
                            ) {
                                Text(
                                    text = "ZEN BRAKE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RadiantEmberSoft,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Breaks dopamine loops before habit strikes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }
            }

            Text(
                text = "A deliberate neuro-pause: when you instinctively tap a blocked portal, the Black Liquid Circle rises with a breath sequence before any bypass override can open.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSlate
            )

            // Recessed Circular 4 SEC Breath Gauge Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianInsetTrough)
                    .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(20.dp))
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = CelestialOpal.copy(alpha = 0.16f),
                            radius = size.minDimension / 2f
                        )
                        drawCircle(
                            color = CelestialOpalGlow,
                            radius = size.minDimension * 0.44f,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "4",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AlabasterMist,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "SEC",
                            style = MaterialTheme.typography.labelSmall,
                            color = CelestialOpalGlow
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = CelestialOpalContainer
                ) {
                    Text(
                        text = "• INHALE (4S)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CelestialOpalGlow,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Breathe in calmly as chest expands...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }

            // Full-Width Radiant Ember "TEST FRICTION RITUAL" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onTestFrictionRitual,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = RadiantEmber,
                            spotColor = RadiantEmber
                        ),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadiantEmber,
                        contentColor = ObsidianVoidCanvas
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TEST FRICTION RITUAL",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianSurfaceCard)
                        .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(16.dp))
                        .clickable { onTestFrictionRitual() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Configure Ritual",
                        tint = MutedSlate
                    )
                }
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceLow)
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CelestialOpalContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SlowMotionVideo,
                        contentDescription = null,
                        tint = CelestialOpalGlow
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Surgical Short-Form Feed Shields",
                        style = MaterialTheme.typography.titleMedium,
                        color = AlabasterMist,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Neutralize Reels, Shorts & TikTok while keeping Direct Messages intact",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
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
                        .background(ObsidianInsetTrough)
                        .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(18.dp))
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
                                style = MaterialTheme.typography.titleSmall,
                                color = AlabasterMist,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${shield.interceptsToday} scroll loops intercepted today",
                                style = MaterialTheme.typography.bodySmall,
                                color = CelestialOpalGlow
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
                            label = { Text(modeBadgeText, color = AlabasterMist) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CelestialOpalGlow,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            modifier = Modifier.testTag("shield_mode_chip_${shield.id}")
                        )

                        AssistChip(
                            onClick = { onToggleShieldKeepDms(shield) },
                            label = {
                                Text(
                                    text = if (shield.allowDirectMessages) "DMs Open" else "Full Lock",
                                    color = AlabasterMist
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = RadiantEmberSoft,
                                    modifier = Modifier.size(15.dp)
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
                                contentDescription = "Preview Liquid Intercept",
                                tint = CelestialOpalGlow
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automated Focus Schedules",
                        style = MaterialTheme.typography.titleMedium,
                        color = AlabasterMist,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Recurring deep work & screen dark curfews",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                }
                Button(
                    onClick = onAddScheduleClick,
                    modifier = Modifier.testTag("add_schedule_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadiantEmber,
                        contentColor = ObsidianVoidCanvas
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", fontWeight = FontWeight.Bold)
                }
            }

            schedules.forEach { schedule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianInsetTrough)
                        .border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(16.dp))
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
                            tint = CelestialOpalGlow
                        )
                        Column {
                            Text(
                                text = schedule.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = AlabasterMist,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${schedule.timeWindowLabel} · ${schedule.daysSummary}",
                                style = MaterialTheme.typography.labelMedium,
                                color = RadiantEmberSoft
                            )
                            Text(
                                text = schedule.targetSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
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
                                tint = MutedSlate
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
        containerColor = ObsidianSurfaceLow,
        title = {
            Text(
                text = "Add App or Distracting Domain",
                style = MaterialTheme.typography.headlineSmall,
                color = AlabasterMist
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
                    placeholder = { Text("e.g., Instagram, YouTube, TikTok, X...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("choose_app_search_input")
                )

                Text(
                    text = "Select a portal to shield:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSlate
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianInsetTrough),
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
                                    if (isSelected) CelestialOpalContainer else ObsidianSurfaceCard
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
                                    color = AlabasterMist,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${app.category} · ${app.packageName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSlate
                                )
                            }
                            if (alreadyInList) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CelestialOpalGlow
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
                        text = "Quarantine immediately (triggers Black Liquid Pop-Up on launch)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AlabasterMist
                    )
                }

                Text(
                    text = "Daily Air Allowance: ${dailyLimitSlider.roundToInt()} mins/day",
                    style = MaterialTheme.typography.labelMedium,
                    color = CelestialOpalGlow
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
                        text = "Enable Strict Mode (locks break overrides)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AlabasterMist
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
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadiantEmber,
                    contentColor = ObsidianVoidCanvas
                ),
                modifier = Modifier.testTag("confirm_block_app_button")
            ) {
                Text(
                    if (blockImmediately) "Engage Shield" else "Save Allowance",
                    fontWeight = FontWeight.Bold
                )
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
        containerColor = ObsidianSurfaceLow,
        title = { Text("New Automated Shield Schedule", color = AlabasterMist) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Routine Title") },
                    placeholder = { Text("Afternoon Deep Work") },
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
                    label = { Text("Shielded Portals") },
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
                        style = MaterialTheme.typography.bodySmall,
                        color = AlabasterMist
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
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadiantEmber,
                    contentColor = ObsidianVoidCanvas
                ),
                modifier = Modifier.testTag("confirm_add_schedule_button")
            ) {
                Text("Save Schedule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedSlate)
            }
        }
    )
}

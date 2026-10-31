package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blocker.RegainOverlayWindowManager
import com.example.blocker.RegainPermissionsManager
import com.example.notifications.RegainNotificationHelper
import com.example.ui.AnimatedPopUpAlert
import com.example.ui.RegainTab
import com.example.ui.RegainUiState
import com.example.ui.RegainViewModel
import com.example.ui.components.MindfulInterceptModal
import com.example.ui.computeAdaptiveLayoutInfo
import com.example.ui.screens.AiCoachInsightsScreen
import com.example.ui.screens.FocusStudioScreen
import com.example.ui.screens.PermissionsAndOverlayControlCard
import com.example.ui.screens.SanctuaryHomeScreen
import com.example.ui.screens.ShieldLimitsScreen
import com.example.ui.screens.StudyRoomsScreen
import com.example.ui.theme.AlabasterMist
import com.example.ui.theme.CelestialOpal
import com.example.ui.theme.CelestialOpalContainer
import com.example.ui.theme.CelestialOpalGlow
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianHairlineBorder
import com.example.ui.theme.ObsidianInsetTrough
import com.example.ui.theme.ObsidianSurfaceCard
import com.example.ui.theme.ObsidianSurfaceLow
import com.example.ui.theme.ObsidianVoidCanvas
import com.example.ui.theme.RadiantEmber
import com.example.ui.theme.RadiantEmberContainer
import com.example.ui.theme.RadiantEmberSoft
import com.example.ui.theme.RegainZenTheme
import com.example.ui.theme.WarmCreamIvory
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var pendingInterceptTarget by mutableStateOf<String?>(null)
    private var pendingInterceptSubtitle by mutableStateOf<String?>(null)
    private var pendingReturnedFromOverlayTarget by mutableStateOf<String?>(null)

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
    }

    override fun onPause() {
        isAppInForeground = false
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isAppInForeground = true
        extractInterceptExtras(intent)

        enableEdgeToEdge()
        setContent {
            RegainZenTheme {
                val regainViewModel: RegainViewModel = viewModel()
                val uiState by regainViewModel.uiState.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) {
                    regainViewModel.refreshPermissionsStatus()
                }

                val requestNotificationPermissionAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !RegainNotificationHelper.hasNotificationPermission(context)
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        RegainPermissionsManager.openNotificationSettings(context)
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isAppInForeground = true
                            regainViewModel.refreshPermissionsStatus()
                        } else if (event == Lifecycle.Event.ON_PAUSE) {
                            isAppInForeground = false
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(pendingReturnedFromOverlayTarget) {
                    val returnedTarget = pendingReturnedFromOverlayTarget
                    if (!returnedTarget.isNullOrBlank()) {
                        regainViewModel.handleReturnedFromOverlaySanctuary(returnedTarget)
                        pendingReturnedFromOverlayTarget = null
                    }
                }

                LaunchedEffect(pendingInterceptTarget, pendingInterceptSubtitle) {
                    val target = pendingInterceptTarget
                    if (!target.isNullOrBlank()) {
                        regainViewModel.handleIncomingInterceptIntent(target, pendingInterceptSubtitle)
                        pendingInterceptTarget = null
                        pendingInterceptSubtitle = null
                    }
                }

                LaunchedEffect(uiState.popUpAlert?.id) {
                    if (uiState.popUpAlert != null) {
                        delay(5500L)
                        regainViewModel.clearBannerMessage()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    RegainZenAdaptiveApp(
                        uiState = uiState,
                        viewModel = regainViewModel,
                        onRequestNotificationPermission = requestNotificationPermissionAction,
                        onRequestOverlayPermission = {
                            RegainPermissionsManager.openOverlayPermissionSettings(context)
                        },
                        onRequestUsagePermission = {
                            RegainPermissionsManager.openUsageAccessSettings(context)
                        },
                        onRequestAccessibilityPermission = {
                            RegainPermissionsManager.openAccessibilitySettings(context)
                        },
                        onRequestBatteryPermission = {
                            RegainPermissionsManager.requestIgnoreBatteryOptimizations(context)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractInterceptExtras(intent)
    }

    private fun extractInterceptExtras(intent: Intent?) {
        val returnedTarget = intent?.getStringExtra(RegainOverlayWindowManager.EXTRA_RETURNED_FROM_OVERLAY)
        if (!returnedTarget.isNullOrBlank()) {
            pendingReturnedFromOverlayTarget = returnedTarget
            intent.removeExtra(RegainOverlayWindowManager.EXTRA_RETURNED_FROM_OVERLAY)
            return
        }
        val target = intent?.getStringExtra(RegainNotificationHelper.EXTRA_INTERCEPT_TARGET)
        val subtitle = intent?.getStringExtra(RegainNotificationHelper.EXTRA_INTERCEPT_SUBTITLE)
        if (!target.isNullOrBlank()) {
            pendingInterceptTarget = target
            pendingInterceptSubtitle = subtitle
            intent.removeExtra(RegainNotificationHelper.EXTRA_INTERCEPT_TARGET)
            intent.removeExtra(RegainNotificationHelper.EXTRA_INTERCEPT_SUBTITLE)
        }
    }

    companion object {
        @Volatile
        var isAppInForeground: Boolean = false
    }
}

@Composable
fun RegainZenAdaptiveApp(
    uiState: RegainUiState,
    viewModel: RegainViewModel,
    onRequestNotificationPermission: () -> Unit = {},
    onRequestOverlayPermission: () -> Unit = {},
    onRequestUsagePermission: () -> Unit = {},
    onRequestAccessibilityPermission: () -> Unit = {},
    onRequestBatteryPermission: () -> Unit = {}
) {
    if (uiState.showPermissionsDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPermissionsDialog,
            containerColor = ObsidianSurfaceLow,
            title = {
                Text(
                    text = "Aegis Shield & Pop-Up Permissions",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlabasterMist
                )
            },
            text = {
                PermissionsAndOverlayControlCard(
                    permissionStatus = uiState.permissionStatus,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestUsagePermission = onRequestUsagePermission,
                    onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                    onRequestBatteryPermission = onRequestBatteryPermission
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.refreshPermissionsStatus()
                        viewModel.dismissPermissionsDialog()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadiantEmber,
                        contentColor = ObsidianVoidCanvas
                    ),
                    modifier = Modifier.testTag("done_permissions_dialog_button")
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissPermissionsDialog()
                        viewModel.selectTab(RegainTab.SHIELD)
                    }
                ) {
                    Text("Open Shield Grid", color = CelestialOpalGlow)
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoidCanvas)
    ) {
        val layoutInfo = computeAdaptiveLayoutInfo(maxWidth = maxWidth, maxHeight = maxHeight)

        if (layoutInfo.useNavigationRail) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                RegainNavigationRail(
                    currentTab = uiState.currentTab,
                    onSelectTab = viewModel::selectTab
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    ObsidianTopAppBar(
                        uiState = uiState,
                        onOpenPermissions = viewModel::openPermissionsDialog,
                        onNavigateSanctuary = { viewModel.selectTab(RegainTab.SANCTUARY) }
                    )
                    StaticStatusFeedbackBanner(
                        popUpAlert = uiState.popUpAlert,
                        fallbackMessage = uiState.statusBannerMessage,
                        onDismiss = viewModel::clearBannerMessage
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        RegainScreenContent(
                            uiState = uiState,
                            layoutInfo = layoutInfo,
                            viewModel = viewModel,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onRequestUsagePermission = onRequestUsagePermission,
                            onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                            onRequestBatteryPermission = onRequestBatteryPermission
                        )
                    }
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = ObsidianVoidCanvas,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    ObsidianTopAppBar(
                        uiState = uiState,
                        onOpenPermissions = viewModel::openPermissionsDialog,
                        onNavigateSanctuary = { viewModel.selectTab(RegainTab.SANCTUARY) }
                    )
                },
                bottomBar = {
                    RegainBottomNavigationBar(
                        currentTab = uiState.currentTab,
                        onSelectTab = viewModel::selectTab
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    StaticStatusFeedbackBanner(
                        popUpAlert = uiState.popUpAlert,
                        fallbackMessage = uiState.statusBannerMessage,
                        onDismiss = viewModel::clearBannerMessage
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        RegainScreenContent(
                            uiState = uiState,
                            layoutInfo = layoutInfo,
                            viewModel = viewModel,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onRequestUsagePermission = onRequestUsagePermission,
                            onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                            onRequestBatteryPermission = onRequestBatteryPermission
                        )
                    }
                }
            }
        }

        MindfulInterceptModal(
            state = uiState.interceptState,
            strictPhraseTarget = uiState.userProgress.strictPassphrase,
            onReturnToFocus = viewModel::chooseMindfulReturnFromIntercept,
            onGrantFiveMinGrace = viewModel::grantEmergencyFiveMinBypass,
            onDismiss = viewModel::dismissInterceptModal
        )
    }
}

/**
 * Top bar matching the Obsidian Sanctuary & Tactile Chrono reference designs:
 * - Left: Glowing Radiant Ember dot + Screen/Brand Title + Version pill (`v1.3`)
 * - Right: Streak Pill (`14D` / `streakDays`), Leaf Coins Pill (`480`), Shield/Permissions button, and Avatar badge.
 */
@Composable
private fun ObsidianTopAppBar(
    uiState: RegainUiState,
    onOpenPermissions: () -> Unit,
    onNavigateSanctuary: () -> Unit
) {
    val headerTitle = when (uiState.currentTab) {
        RegainTab.SANCTUARY -> "Sanctuary"
        RegainTab.FOCUS -> "RegainZen"
        RegainTab.SHIELD -> "Shield"
        RegainTab.ROOMS -> "Rooms"
        RegainTab.AI_COACH -> "RegainZen"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clickable { onNavigateSanctuary() }
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(
                        if (uiState.currentTab == RegainTab.SHIELD) CelestialOpalGlow
                        else RadiantEmber
                    )
            )
            Text(
                text = headerTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AlabasterMist
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ObsidianSurfaceCard,
                modifier = Modifier.border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "V1.3",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Streak Badge Pill (e.g. "14D")
            Surface(
                shape = RoundedCornerShape(50),
                color = CelestialOpalContainer,
                modifier = Modifier.border(1.dp, CelestialOpal.copy(alpha = 0.35f), RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = CelestialOpalGlow,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${uiState.userProgress.streakDays}D",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CelestialOpalGlow
                    )
                }
            }

            // Tactile Chrono Coins Pill (e.g. "480")
            Surface(
                shape = RoundedCornerShape(50),
                color = ObsidianSurfaceCard,
                modifier = Modifier.border(1.dp, ObsidianHairlineBorder, RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = WarmCreamIvory,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${uiState.userProgress.leafCoins}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AlabasterMist
                    )
                }
            }

            // Permissions / Shield Quick Icon Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurfaceCard)
                    .border(
                        width = 1.dp,
                        color = if (uiState.permissionStatus.allGranted) {
                            CelestialOpal.copy(alpha = 0.45f)
                        } else {
                            RadiantEmber.copy(alpha = 0.55f)
                        },
                        shape = CircleShape
                    )
                    .clickable(onClick = onOpenPermissions),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permissions & Shield Setup",
                    tint = if (uiState.permissionStatus.allGranted) CelestialOpalGlow else RadiantEmberSoft,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Warm Coral Avatar Pill matching screenshots
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(RadiantEmberSoft)
                    .clickable(onClick = onNavigateSanctuary),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_rega_mascot),
                    contentDescription = "Rega Companion",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

/**
 * Version 1.3.0 — Static in-app tab switching (theme/tab animations reserved for v1.3.1).
 */
@Composable
private fun RegainScreenContent(
    uiState: RegainUiState,
    layoutInfo: com.example.ui.AdaptiveLayoutInfo,
    viewModel: RegainViewModel,
    onRequestNotificationPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestBatteryPermission: () -> Unit
) {
    when (uiState.currentTab) {
        RegainTab.SANCTUARY -> SanctuaryHomeScreen(
            uiState = uiState,
            layoutInfo = layoutInfo,
            onNavigateTab = viewModel::selectTab,
            onStartQuickFocus = {
                viewModel.selectTab(RegainTab.FOCUS)
                if (!uiState.timerState.isRunning) {
                    viewModel.toggleStartPauseFocusTimer()
                }
            },
            onPreviewReelsIntercept = {
                val reelsShield = uiState.shortFormShields.find { it.id == "instagram_reels" }
                val app = uiState.appLimits.find { it.packageName == "com.instagram.android" }
                viewModel.triggerMindfulIntercept(
                    targetName = "Instagram Reels",
                    targetSubtitle = "Targeted Short-Form Feed Shield (DMs Open)",
                    usedMinutes = reelsShield?.usedMinutesToday ?: 0,
                    limitMinutes = app?.dailyLimitMinutes ?: 30,
                    isStrictLocked = false,
                    keepDmsAvailable = true,
                    packageName = "com.instagram.android"
                )
            },
            onOpenPermissionsSetup = viewModel::openPermissionsDialog,
            onClaimQuest = viewModel::claimDailyQuestReward,
            onUpdateDailyGoal = viewModel::updateMindfulDailyGoal
        )

        RegainTab.FOCUS -> FocusStudioScreen(
            timerState = uiState.timerState,
            recentSessions = uiState.focusSessions,
            layoutInfo = layoutInfo,
            onBackToHome = { viewModel.selectTab(RegainTab.SANCTUARY) },
            onSelectMode = viewModel::selectTimerMode,
            onUpdateCustomMinutes = viewModel::updateCustomCountdownMinutes,
            onSelectTag = viewModel::selectFocusTag,
            onToggleStrictLock = viewModel::toggleStrictFocusLock,
            onSelectAmbientSound = viewModel::selectAmbientSound,
            onUpdateVolume = viewModel::updateSoundVolume,
            onToggleSoundPreview = viewModel::toggleAmbientPreview,
            onToggleStartPause = viewModel::toggleStartPauseFocusTimer,
            onResetTimer = viewModel::resetFocusTimer,
            onSaveExactStudiedTime = viewModel::completeCurrentFocusSession
        )

        RegainTab.SHIELD -> ShieldLimitsScreen(
            permissionStatus = uiState.permissionStatus,
            discoverableApps = uiState.discoverableApps,
            shortFormShields = uiState.shortFormShields,
            appLimits = uiState.appLimits,
            schedules = uiState.schedules,
            layoutInfo = layoutInfo,
            onBackToHome = { viewModel.selectTab(RegainTab.SANCTUARY) },
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestOverlayPermission = onRequestOverlayPermission,
            onRequestUsagePermission = onRequestUsagePermission,
            onRequestAccessibilityPermission = onRequestAccessibilityPermission,
            onRequestBatteryPermission = onRequestBatteryPermission,
            onAddOrBlockApp = viewModel::addOrBlockSelectedApp,
            onToggleBlockAppCompletely = viewModel::toggleBlockAppCompletely,
            onRemoveAppFromBlocker = viewModel::removeAppFromBlocker,
            onCycleShieldMode = viewModel::cycleShortFormShieldMode,
            onToggleShieldEnabled = viewModel::toggleShortFormShieldEnabled,
            onToggleShieldKeepDms = viewModel::toggleShortFormKeepDms,
            onTestShieldIntercept = { shield ->
                viewModel.triggerMindfulIntercept(
                    targetName = "${shield.platformName} (${shield.feedName})",
                    targetSubtitle = "Targeted Short-Form Shield",
                    usedMinutes = shield.usedMinutesToday,
                    limitMinutes = shield.dailyCapMinutes.coerceAtLeast(15),
                    isStrictLocked = false,
                    keepDmsAvailable = shield.allowDirectMessages
                )
            },
            onToggleAppLimit = viewModel::toggleAppLimitEnabled,
            onUpdateAppLimitMinutes = viewModel::updateAppDailyLimitMinutes,
            onToggleAppStrict = viewModel::toggleAppStrictLock,
            onTestAppIntercept = { app ->
                viewModel.triggerMindfulIntercept(
                    targetName = app.appName,
                    targetSubtitle = if (app.isBlockedCompletely) {
                        "Blocked Completely by Regain Zen App Blocker"
                    } else {
                        "${app.category} · Daily Cap ${app.dailyLimitMinutes}m"
                    },
                    usedMinutes = app.usedTodayMinutes,
                    limitMinutes = app.dailyLimitMinutes,
                    isStrictLocked = app.isStrictLocked,
                    keepDmsAvailable = false,
                    packageName = app.packageName
                )
            },
            onToggleSchedule = viewModel::toggleScheduleEnabled,
            onAddSchedule = viewModel::addCustomSchedule,
            onDeleteSchedule = viewModel::deleteSchedule
        )

        RegainTab.ROOMS -> StudyRoomsScreen(
            studyRoomState = uiState.studyRoomState,
            userFocusedMinutesToday = uiState.totalFocusedMinutesToday,
            userStreakDays = uiState.userProgress.streakDays,
            layoutInfo = layoutInfo,
            onBackToHome = { viewModel.selectTab(RegainTab.SANCTUARY) },
            onJoinRoom = viewModel::joinStudyRoom,
            onToggleRoomTicker = viewModel::toggleStudyRoomFocusTicker,
            onSendCheer = viewModel::sendRoomFocusCheer
        )

        RegainTab.AI_COACH -> AiCoachInsightsScreen(
            uiState = uiState,
            singlePromptPreview = viewModel.buildLiveSinglePromptPreview(),
            layoutInfo = layoutInfo,
            onBackToHome = { viewModel.selectTab(RegainTab.SANCTUARY) },
            onGenerateDailyInsight = { viewModel.generateDailyAiCoachInsight() },
            onSetMode = viewModel::setAiCoachMode,
            onSendChatMessage = viewModel::sendAiChatMessage,
            onUpdateApiKey = viewModel::updateCustomGeminiApiKey,
            onToggleApiKeyDialog = viewModel::setShowApiKeyDialog
        )
    }
}

@Composable
private fun RegainBottomNavigationBar(
    currentTab: RegainTab,
    onSelectTab: (RegainTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(
                width = 1.dp,
                color = ObsidianHairlineBorder,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .testTag("bottom_navigation_bar"),
        containerColor = ObsidianInsetTrough,
        tonalElevation = 0.dp
    ) {
        RegainTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(selected),
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.shortNavLabel(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CelestialOpalGlow,
                    selectedTextColor = CelestialOpalGlow,
                    indicatorColor = ObsidianSurfaceCard,
                    unselectedIconColor = MutedSlate,
                    unselectedTextColor = MutedSlate
                ),
                modifier = Modifier.testTag("nav_tab_${tab.route}")
            )
        }
    }
}

@Composable
private fun RegainNavigationRail(
    currentTab: RegainTab,
    onSelectTab: (RegainTab) -> Unit
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .border(1.dp, ObsidianHairlineBorder)
            .testTag("side_navigation_rail"),
        containerColor = ObsidianInsetTrough
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        RegainTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(selected),
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.shortNavLabel(),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = CelestialOpalGlow,
                    selectedTextColor = CelestialOpalGlow,
                    indicatorColor = ObsidianSurfaceCard,
                    unselectedIconColor = MutedSlate,
                    unselectedTextColor = MutedSlate
                ),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .testTag("nav_rail_${tab.route}")
            )
        }
    }
}

@Composable
private fun StaticStatusFeedbackBanner(
    popUpAlert: AnimatedPopUpAlert?,
    fallbackMessage: String?,
    onDismiss: () -> Unit
) {
    val isVisible = popUpAlert != null || !fallbackMessage.isNullOrBlank()
    if (!isVisible) return

    val title = popUpAlert?.title ?: "Regain Zen Telemetry"
    val body = popUpAlert?.message ?: fallbackMessage.orEmpty()
    val badge = popUpAlert?.badgeText ?: "v1.3.0"
    val isWarning = popUpAlert?.isWarning == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(
                width = 1.dp,
                color = if (isWarning) RadiantEmber.copy(alpha = 0.5f) else CelestialOpal.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onDismiss() }
            .testTag("status_feedback_banner"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ObsidianSurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isWarning) RadiantEmberContainer else CelestialOpalContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (isWarning) RadiantEmber else CelestialOpalGlow,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AlabasterMist
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ObsidianInsetTrough
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isWarning) RadiantEmberSoft else CelestialOpalGlow,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss notification",
                    tint = MutedSlate,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun RegainTab.shortNavLabel(): String = when (this) {
    RegainTab.SANCTUARY -> "Sanctuary"
    RegainTab.FOCUS -> "Focus"
    RegainTab.SHIELD -> "Shield"
    RegainTab.ROOMS -> "Rooms"
    RegainTab.AI_COACH -> "Coach"
}

private fun RegainTab.icon(selected: Boolean): ImageVector = when (this) {
    RegainTab.SANCTUARY -> if (selected) Icons.Filled.Spa else Icons.Outlined.Spa
    RegainTab.FOCUS -> if (selected) Icons.Filled.Timer else Icons.Outlined.Timer
    RegainTab.SHIELD -> if (selected) Icons.Filled.Security else Icons.Outlined.Security
    RegainTab.ROOMS -> if (selected) Icons.Filled.Groups else Icons.Outlined.Groups
    RegainTab.AI_COACH -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
}

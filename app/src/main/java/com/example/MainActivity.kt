package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.RegainTab
import com.example.ui.RegainUiState
import com.example.ui.RegainViewModel
import com.example.ui.components.MindfulInterceptModal
import com.example.ui.computeAdaptiveLayoutInfo
import com.example.ui.screens.AiCoachInsightsScreen
import com.example.ui.screens.FocusStudioScreen
import com.example.ui.screens.SanctuaryHomeScreen
import com.example.ui.screens.ShieldLimitsScreen
import com.example.ui.screens.StudyRoomsScreen
import com.example.ui.theme.RegainZenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RegainZenTheme {
                val regainViewModel: RegainViewModel = viewModel()
                val uiState by regainViewModel.uiState.collectAsStateWithLifecycle()

                RegainZenAdaptiveApp(
                    uiState = uiState,
                    viewModel = regainViewModel
                )
            }
        }
    }
}

@Composable
fun RegainZenAdaptiveApp(
    uiState: RegainUiState,
    viewModel: RegainViewModel
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    StatusFeedbackToastBanner(
                        message = uiState.statusBannerMessage,
                        onDismiss = viewModel::clearBannerMessage
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        RegainScreenContent(
                            uiState = uiState,
                            layoutInfo = layoutInfo,
                            viewModel = viewModel
                        )
                    }
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
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
                    StatusFeedbackToastBanner(
                        message = uiState.statusBannerMessage,
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
                            viewModel = viewModel
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

@Composable
private fun RegainScreenContent(
    uiState: RegainUiState,
    layoutInfo: com.example.ui.AdaptiveLayoutInfo,
    viewModel: RegainViewModel
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
                    keepDmsAvailable = true
                )
            },
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
            onCompleteNowDemo = viewModel::completeCurrentFocusSession
        )

        RegainTab.SHIELD -> ShieldLimitsScreen(
            shortFormShields = uiState.shortFormShields,
            appLimits = uiState.appLimits,
            schedules = uiState.schedules,
            layoutInfo = layoutInfo,
            onBackToHome = { viewModel.selectTab(RegainTab.SANCTUARY) },
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
                    targetSubtitle = "${app.category} · Daily Cap ${app.dailyLimitMinutes}m",
                    usedMinutes = app.usedTodayMinutes,
                    limitMinutes = app.dailyLimitMinutes,
                    isStrictLocked = app.isStrictLocked,
                    keepDmsAvailable = false
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
            onResetQuotaForDemo = viewModel::resetDailyAiQuotaForTesting
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
            .testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        RegainTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationBarItem(
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
                        text = tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
            .testTag("side_navigation_rail"),
        containerColor = MaterialTheme.colorScheme.surface
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
                label = { Text(tab.label) },
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .testTag("nav_rail_${tab.route}")
            )
        }
    }
}

@Composable
private fun StatusFeedbackToastBanner(
    message: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { onDismiss() }
                .testTag("status_feedback_banner"),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun RegainTab.icon(selected: Boolean): ImageVector = when (this) {
    RegainTab.SANCTUARY -> if (selected) Icons.Filled.Spa else Icons.Outlined.Spa
    RegainTab.FOCUS -> if (selected) Icons.Filled.Timer else Icons.Outlined.Timer
    RegainTab.SHIELD -> if (selected) Icons.Filled.Security else Icons.Outlined.Security
    RegainTab.ROOMS -> if (selected) Icons.Filled.Groups else Icons.Outlined.Groups
    RegainTab.AI_COACH -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
}

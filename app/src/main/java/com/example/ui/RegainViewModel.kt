package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.SinglePromptAiCoach
import com.example.audio.AmbientSoundPreset
import com.example.audio.ZenSoundEngine
import com.example.blocker.BlockerPermissionStatus
import com.example.blocker.RegainBlockerMonitorService
import com.example.blocker.RegainOverlayWindowManager
import com.example.blocker.RegainPermissionsManager
import com.example.data.AiCoachInsightEntity
import com.example.data.AppLimitEntity
import com.example.data.DailyQuestEntity
import com.example.data.FocusSessionEntity
import com.example.data.RegainDatabase
import com.example.data.RegainRepository
import com.example.data.ScheduleBlockEntity
import com.example.data.SelectableDeviceApp
import com.example.data.ShortFormShieldEntity
import com.example.data.UserProgressEntity
import com.example.notifications.RegainNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class RegainTab(val route: String, val label: String) {
    SANCTUARY("sanctuary", "Sanctuary"),
    FOCUS("focus", "Focus"),
    SHIELD("shield", "App Blocker"),
    ROOMS("rooms", "Study Rooms"),
    AI_COACH("ai_coach", "AI Coach")
}

enum class FocusTimerMode(val label: String, val defaultMinutes: Int) {
    POMODORO_25("Pomodoro 25/5", 25),
    POMODORO_50("Deep 50/10", 50),
    COUNTDOWN("Custom Timer", 30),
    STOPWATCH("Flow Stopwatch", 0)
}

data class AnimatedPopUpAlert(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val badgeText: String = "Regain Zen v1.2",
    val actionLabel: String? = null,
    val isWarning: Boolean = false
)

data class VirtualStudyPeer(
    val id: String,
    val name: String,
    val countryFlag: String,
    val activityTag: String,
    val focusedMinutesToday: Int,
    val streakDays: Int,
    val statusNote: String
)

data class VirtualStudyRoom(
    val id: String,
    val name: String,
    val subtitle: String,
    val ambientRecommendation: AmbientSoundPreset,
    val activeMembersCount: Int,
    val peers: List<VirtualStudyPeer>
)

data class MindfulInterceptState(
    val isVisible: Boolean = false,
    val targetName: String = "Instagram Reels",
    val targetSubtitle: String = "Short-Form Infinite Feed Shield",
    val usedMinutes: Int = 0,
    val limitMinutes: Int = 30,
    val isStrictLocked: Boolean = false,
    val keepDmsAvailable: Boolean = true,
    val packageName: String = "com.regainzen.preview"
)

data class FocusTaskItem(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val isActive: Boolean = false
)

fun defaultFocusTasks(): List<FocusTaskItem> = listOf(
    FocusTaskItem(
        id = "task_1",
        title = "System Wireframes Draft",
        durationMinutes = 25,
        isCompleted = true,
        isActive = false
    ),
    FocusTaskItem(
        id = "task_2",
        title = "Design Review with Martin",
        durationMinutes = 25,
        isCompleted = false,
        isActive = true
    ),
    FocusTaskItem(
        id = "task_3",
        title = "Documentation & Handover specs",
        durationMinutes = 50,
        isCompleted = false,
        isActive = false
    )
)

data class ActiveTimerUiState(
    val mode: FocusTimerMode = FocusTimerMode.POMODORO_25,
    val customCountdownMinutes: Int = 25,
    val focusDurationMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val currentRound: Int = 3,
    val totalRounds: Int = 4,
    val activeSprintTitle: String = "DESIGN ARCHITECTURE • SPRINT 3",
    val selectedTag: String = "Design Review with Martin",
    val isRunning: Boolean = false,
    val isBreakPhase: Boolean = false,
    val elapsedSeconds: Int = 0,
    val totalTargetSeconds: Int = 25 * 60,
    val strictFocusLock: Boolean = true,
    val selectedSound: AmbientSoundPreset = AmbientSoundPreset.ZEN_RAIN,
    val soundVolume: Float = 0.65f,
    val isSoundPlayingPreview: Boolean = false,
    val tasks: List<FocusTaskItem> = defaultFocusTasks()
) {
    val exactElapsedFormatted: String
        get() = formatExactDurationSeconds(elapsedSeconds)
}

data class StudyRoomUiState(
    val joinedRoomId: String? = "room_kyoto_zen",
    val isInRoomFocus: Boolean = false,
    val roomSessionSeconds: Int = 0,
    val cheerCount: Int = 0,
    val rooms: List<VirtualStudyRoom> = defaultStudyRooms()
)

enum class AiCoachMode {
    INSIGHTS,
    CHAT
}

data class AiCoachUiState(
    val mode: AiCoachMode = AiCoachMode.INSIGHTS,
    val chatHistory: List<com.example.ai.ChatMessage> = emptyList(),
    val isSendingChat: Boolean = false,
    val showApiKeyDialog: Boolean = false
)

data class RegainUiState(
    val currentTab: RegainTab = RegainTab.SANCTUARY,
    val appLimits: List<AppLimitEntity> = emptyList(),
    val shortFormShields: List<ShortFormShieldEntity> = emptyList(),
    val focusSessions: List<FocusSessionEntity> = emptyList(),
    val schedules: List<ScheduleBlockEntity> = emptyList(),
    val dailyQuests: List<DailyQuestEntity> = emptyList(),
    val userProgress: UserProgressEntity = UserProgressEntity(),
    val aiInsights: List<AiCoachInsightEntity> = emptyList(),
    val todayDateKey: String = "",
    val timerState: ActiveTimerUiState = ActiveTimerUiState(),
    val studyRoomState: StudyRoomUiState = StudyRoomUiState(),
    val interceptState: MindfulInterceptState = MindfulInterceptState(),
    val isGeneratingAiInsight: Boolean = false,
    val statusBannerMessage: String? = null,
    val popUpAlert: AnimatedPopUpAlert? = null,
    val permissionStatus: BlockerPermissionStatus = BlockerPermissionStatus(),
    val showPermissionsDialog: Boolean = false,
    val discoverableApps: List<SelectableDeviceApp> = emptyList(),
    val aiCoachState: AiCoachUiState = AiCoachUiState()
) {
    val hasSystemUsagePermission: Boolean
        get() = permissionStatus.hasUsageStatsPermission

    val todayAiInsight: AiCoachInsightEntity?
        get() = aiInsights.firstOrNull { it.dateKey == todayDateKey }

    val dailyAiRequestsRemaining: Int
        get() = if ((todayAiInsight?.requestsUsedToday ?: 0) >= 1) 0 else 1

    val totalScreenTimeMinutesToday: Int
        get() = appLimits.sumOf { it.usedTodayMinutes }

    val totalPickupsToday: Int
        get() = appLimits.sumOf { it.pickupsToday }

    val totalShortFormInterceptsToday: Int
        get() = shortFormShields.sumOf { it.interceptsToday } + appLimits.sumOf { it.blockedCountToday }

    val activelyBlockedAppsCount: Int
        get() = appLimits.count { it.isEnabled && (it.isBlockedCompletely || it.isStrictLocked) }

    val totalFocusedSecondsToday: Int
        get() = focusSessions.sumOf {
            if (it.durationSeconds > 0) it.durationSeconds else it.durationMinutes * 60
        } + studyRoomState.roomSessionSeconds + (if (timerState.isRunning) timerState.elapsedSeconds else 0)

    val totalFocusedMinutesToday: Int
        get() = totalFocusedSecondsToday / 60

    val exactTotalFocusedFormatted: String
        get() = formatExactDurationSeconds(totalFocusedSecondsToday)
}

fun formatExactDurationSeconds(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val hrs = clamped / 3600
    val mins = (clamped % 3600) / 60
    val secs = clamped % 60
    return when {
        hrs > 0 -> "${hrs}h ${mins}m ${secs}s"
        mins > 0 && secs > 0 -> "${mins}m ${secs}s"
        mins > 0 -> "${mins}m 0s"
        else -> "${secs}s"
    }
}

class RegainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val database = RegainDatabase.getDatabase(application)
    private val repository = RegainRepository(database.regainDao(), application)
    private val soundEngine = ZenSoundEngine(viewModelScope)
    private val aiCoach = SinglePromptAiCoach()
    private val geminiService = com.example.ai.GeminiAiService()

    private val _currentTab = MutableStateFlow(RegainTab.SANCTUARY)
    private val _timerState = MutableStateFlow(ActiveTimerUiState())
    private val _studyRoomState = MutableStateFlow(StudyRoomUiState())
    private val _interceptState = MutableStateFlow(MindfulInterceptState())
    private val _isGeneratingAi = MutableStateFlow(false)
    private val _bannerMessage = MutableStateFlow<String?>(null)
    private val _popUpAlert = MutableStateFlow<AnimatedPopUpAlert?>(null)
    private val _permissionStatus = MutableStateFlow(RegainPermissionsManager.checkAllPermissions(application))
    private val _showPermissionsDialog = MutableStateFlow(false)
    private val _discoverableApps = MutableStateFlow<List<SelectableDeviceApp>>(emptyList())
    private val _aiCoachState = MutableStateFlow(AiCoachUiState())

    private var timerJob: Job? = null
    private var roomTickerJob: Job? = null

    private val dbCombinedFlow = combine(
        repository.appLimits,
        repository.shortFormShields,
        repository.focusSessions,
        repository.schedules,
        repository.dailyQuests
    ) { apps, shields, sessions, schedules, quests ->
        DbBundle(apps, shields, sessions, schedules, quests)
    }

    private val localUiFlow = combine(
        _currentTab,
        _timerState,
        _studyRoomState,
        _interceptState,
        _isGeneratingAi
    ) { tab, timer, rooms, intercept, generatingAi ->
        LocalUiBundle(tab, timer, rooms, intercept, generatingAi)
    }

    private val overlayAndPermsFlow = combine(
        combine(_bannerMessage, _popUpAlert) { b, p -> b to p },
        _permissionStatus,
        _showPermissionsDialog,
        _discoverableApps,
        _aiCoachState
    ) { bannerPopUp, perms, showPerms, apps, aiCoachState ->
        OverlayPermsBundle(bannerPopUp.first, bannerPopUp.second, perms, showPerms, apps, aiCoachState)
    }

    val uiState: StateFlow<RegainUiState> = combine(
        dbCombinedFlow,
        repository.userProgress,
        repository.aiInsights,
        localUiFlow,
        overlayAndPermsFlow
    ) { db, progress, insights, local, overlay ->
        RegainUiState(
            currentTab = local.tab,
            appLimits = db.apps,
            shortFormShields = db.shields,
            focusSessions = db.sessions,
            schedules = db.schedules,
            dailyQuests = db.quests,
            userProgress = progress ?: UserProgressEntity(),
            aiInsights = insights,
            todayDateKey = repository.todayDateKey(),
            timerState = local.timer,
            studyRoomState = local.rooms,
            interceptState = local.intercept,
            isGeneratingAiInsight = local.generatingAi,
            statusBannerMessage = overlay.banner,
            popUpAlert = overlay.popUp,
            permissionStatus = overlay.perms,
            showPermissionsDialog = overlay.showPerms,
            discoverableApps = overlay.discoverableApps,
            aiCoachState = overlay.aiCoachState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RegainUiState(
            todayDateKey = repository.todayDateKey(),
            permissionStatus = RegainPermissionsManager.checkAllPermissions(application)
        )
    )

    init {
        RegainNotificationHelper.ensureChannelsCreated(appContext)
        RegainBlockerMonitorService.ensureServiceRunning(appContext)
        viewModelScope.launch {
            repository.ensureSeededAndSyncSystemUsage()
            _discoverableApps.value = repository.getDiscoverableAppsForBlocker()
            refreshPermissionsStatus()
        }
    }

    fun selectTab(tab: RegainTab) {
        _currentTab.value = tab
    }

    fun clearBannerMessage() {
        _bannerMessage.value = null
        _popUpAlert.value = null
    }

    fun openPermissionsDialog() {
        refreshPermissionsStatus()
        _showPermissionsDialog.value = true
    }

    fun dismissPermissionsDialog() {
        _showPermissionsDialog.value = false
    }

    fun refreshPermissionsStatus() {
        val status = RegainPermissionsManager.checkAllPermissions(appContext)
        _permissionStatus.value = status
        RegainBlockerMonitorService.ensureServiceRunning(appContext)
        if (status.hasUsageStatsPermission) {
            viewModelScope.launch {
                repository.syncSystemUsageNow()
            }
        }
    }

    fun handleReturnedFromOverlaySanctuary(targetName: String?) {
        if (targetName.isNullOrBlank()) return
        RegainOverlayWindowManager.dismissFloatingPopUp(appContext)
        _interceptState.update { it.copy(isVisible = false) }
        _currentTab.value = RegainTab.SANCTUARY
        postBanner(
            message = "Closed $targetName & returned to Regain Zen Focus Sanctuary! Earned +15 Leaf Coins & saved ~8m.",
            title = "Returned to Focus Sanctuary",
            badgeText = "+15 Leaf Coins"
        )
    }

    fun handleIncomingInterceptIntent(targetName: String?, subtitle: String?) {
        if (targetName.isNullOrBlank()) return
        val matchedApp = uiState.value.appLimits.find {
            it.appName.equals(targetName, ignoreCase = true) || it.packageName == targetName
        }
        triggerMindfulIntercept(
            targetName = matchedApp?.appName ?: targetName,
            targetSubtitle = subtitle ?: "Blocked by Regain Zen App & Shorts Shield",
            usedMinutes = matchedApp?.usedTodayMinutes ?: 0,
            limitMinutes = matchedApp?.dailyLimitMinutes ?: 30,
            isStrictLocked = matchedApp?.isStrictLocked ?: false,
            keepDmsAvailable = false,
            packageName = matchedApp?.packageName ?: "com.blocked.app"
        )
    }

    private fun postBanner(
        message: String,
        title: String = "Regain Zen Pop-Up",
        badgeText: String = "Mindful Update",
        isWarning: Boolean = false
    ) {
        _bannerMessage.value = message
        _popUpAlert.value = AnimatedPopUpAlert(
            title = title,
            message = message,
            badgeText = badgeText,
            isWarning = isWarning
        )
    }

    // --- FOCUS & POMODORO TIMER (RECORDS EXACT TIME STUDIED + AUTO-ADVANCES BREAKS) ---

    fun selectTimerMode(mode: FocusTimerMode) {
        if (_timerState.value.isRunning) return
        val s = _timerState.value
        val nextFocusMinutes = when (mode) {
            FocusTimerMode.POMODORO_25 -> 25
            FocusTimerMode.POMODORO_50 -> 50
            FocusTimerMode.COUNTDOWN -> s.customCountdownMinutes
            FocusTimerMode.STOPWATCH -> s.focusDurationMinutes
        }
        val targetSeconds = when (mode) {
            FocusTimerMode.POMODORO_25 -> 25 * 60
            FocusTimerMode.POMODORO_50 -> 50 * 60
            FocusTimerMode.COUNTDOWN -> s.customCountdownMinutes * 60
            FocusTimerMode.STOPWATCH -> 0
        }
        _timerState.update {
            it.copy(
                mode = mode,
                focusDurationMinutes = nextFocusMinutes,
                isBreakPhase = false,
                elapsedSeconds = 0,
                totalTargetSeconds = targetSeconds
            )
        }
    }

    fun selectShortRestMode() {
        if (_timerState.value.isRunning) return
        val s = _timerState.value
        _timerState.update {
            it.copy(
                isBreakPhase = true,
                elapsedSeconds = 0,
                totalTargetSeconds = s.shortBreakMinutes.coerceAtLeast(1) * 60
            )
        }
    }

    fun updateCustomCountdownMinutes(minutes: Int) {
        if (_timerState.value.isRunning) return
        val clamped = minutes.coerceIn(5, 120)
        _timerState.update {
            it.copy(
                mode = FocusTimerMode.COUNTDOWN,
                customCountdownMinutes = clamped,
                focusDurationMinutes = clamped,
                isBreakPhase = false,
                elapsedSeconds = 0,
                totalTargetSeconds = clamped * 60
            )
        }
    }

    fun updatePomodoroConfiguration(
        focusMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int,
        totalRounds: Int,
        sprintTitle: String
    ) {
        val clampedFocus = focusMinutes.coerceIn(5, 120)
        val clampedShort = shortBreakMinutes.coerceIn(1, 30)
        val clampedLong = longBreakMinutes.coerceIn(5, 60)
        val clampedRounds = totalRounds.coerceIn(2, 8)
        val cleanTitle = sprintTitle.trim().ifBlank { "DESIGN ARCHITECTURE • SPRINT 3" }

        _timerState.update { current ->
            val newTarget = if (current.isRunning) {
                current.totalTargetSeconds
            } else if (current.isBreakPhase) {
                clampedShort * 60
            } else {
                clampedFocus * 60
            }
            val nextMode = when (clampedFocus) {
                25 -> FocusTimerMode.POMODORO_25
                50 -> FocusTimerMode.POMODORO_50
                else -> FocusTimerMode.COUNTDOWN
            }
            current.copy(
                mode = if (current.mode == FocusTimerMode.STOPWATCH) FocusTimerMode.STOPWATCH else nextMode,
                customCountdownMinutes = clampedFocus,
                focusDurationMinutes = clampedFocus,
                shortBreakMinutes = clampedShort,
                longBreakMinutes = clampedLong,
                totalRounds = clampedRounds,
                currentRound = current.currentRound.coerceIn(1, clampedRounds),
                activeSprintTitle = cleanTitle.uppercase(Locale.US),
                elapsedSeconds = if (current.isRunning) current.elapsedSeconds else 0,
                totalTargetSeconds = newTarget
            )
        }

        postBanner(
            message = "Updated Pomodoro cycle: ${clampedFocus}m Focus • ${clampedShort}m Short Break • ${clampedLong}m Long Break ($clampedRounds rounds).",
            title = "Pomodoro Cycle Configured",
            badgeText = "${clampedFocus}m / ${clampedShort}m"
        )
    }

    fun selectFocusTag(tag: String) {
        _timerState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                task.copy(isActive = task.title.equals(tag, ignoreCase = true))
            }
            state.copy(
                selectedTag = tag,
                activeSprintTitle = "${tag.uppercase(Locale.US)} • SPRINT ${state.currentRound}",
                tasks = updatedTasks
            )
        }
    }

    fun toggleTaskCompleted(taskId: String) {
        _timerState.update { state ->
            val updated = state.tasks.map { task ->
                if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
            }
            state.copy(tasks = updated)
        }
    }

    fun selectActiveTask(taskId: String) {
        _timerState.update { state ->
            val targetTask = state.tasks.find { it.id == taskId } ?: return@update state
            val updated = state.tasks.map { task ->
                task.copy(isActive = task.id == taskId)
            }
            state.copy(
                selectedTag = targetTask.title,
                activeSprintTitle = "${targetTask.title.uppercase(Locale.US)} • SPRINT ${state.currentRound}",
                tasks = updated
            )
        }
    }

    fun addFocusTask(title: String, durationMinutes: Int) {
        val clean = title.trim()
        if (clean.isBlank()) return
        val clampedMins = durationMinutes.coerceIn(5, 120)
        _timerState.update { state ->
            val newTask = FocusTaskItem(
                id = "task_${System.currentTimeMillis()}",
                title = clean,
                durationMinutes = clampedMins,
                isCompleted = false,
                isActive = state.tasks.none { it.isActive }
            )
            state.copy(tasks = state.tasks + newTask)
        }
    }

    fun toggleStrictFocusLock(enabled: Boolean) {
        _timerState.update { it.copy(strictFocusLock = enabled) }
    }

    fun selectAmbientSound(preset: AmbientSoundPreset) {
        _timerState.update { it.copy(selectedSound = preset) }
        if (_timerState.value.isRunning || _timerState.value.isSoundPlayingPreview) {
            if (preset == AmbientSoundPreset.NONE) {
                soundEngine.stop()
                _timerState.update { it.copy(isSoundPlayingPreview = false) }
            } else {
                soundEngine.startOrSwitch(preset)
            }
        }
    }

    fun cycleAmbientSound(forward: Boolean) {
        val playablePresets = AmbientSoundPreset.entries.filter { it != AmbientSoundPreset.NONE }
        if (playablePresets.isEmpty()) return
        val current = _timerState.value.selectedSound
        val currentIdx = playablePresets.indexOf(current).let { if (it < 0) 0 else it }
        val nextIdx = if (forward) {
            (currentIdx + 1) % playablePresets.size
        } else {
            (currentIdx - 1 + playablePresets.size) % playablePresets.size
        }
        val nextPreset = playablePresets[nextIdx]
        selectAmbientSound(nextPreset)
    }

    fun updateSoundVolume(volume: Float) {
        soundEngine.setVolume(volume)
        _timerState.update { it.copy(soundVolume = volume) }
    }

    fun toggleAmbientPreview() {
        val current = _timerState.value
        val nextPreview = !current.isSoundPlayingPreview
        if (nextPreview) {
            val preset = if (current.selectedSound == AmbientSoundPreset.NONE) {
                AmbientSoundPreset.ZEN_RAIN
            } else {
                current.selectedSound
            }
            _timerState.update { it.copy(selectedSound = preset, isSoundPlayingPreview = true) }
            soundEngine.startOrSwitch(preset)
        } else {
            _timerState.update { it.copy(isSoundPlayingPreview = false) }
            if (!current.isRunning) {
                soundEngine.stop()
            }
        }
    }

    fun toggleStartPauseFocusTimer() {
        val current = _timerState.value
        if (current.isRunning) {
            timerJob?.cancel()
            soundEngine.stop()
            _timerState.update { it.copy(isRunning = false, isSoundPlayingPreview = false) }
        } else {
            _timerState.update { it.copy(isRunning = true) }
            if (current.selectedSound != AmbientSoundPreset.NONE) {
                soundEngine.startOrSwitch(current.selectedSound)
            }
            startTimerTickerLoop()
        }
    }

    private fun startTimerTickerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _timerState.value.isRunning) {
                delay(1000L)
                val s = _timerState.value
                val nextElapsed = s.elapsedSeconds + 1
                if (s.mode != FocusTimerMode.STOPWATCH && nextElapsed >= s.totalTargetSeconds) {
                    _timerState.update { it.copy(elapsedSeconds = s.totalTargetSeconds) }
                    handlePomodoroPhaseCompletedAutoAdvance()
                } else {
                    _timerState.update { it.copy(elapsedSeconds = nextElapsed) }
                }
            }
        }
    }

    private fun handlePomodoroPhaseCompletedAutoAdvance() {
        val s = _timerState.value
        if (!s.isBreakPhase) {
            // Focus round finished -> Save exact studied seconds & auto-advance into Short or Long Break
            val exactStudiedSeconds = s.elapsedSeconds.coerceAtLeast(1)
            persistCompletedFocusSessionAsync(
                selectedTag = s.selectedTag,
                mode = s.mode,
                selectedSound = s.selectedSound,
                exactStudiedSeconds = exactStudiedSeconds
            )

            val isLongBreakNext = s.currentRound >= s.totalRounds
            val breakMinutes = if (isLongBreakNext) s.longBreakMinutes else s.shortBreakMinutes
            val breakTypeLabel = if (isLongBreakNext) "Long Break" else "Short Break"

            _timerState.update {
                it.copy(
                    isRunning = true,
                    isBreakPhase = true,
                    elapsedSeconds = 0,
                    totalTargetSeconds = breakMinutes.coerceAtLeast(1) * 60
                )
            }

            val notifTitle = "Break Time! ($breakMinutes min $breakTypeLabel)"
            val notifBody = "Round ${s.currentRound} of ${s.totalRounds} complete! Take a ${breakMinutes}m restorative break before Round ${if (isLongBreakNext) 1 else s.currentRound + 1}."
            RegainNotificationHelper.sendPomodoroBreakNotification(
                context = appContext,
                title = notifTitle,
                body = notifBody
            )
            postBanner(
                message = notifBody,
                title = notifTitle,
                badgeText = "Round ${s.currentRound}/${s.totalRounds}"
            )
        } else {
            // Break phase finished -> Auto-advance into next Focus Round and notify user
            val nextRound = if (s.currentRound >= s.totalRounds) 1 else s.currentRound + 1
            val focusMins = s.focusDurationMinutes.coerceAtLeast(5)

            _timerState.update {
                it.copy(
                    isRunning = true,
                    isBreakPhase = false,
                    currentRound = nextRound,
                    elapsedSeconds = 0,
                    totalTargetSeconds = focusMins * 60
                )
            }

            val notifTitle = "Break Over • Round $nextRound of ${s.totalRounds} Started"
            val notifBody = "Restorative break complete. Auto-advancing into ${focusMins}m Deep Flow (${s.selectedTag})."
            RegainNotificationHelper.sendPomodoroBreakNotification(
                context = appContext,
                title = notifTitle,
                body = notifBody
            )
            postBanner(
                message = notifBody,
                title = notifTitle,
                badgeText = "Round $nextRound/${s.totalRounds}"
            )
        }
    }

    fun resetFocusTimer() {
        timerJob?.cancel()
        soundEngine.stop()
        _timerState.update {
            val resetTarget = if (it.mode == FocusTimerMode.STOPWATCH) {
                0
            } else {
                it.focusDurationMinutes.coerceAtLeast(5) * 60
            }
            it.copy(
                isRunning = false,
                isBreakPhase = false,
                elapsedSeconds = 0,
                totalTargetSeconds = resetTarget,
                isSoundPlayingPreview = false
            )
        }
    }

    /**
     * Records the EXACT time the user studied (elapsedSeconds), never the target preset duration clicked on.
     */
    fun completeCurrentFocusSession() {
        val s = _timerState.value
        val exactStudiedSeconds = s.elapsedSeconds
        if (exactStudiedSeconds <= 0) {
            postBanner(
                message = "Start the study timer first! Only the exact time you actually studied is recorded (currently 0s).",
                title = "0s Studied So Far",
                badgeText = "Exact Timer",
                isWarning = true
            )
            return
        }

        timerJob?.cancel()
        soundEngine.stop()

        _timerState.update {
            it.copy(
                isRunning = false,
                isBreakPhase = false,
                elapsedSeconds = 0,
                isSoundPlayingPreview = false
            )
        }

        if (!s.isBreakPhase) {
            persistCompletedFocusSessionAsync(
                selectedTag = s.selectedTag,
                mode = s.mode,
                selectedSound = s.selectedSound,
                exactStudiedSeconds = exactStudiedSeconds
            )
        } else {
            postBanner(
                message = "Ended break early. Ready for Round ${(s.currentRound % s.totalRounds) + 1} of ${s.totalRounds}.",
                title = "Break Ended",
                badgeText = "Pomodoro"
            )
        }
    }

    private fun persistCompletedFocusSessionAsync(
        selectedTag: String,
        mode: FocusTimerMode,
        selectedSound: AmbientSoundPreset,
        exactStudiedSeconds: Int
    ) {
        val exactMinutesFloor = exactStudiedSeconds / 60
        val exactFormatted = formatExactDurationSeconds(exactStudiedSeconds)
        val coins = (exactStudiedSeconds / 60).coerceAtLeast(1)

        viewModelScope.launch {
            repository.insertFocusSession(
                FocusSessionEntity(
                    title = "$selectedTag (${mode.label})",
                    tag = selectedTag,
                    mode = mode.name,
                    durationMinutes = exactMinutesFloor,
                    durationSeconds = exactStudiedSeconds,
                    ambientSound = selectedSound.title,
                    coinsEarned = coins
                )
            )
            val progress = uiState.value.userProgress
            val updatedCoins = progress.leafCoins + coins
            val newLevel = (updatedCoins / 120).coerceAtLeast(1)
            val updatedStreak = if (progress.streakDays == 0) 1 else progress.streakDays
            val stageName = when {
                newLevel >= 6 -> "Ancient Bonsai Guardian"
                newLevel >= 4 -> "Blooming Lotus Sage"
                newLevel >= 2 -> "Sprouting Sage Spirit"
                else -> "Mindful Pebble Seed"
            }
            repository.upsertUserProgress(
                progress.copy(
                    leafCoins = updatedCoins,
                    regaLevel = newLevel,
                    streakDays = updatedStreak,
                    regaStageName = stageName,
                    savedMinutesToday = progress.savedMinutesToday + (exactMinutesFloor / 2)
                )
            )
            val focusQuest = uiState.value.dailyQuests.find { it.id == "quest_focus_25" }
            if (focusQuest != null && !focusQuest.isCompleted) {
                repository.updateDailyQuest(
                    focusQuest.copy(currentProgress = 1, isCompleted = true)
                )
            }

            RegainNotificationHelper.sendStudySessionCompletedNotification(
                context = appContext,
                tag = selectedTag,
                exactDurationFormatted = exactFormatted,
                coinsEarned = coins
            )

            postBanner(
                message = "Recorded exact study time: $exactFormatted of $selectedTag! Rega earned +$coins Leaf Coins.",
                title = "Exact Study Time Saved: $exactFormatted",
                badgeText = "+$coins Leaf Coins"
            )
        }
    }

    // --- APP BLOCKER, APP LIMITS, REELS/SHORTS SHIELD & SCHEDULES ---

    fun toggleBlockAppCompletely(app: AppLimitEntity) {
        val nextBlocked = !app.isBlockedCompletely
        if (app.isStrictLocked && app.isBlockedCompletely && !nextBlocked) {
            triggerMindfulIntercept(
                targetName = app.appName,
                targetSubtitle = "Strict Mode App Blocker Active",
                usedMinutes = app.usedTodayMinutes,
                limitMinutes = app.dailyLimitMinutes,
                isStrictLocked = true,
                keepDmsAvailable = false
            )
            return
        }
        RegainOverlayWindowManager.clearGraceWindow(app.packageName)
        viewModelScope.launch {
            val updated = app.copy(
                isBlockedCompletely = nextBlocked,
                isEnabled = if (nextBlocked) true else app.isEnabled
            )
            repository.updateAppLimit(updated)
            val shieldQuest = uiState.value.dailyQuests.find { it.id == "quest_reels_shield" }
            if (shieldQuest != null && !shieldQuest.isCompleted && nextBlocked) {
                repository.updateDailyQuest(
                    shieldQuest.copy(currentProgress = 1, isCompleted = true)
                )
            }
            RegainNotificationHelper.sendAppBlockerUpdatedNotification(
                context = appContext,
                appName = app.appName,
                isBlocked = nextBlocked
            )
            postBanner(
                message = if (nextBlocked) {
                    "${app.appName} is now BLOCKED! Opening ${app.appName} will trigger an animated pop-up shield immediately (0s delay)."
                } else {
                    "Immediate block lifted for ${app.appName}. Daily limit (${app.dailyLimitMinutes}m) remains active."
                },
                title = if (nextBlocked) "Blocked ${app.appName}" else "Unblocked ${app.appName}",
                badgeText = "App Blocker"
            )
        }
    }

    fun addOrBlockSelectedApp(
        selectableApp: SelectableDeviceApp,
        blockImmediately: Boolean,
        dailyLimitMinutes: Int,
        strictMode: Boolean
    ) {
        val cleanName = selectableApp.appName.trim()
        if (cleanName.isBlank()) return
        val pkg = selectableApp.packageName.ifBlank {
            "com.custom.blocked." + cleanName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
        }
        val iconKey = cleanName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").take(14).ifBlank { "app" }
        RegainOverlayWindowManager.clearGraceWindow(pkg)

        viewModelScope.launch {
            val existing = uiState.value.appLimits.find { it.packageName == pkg }
            val entity = AppLimitEntity(
                packageName = pkg,
                appName = cleanName,
                category = selectableApp.category.ifBlank { "Blocked App" },
                iconKey = existing?.iconKey ?: iconKey,
                dailyLimitMinutes = dailyLimitMinutes.coerceIn(5, 180),
                usedTodayMinutes = existing?.usedTodayMinutes ?: 0,
                pickupsToday = existing?.pickupsToday ?: 0,
                blockedCountToday = existing?.blockedCountToday ?: 0,
                isStrictLocked = strictMode,
                isEnabled = true,
                isBlockedCompletely = blockImmediately
            )
            repository.updateAppLimit(entity)

            val shieldQuest = uiState.value.dailyQuests.find { it.id == "quest_reels_shield" }
            if (shieldQuest != null && !shieldQuest.isCompleted) {
                repository.updateDailyQuest(
                    shieldQuest.copy(currentProgress = 1, isCompleted = true)
                )
            }

            RegainNotificationHelper.sendAppBlockerUpdatedNotification(
                context = appContext,
                appName = cleanName,
                isBlocked = blockImmediately || strictMode
            )

            postBanner(
                message = if (blockImmediately || strictMode) {
                    "Added & blocked $cleanName! Regain Zen will intercept it immediately (0s delay) with a pop-up animation."
                } else {
                    "Added $cleanName with a ${entity.dailyLimitMinutes}m daily limit."
                },
                title = "App Added to Blocker",
                badgeText = cleanName
            )
        }
    }

    fun removeAppFromBlocker(app: AppLimitEntity) {
        if (app.isStrictLocked && (app.isBlockedCompletely || app.isEnabled)) {
            triggerMindfulIntercept(
                targetName = app.appName,
                targetSubtitle = "Strict Mode Protected App Rule",
                usedMinutes = app.usedTodayMinutes,
                limitMinutes = app.dailyLimitMinutes,
                isStrictLocked = true,
                keepDmsAvailable = false
            )
            return
        }
        RegainOverlayWindowManager.clearGraceWindow(app.packageName)
        viewModelScope.launch {
            repository.deleteAppLimit(app.packageName)
            postBanner(
                message = "Removed ${app.appName} from your App Blocker list.",
                title = "Removed ${app.appName}",
                badgeText = "App Blocker"
            )
        }
    }

    fun toggleAppLimitEnabled(app: AppLimitEntity) {
        if (app.isStrictLocked && app.isEnabled) {
            triggerMindfulIntercept(
                targetName = app.appName,
                targetSubtitle = "Strict Mode App Limit (${app.dailyLimitMinutes}m/day)",
                usedMinutes = app.usedTodayMinutes,
                limitMinutes = app.dailyLimitMinutes,
                isStrictLocked = true,
                keepDmsAvailable = false
            )
            return
        }
        RegainOverlayWindowManager.clearGraceWindow(app.packageName)
        viewModelScope.launch {
            val nextEnabled = !app.isEnabled
            repository.updateAppLimit(
                app.copy(
                    isEnabled = nextEnabled,
                    isBlockedCompletely = if (!nextEnabled) false else app.isBlockedCompletely
                )
            )
        }
    }

    fun updateAppDailyLimitMinutes(app: AppLimitEntity, newLimitMinutes: Int) {
        viewModelScope.launch {
            repository.updateAppLimit(
                app.copy(dailyLimitMinutes = newLimitMinutes.coerceIn(5, 180))
            )
        }
    }

    fun toggleAppStrictLock(app: AppLimitEntity) {
        val nextStrict = !app.isStrictLocked
        RegainOverlayWindowManager.clearGraceWindow(app.packageName)
        viewModelScope.launch {
            repository.updateAppLimit(
                app.copy(
                    isStrictLocked = nextStrict,
                    isEnabled = if (nextStrict) true else app.isEnabled
                )
            )
            postBanner(
                message = if (nextStrict) {
                    "Strict Mode enabled for ${app.appName}. Opening ${app.appName} will immediately trigger the Strict Pop-Up Shield (0s delay)."
                } else {
                    "Strict Lock relaxed for ${app.appName}."
                },
                title = "Strict Mode Updated",
                badgeText = app.appName
            )
        }
    }

    fun cycleShortFormShieldMode(shield: ShortFormShieldEntity) {
        val nextMode = when (shield.shieldMode) {
            "BLOCK_COMPLETELY" -> "MINDFUL_BREATH_PAUSE"
            "MINDFUL_BREATH_PAUSE" -> "DAILY_CAP_15M"
            else -> "BLOCK_COMPLETELY"
        }
        viewModelScope.launch {
            repository.updateShortFormShield(
                shield.copy(
                    shieldMode = nextMode,
                    dailyCapMinutes = if (nextMode == "DAILY_CAP_15M") 15 else 0,
                    isEnabled = true
                )
            )
        }
    }

    fun toggleShortFormShieldEnabled(shield: ShortFormShieldEntity) {
        viewModelScope.launch {
            val nextEnabled = !shield.isEnabled
            repository.updateShortFormShield(shield.copy(isEnabled = nextEnabled))
            val shieldQuest = uiState.value.dailyQuests.find { it.id == "quest_reels_shield" }
            if (shieldQuest != null && !shieldQuest.isCompleted && nextEnabled) {
                repository.updateDailyQuest(
                    shieldQuest.copy(currentProgress = 1, isCompleted = true)
                )
            }
        }
    }

    fun toggleShortFormKeepDms(shield: ShortFormShieldEntity) {
        viewModelScope.launch {
            repository.updateShortFormShield(
                shield.copy(allowDirectMessages = !shield.allowDirectMessages)
            )
        }
    }

    fun toggleScheduleEnabled(schedule: ScheduleBlockEntity) {
        viewModelScope.launch {
            repository.updateSchedule(schedule.copy(isEnabled = !schedule.isEnabled))
        }
    }

    fun addCustomSchedule(
        title: String,
        timeWindow: String,
        daysSummary: String,
        targetSummary: String,
        isStrict: Boolean
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertSchedule(
                ScheduleBlockEntity(
                    title = title.trim(),
                    timeWindowLabel = timeWindow.ifBlank { "14:00 – 17:00" },
                    daysSummary = daysSummary.ifBlank { "Mon – Fri" },
                    targetSummary = targetSummary.ifBlank { "Reels, Shorts & Social Feeds" },
                    isStrictMode = isStrict,
                    isEnabled = true
                )
            )
            postBanner(
                message = "Added automated block schedule: ${title.trim()}",
                title = "Schedule Created",
                badgeText = timeWindow
            )
        }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch {
            repository.deleteSchedule(id)
        }
    }

    // --- MINDFUL INTERCEPT & BREATHWORK OVERLAY + HEADS-UP POP-UP NOTIFICATION ---

    fun triggerMindfulIntercept(
        targetName: String,
        targetSubtitle: String,
        usedMinutes: Int,
        limitMinutes: Int,
        isStrictLocked: Boolean,
        keepDmsAvailable: Boolean,
        packageName: String = "com.regainzen.preview"
    ) {
        RegainNotificationHelper.sendHeadsUpInterceptNotification(
            context = appContext,
            targetName = targetName,
            reasonSubtitle = targetSubtitle
        )

        val shownSystemFloatingCard = RegainOverlayWindowManager.showFloatingBlockerPopUp(
            context = appContext,
            packageName = packageName,
            targetName = targetName,
            reasonSubtitle = targetSubtitle,
            usedMinutes = usedMinutes,
            limitMinutes = limitMinutes,
            isStrictLocked = isStrictLocked,
            keepDmsAvailable = keepDmsAvailable
        )

        if (!shownSystemFloatingCard) {
            _interceptState.value = MindfulInterceptState(
                isVisible = true,
                targetName = targetName,
                targetSubtitle = targetSubtitle,
                usedMinutes = usedMinutes,
                limitMinutes = limitMinutes,
                isStrictLocked = isStrictLocked,
                keepDmsAvailable = keepDmsAvailable,
                packageName = packageName
            )
        }
    }

    fun chooseMindfulReturnFromIntercept() {
        val target = _interceptState.value.targetName
        _interceptState.update { it.copy(isVisible = false) }
        viewModelScope.launch {
            val progress = uiState.value.userProgress
            repository.upsertUserProgress(
                progress.copy(
                    leafCoins = progress.leafCoins + 15,
                    savedMinutesToday = progress.savedMinutesToday + 8,
                    mindfulBreathsCompleted = progress.mindfulBreathsCompleted + 1
                )
            )
            val breathQuest = uiState.value.dailyQuests.find { it.id == "quest_mindful_breath" }
            if (breathQuest != null && !breathQuest.isCompleted) {
                val nextProg = (breathQuest.currentProgress + 1).coerceAtMost(breathQuest.targetProgress)
                repository.updateDailyQuest(
                    breathQuest.copy(
                        currentProgress = nextProg,
                        isCompleted = nextProg >= breathQuest.targetProgress
                    )
                )
            }
            postBanner(
                message = "Mindful victory over $target! Reclaimed ~8m and earned +15 Leaf Coins.",
                title = "Exited $target Mindfully",
                badgeText = "+15 Leaf Coins"
            )
        }
    }

    fun grantEmergencyFiveMinBypass(minutes: Int = 5) {
        val currentIntercept = _interceptState.value
        val target = currentIntercept.targetName
        val pkg = currentIntercept.packageName
        val validMinutes = minutes.coerceIn(1, 60)
        RegainOverlayWindowManager.grantBreakMinutes(pkg, validMinutes)
        _interceptState.update { it.copy(isVisible = false) }
        viewModelScope.launch {
            val progress = uiState.value.userProgress
            repository.upsertUserProgress(
                progress.copy(mindfulBreathsCompleted = progress.mindfulBreathsCompleted + 1)
            )
            postBanner(
                message = "Mindful pause complete. Granted a ${validMinutes}-minute intentional break for $target.",
                title = "${validMinutes}m Break Active",
                badgeText = target
            )
        }
    }

    fun dismissInterceptModal() {
        _interceptState.update { it.copy(isVisible = false) }
    }

    // --- DAILY QUESTS & REGA COMPANION ---

    fun claimDailyQuestReward(quest: DailyQuestEntity) {
        if (!quest.isCompleted || quest.isClaimed) return
        viewModelScope.launch {
            repository.updateDailyQuest(quest.copy(isClaimed = true))
            val progress = uiState.value.userProgress
            val newCoins = progress.leafCoins + quest.rewardCoins
            repository.upsertUserProgress(
                progress.copy(
                    leafCoins = newCoins,
                    regaLevel = (newCoins / 120).coerceAtLeast(1)
                )
            )
            postBanner(
                message = "Claimed +${quest.rewardCoins} Leaf Coins from '${quest.title}'!",
                title = "Quest Reward Claimed",
                badgeText = "+${quest.rewardCoins} Coins"
            )
        }
    }

    fun updateMindfulDailyGoal(minutes: Int) {
        viewModelScope.launch {
            val progress = uiState.value.userProgress
            repository.upsertUserProgress(
                progress.copy(mindfulGoalMinutes = minutes.coerceIn(45, 300))
            )
        }
    }

    // --- VIRTUAL STUDY TOGETHER ROOMS (OFFLINE SIMULATED COMPANIONS) ---

    fun joinStudyRoom(roomId: String) {
        _studyRoomState.update {
            it.copy(joinedRoomId = roomId)
        }
        val room = _studyRoomState.value.rooms.find { it.id == roomId }
        if (room != null) {
            postBanner(
                message = "Joined '${room.name}' with ${room.activeMembersCount} mindful study companions.",
                title = "Entered Study Sanctuary",
                badgeText = room.name
            )
        }
    }

    fun toggleStudyRoomFocusTicker() {
        val current = _studyRoomState.value
        if (current.isInRoomFocus) {
            roomTickerJob?.cancel()
            soundEngine.stop()
            _studyRoomState.update { it.copy(isInRoomFocus = false) }
            val mins = (current.roomSessionSeconds / 60)
            val exactRoomTime = formatExactDurationSeconds(current.roomSessionSeconds)
            if (mins > 0) {
                viewModelScope.launch {
                    val progress = uiState.value.userProgress
                    val updatedCoins = progress.leafCoins + mins
                    val nextStreak = if (progress.streakDays == 0) 1 else progress.streakDays
                    repository.upsertUserProgress(
                        progress.copy(
                            leafCoins = updatedCoins,
                            streakDays = nextStreak,
                            savedMinutesToday = progress.savedMinutesToday + mins
                        )
                    )
                }
                postBanner(
                    message = "Paused study room session ($exactRoomTime exact focus time recorded).",
                    title = "Study Room Session Saved",
                    badgeText = exactRoomTime
                )
            } else {
                postBanner(
                    message = "Paused study room timer ($exactRoomTime exact time recorded).",
                    title = "Study Room Paused",
                    badgeText = exactRoomTime
                )
            }
        } else {
            _studyRoomState.update { it.copy(isInRoomFocus = true) }
            val room = current.rooms.find { it.id == current.joinedRoomId }
            val sound = room?.ambientRecommendation ?: AmbientSoundPreset.ZEN_RAIN
            soundEngine.startOrSwitch(sound)
            roomTickerJob?.cancel()
            roomTickerJob = viewModelScope.launch {
                while (isActive && _studyRoomState.value.isInRoomFocus) {
                    delay(1000L)
                    _studyRoomState.update {
                        it.copy(roomSessionSeconds = it.roomSessionSeconds + 1)
                    }
                }
            }
        }
    }

    fun sendRoomFocusCheer() {
        _studyRoomState.update { it.copy(cheerCount = it.cheerCount + 1) }
        postBanner(
            message = "Sent a silent Zen Leaf Cheer to your study room companions!",
            title = "Zen Leaf Cheer Sent",
            badgeText = "Study Together"
        )
    }

    // --- 1-PROMPT CAPPED AI PRODUCTIVITY COACH ---

    fun generateDailyAiCoachInsight(preferOnlineGemini: Boolean = false) {
        val state = uiState.value
        val existingToday = state.todayAiInsight
        if (existingToday != null && existingToday.requestsUsedToday >= 1) {
            postBanner(
                message = "Daily 1-Prompt Free Quota (1/1) already used today. Showing cached insight.",
                title = "1/1 Daily AI Quota Capped",
                badgeText = "Free Tier Guard"
            )
            return
        }
        if (_isGeneratingAi.value) return

        viewModelScope.launch {
            _isGeneratingAi.value = true
            try {
                val userKey = state.userProgress.customGeminiApiKey.ifBlank { "" }
                val providerId = state.userProgress.aiProvider
                delay(450L)
                val insight = aiCoach.generateDailyCappedInsight(
                    dateKey = state.todayDateKey,
                    apps = state.appLimits,
                    shields = state.shortFormShields,
                    sessions = state.focusSessions,
                    progress = state.userProgress,
                    preferOnlineGeminiIfKeyPresent = userKey.isNotBlank(),
                    optionalApiKey = userKey,
                    providerId = providerId
                )
                repository.upsertAiInsight(insight)
                postBanner(
                    message = "Generated today's personalized AI Coaching Insight (${insight.sourceMode}).",
                    title = "AI Coach Insight Ready",
                    badgeText = "1/1 Quota Used"
                )
            } finally {
                _isGeneratingAi.value = false
            }
        }
    }

    fun setAiCoachMode(mode: AiCoachMode) {
        _aiCoachState.update { it.copy(mode = mode) }
    }

    fun updateCustomGeminiApiKey(key: String) {
        updateAiProviderAndKey(
            providerId = uiState.value.userProgress.aiProvider,
            key = key
        )
    }

    fun updateAiProviderAndKey(providerId: String, key: String) {
        viewModelScope.launch {
            val cleanKey = key.trim()
            val resolvedProvider = com.example.ai.AiProvider.resolveEffectiveProvider(providerId, cleanKey)
            val progress = uiState.value.userProgress
            repository.upsertUserProgress(
                progress.copy(
                    customGeminiApiKey = cleanKey,
                    aiProvider = resolvedProvider.id
                )
            )
            postBanner(
                message = if (cleanKey.isBlank()) {
                    "Cleared API key. Using 100% Offline Zen Heuristic Engine."
                } else {
                    "Saved ${resolvedProvider.displayName} API key locally on device."
                },
                title = "${resolvedProvider.displayName} Active",
                badgeText = resolvedProvider.badgeLabel
            )
        }
    }

    fun setShowApiKeyDialog(show: Boolean) {
        _aiCoachState.update { it.copy(showApiKeyDialog = show) }
    }

    fun sendAiChatMessage(text: String) {
        if (text.isBlank()) return
        val current = _aiCoachState.value
        if (current.isSendingChat) return

        val progress = uiState.value.userProgress
        val userKey = progress.customGeminiApiKey
        val provider = com.example.ai.AiProvider.resolveEffectiveProvider(progress.aiProvider, userKey)
        if (userKey.isBlank()) {
            postBanner(
                message = "Configure a free Groq, OpenRouter, or Gemini API key in AI Settings to chat with Rega.",
                title = "Free API Key Required",
                badgeText = "Groq / OpenRouter / Gemini",
                isWarning = true
            )
            _aiCoachState.update { it.copy(showApiKeyDialog = true) }
            return
        }

        val newUserMsg = com.example.ai.ChatMessage(role = "user", text = text)
        _aiCoachState.update {
            it.copy(
                chatHistory = it.chatHistory + newUserMsg,
                isSendingChat = true
            )
        }

        viewModelScope.launch {
            try {
                val response = geminiService.chat(
                    apiKey = userKey,
                    history = current.chatHistory,
                    userMessage = text,
                    providerId = provider.id
                )
                if (response != null) {
                    _aiCoachState.update {
                        it.copy(
                            chatHistory = it.chatHistory + com.example.ai.ChatMessage(role = "model", text = response),
                            isSendingChat = false
                        )
                    }
                } else {
                    _aiCoachState.update { it.copy(isSendingChat = false) }
                    postBanner(
                        message = "Could not reach ${provider.displayName}. Verify your API key (${provider.keyPrefixHint}) or switch to Groq/OpenRouter free tier.",
                        title = "${provider.displayName} Error",
                        badgeText = "Check API Key",
                        isWarning = true
                    )
                }
            } catch (_: Exception) {
                _aiCoachState.update { it.copy(isSendingChat = false) }
            }
        }
    }

    fun resetDailyAiQuotaForTesting() {
        val state = uiState.value
        val today = state.todayAiInsight ?: return
        viewModelScope.launch {
            repository.upsertAiInsight(today.copy(requestsUsedToday = 0))
            postBanner(
                message = "Reset today's 1-Prompt AI Quota to 0/1 so you can test fresh insights.",
                title = "AI Quota Reset",
                badgeText = "0/1 Used"
            )
        }
    }

    fun buildLiveSinglePromptPreview(): String {
        val s = uiState.value
        return aiCoach.buildSingleConsolidatedPrompt(
            apps = s.appLimits,
            shields = s.shortFormShields,
            sessions = s.focusSessions,
            progress = s.userProgress
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        roomTickerJob?.cancel()
        soundEngine.stop()
    }
}

private data class DbBundle(
    val apps: List<AppLimitEntity>,
    val shields: List<ShortFormShieldEntity>,
    val sessions: List<FocusSessionEntity>,
    val schedules: List<ScheduleBlockEntity>,
    val quests: List<DailyQuestEntity>
)

private data class LocalUiBundle(
    val tab: RegainTab,
    val timer: ActiveTimerUiState,
    val rooms: StudyRoomUiState,
    val intercept: MindfulInterceptState,
    val generatingAi: Boolean
)

private data class OverlayPermsBundle(
    val banner: String?,
    val popUp: AnimatedPopUpAlert?,
    val perms: BlockerPermissionStatus,
    val showPerms: Boolean,
    val discoverableApps: List<SelectableDeviceApp>,
    val aiCoachState: AiCoachUiState
)

fun defaultStudyRooms(): List<VirtualStudyRoom> = listOf(
    VirtualStudyRoom(
        id = "room_kyoto_zen",
        name = "Silent Kyoto Moss Sanctuary",
        subtitle = "25/5 Pomodoro · Camera Off · Rain & Bonsai",
        ambientRecommendation = AmbientSoundPreset.ZEN_RAIN,
        activeMembersCount = 48,
        peers = listOf(
            VirtualStudyPeer("p1", "Haruto K.", "JP", "Architecture Thesis", 25, 1, "In deep flow (25m block)"),
            VirtualStudyPeer("p2", "Ananya R.", "IN", "UPSC & Polity Notes", 20, 1, "Reviewing flashcards"),
            VirtualStudyPeer("p3", "Elena M.", "DE", "Cognitive Science", 15, 1, "Pomodoro #1"),
            VirtualStudyPeer("p4", "Liam T.", "CA", "Rust & Systems Dev", 10, 1, "Zero social media today")
        )
    ),
    VirtualStudyRoom(
        id = "room_stem_cram",
        name = "Medical & STEM Deep Focus Hall",
        subtitle = "50/10 Deep Work · Brown Noise Blanket",
        ambientRecommendation = AmbientSoundPreset.BROWN_NOISE,
        activeMembersCount = 64,
        peers = listOf(
            VirtualStudyPeer("p5", "Dr. Maya S.", "US", "USMLE Step 2 Prep", 35, 2, "Strict Mode locked"),
            VirtualStudyPeer("p6", "jun_park", "KR", "Linear Algebra & ML", 25, 1, "Solving problem sets"),
            VirtualStudyPeer("p7", "Sofia V.", "BR", "Biochemistry Lab", 15, 1, "Reels Shield active")
        )
    ),
    VirtualStudyRoom(
        id = "room_indie_makers",
        name = "Mindful Builders & Writers Loft",
        subtitle = "Uninterrupted Creation · 40Hz Gamma Wave",
        ambientRecommendation = AmbientSoundPreset.BINAURAL_40HZ,
        activeMembersCount = 31,
        peers = listOf(
            VirtualStudyPeer("p8", "Noah C.", "GB", "Kotlin Compose UI", 30, 2, "Shipping v1.1 offline"),
            VirtualStudyPeer("p9", "Chloe L.", "FR", "Editorial Illustration", 20, 1, "Sketching in silence"),
            VirtualStudyPeer("p10", "Mateo G.", "ES", "Novel Draft Ch. 1", 10, 1, "Starting morning flow")
        )
    )
)

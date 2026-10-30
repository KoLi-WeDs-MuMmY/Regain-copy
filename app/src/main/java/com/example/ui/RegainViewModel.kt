package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.SinglePromptAiCoach
import com.example.audio.AmbientSoundPreset
import com.example.audio.ZenSoundEngine
import com.example.data.AiCoachInsightEntity
import com.example.data.AppLimitEntity
import com.example.data.DailyQuestEntity
import com.example.data.FocusSessionEntity
import com.example.data.RegainDatabase
import com.example.data.RegainRepository
import com.example.data.ScheduleBlockEntity
import com.example.data.ShortFormShieldEntity
import com.example.data.UserProgressEntity
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

enum class RegainTab(val route: String, val label: String) {
    SANCTUARY("sanctuary", "Sanctuary"),
    FOCUS("focus", "Focus"),
    SHIELD("shield", "Shield"),
    ROOMS("rooms", "Study Rooms"),
    AI_COACH("ai_coach", "AI Coach")
}

enum class FocusTimerMode(val label: String, val defaultMinutes: Int) {
    POMODORO_25("Pomodoro 25/5", 25),
    POMODORO_50("Deep 50/10", 50),
    COUNTDOWN("Custom Timer", 30),
    STOPWATCH("Flow Stopwatch", 0)
}

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
    val keepDmsAvailable: Boolean = true
)

data class ActiveTimerUiState(
    val mode: FocusTimerMode = FocusTimerMode.POMODORO_25,
    val customCountdownMinutes: Int = 30,
    val selectedTag: String = "Deep Study",
    val isRunning: Boolean = false,
    val isBreakPhase: Boolean = false,
    val elapsedSeconds: Int = 0,
    val totalTargetSeconds: Int = 25 * 60,
    val strictFocusLock: Boolean = true,
    val selectedSound: AmbientSoundPreset = AmbientSoundPreset.ZEN_RAIN,
    val soundVolume: Float = 0.5f,
    val isSoundPlayingPreview: Boolean = false
)

data class StudyRoomUiState(
    val joinedRoomId: String? = "room_kyoto_zen",
    val isInRoomFocus: Boolean = false,
    val roomSessionSeconds: Int = 0,
    val cheerCount: Int = 0,
    val rooms: List<VirtualStudyRoom> = defaultStudyRooms()
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
    val hasSystemUsagePermission: Boolean = false
) {
    val todayAiInsight: AiCoachInsightEntity?
        get() = aiInsights.firstOrNull { it.dateKey == todayDateKey }

    val dailyAiRequestsRemaining: Int
        get() = if ((todayAiInsight?.requestsUsedToday ?: 0) >= 1) 0 else 1

    val totalScreenTimeMinutesToday: Int
        get() = appLimits.sumOf { it.usedTodayMinutes }

    val totalPickupsToday: Int
        get() = appLimits.sumOf { it.pickupsToday }

    val totalShortFormInterceptsToday: Int
        get() = shortFormShields.sumOf { it.interceptsToday }

    val totalFocusedMinutesToday: Int
        get() = focusSessions.sumOf { it.durationMinutes } +
            (studyRoomState.roomSessionSeconds / 60) +
            (if (timerState.isRunning) timerState.elapsedSeconds / 60 else 0)
}

class RegainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = RegainDatabase.getDatabase(application)
    private val repository = RegainRepository(database.regainDao(), application)
    private val soundEngine = ZenSoundEngine(viewModelScope)
    private val aiCoach = SinglePromptAiCoach()

    private val _currentTab = MutableStateFlow(RegainTab.SANCTUARY)
    private val _timerState = MutableStateFlow(ActiveTimerUiState())
    private val _studyRoomState = MutableStateFlow(StudyRoomUiState())
    private val _interceptState = MutableStateFlow(MindfulInterceptState())
    private val _isGeneratingAi = MutableStateFlow(false)
    private val _bannerMessage = MutableStateFlow<String?>(null)

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

    val uiState: StateFlow<RegainUiState> = combine(
        dbCombinedFlow,
        repository.userProgress,
        repository.aiInsights,
        localUiFlow,
        _bannerMessage
    ) { db, progress, insights, local, banner ->
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
            statusBannerMessage = banner,
            hasSystemUsagePermission = repository.hasUsageStatsPermission()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RegainUiState(todayDateKey = repository.todayDateKey())
    )

    init {
        viewModelScope.launch {
            repository.ensureSeededAndSyncSystemUsage()
        }
    }

    fun selectTab(tab: RegainTab) {
        _currentTab.value = tab
    }

    fun clearBannerMessage() {
        _bannerMessage.value = null
    }

    private fun postBanner(message: String) {
        _bannerMessage.value = message
    }

    // --- FOCUS & POMODORO TIMER ---

    fun selectTimerMode(mode: FocusTimerMode) {
        if (_timerState.value.isRunning) return
        val targetSeconds = when (mode) {
            FocusTimerMode.POMODORO_25 -> 25 * 60
            FocusTimerMode.POMODORO_50 -> 50 * 60
            FocusTimerMode.COUNTDOWN -> _timerState.value.customCountdownMinutes * 60
            FocusTimerMode.STOPWATCH -> 0
        }
        _timerState.update {
            it.copy(
                mode = mode,
                isBreakPhase = false,
                elapsedSeconds = 0,
                totalTargetSeconds = targetSeconds
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
                elapsedSeconds = 0,
                totalTargetSeconds = clamped * 60
            )
        }
    }

    fun selectFocusTag(tag: String) {
        _timerState.update { it.copy(selectedTag = tag) }
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
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (isActive && _timerState.value.isRunning) {
                    delay(1000L)
                    val s = _timerState.value
                    val nextElapsed = s.elapsedSeconds + 1
                    if (s.mode != FocusTimerMode.STOPWATCH && nextElapsed >= s.totalTargetSeconds) {
                        completeCurrentFocusSession()
                        break
                    } else {
                        _timerState.update { it.copy(elapsedSeconds = nextElapsed) }
                    }
                }
            }
        }
    }

    fun resetFocusTimer() {
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
    }

    fun completeCurrentFocusSession() {
        timerJob?.cancel()
        soundEngine.stop()
        val s = _timerState.value
        val focusedMinutes = when (s.mode) {
            FocusTimerMode.STOPWATCH -> (s.elapsedSeconds / 60).coerceAtLeast(10)
            else -> (s.totalTargetSeconds / 60).coerceAtLeast(5)
        }
        val coins = focusedMinutes
        _timerState.update {
            it.copy(
                isRunning = false,
                elapsedSeconds = 0,
                isSoundPlayingPreview = false
            )
        }
        viewModelScope.launch {
            repository.insertFocusSession(
                FocusSessionEntity(
                    title = "${s.selectedTag} (${s.mode.label})",
                    tag = s.selectedTag,
                    mode = s.mode.name,
                    durationMinutes = focusedMinutes,
                    ambientSound = s.selectedSound.title,
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
                    savedMinutesToday = progress.savedMinutesToday + (focusedMinutes / 2)
                )
            )
            val focusQuest = uiState.value.dailyQuests.find { it.id == "quest_focus_25" }
            if (focusQuest != null && !focusQuest.isCompleted) {
                repository.updateDailyQuest(
                    focusQuest.copy(currentProgress = 1, isCompleted = true)
                )
            }
            postBanner("Completed ${focusedMinutes}m of ${s.selectedTag}! Rega earned +$coins Leaf Coins.")
        }
    }

    // --- APP LIMITS, REELS/SHORTS SHIELD & SCHEDULES ---

    fun toggleAppLimitEnabled(app: AppLimitEntity) {
        if (app.isStrictLocked && app.isEnabled) {
            // Trigger Mindful Intercept when trying to disable a strict-locked app
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
        viewModelScope.launch {
            repository.updateAppLimit(app.copy(isEnabled = !app.isEnabled))
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
        viewModelScope.launch {
            repository.updateAppLimit(app.copy(isStrictLocked = !app.isStrictLocked))
            postBanner(
                if (!app.isStrictLocked) "Strict Lock enabled for ${app.appName}."
                else "Strict Lock relaxed for ${app.appName}."
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
            postBanner("Added schedule: ${title.trim()}")
        }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch {
            repository.deleteSchedule(id)
        }
    }

    // --- MINDFUL INTERCEPT & BREATHWORK OVERLAY ---

    fun triggerMindfulIntercept(
        targetName: String,
        targetSubtitle: String,
        usedMinutes: Int,
        limitMinutes: Int,
        isStrictLocked: Boolean,
        keepDmsAvailable: Boolean
    ) {
        _interceptState.value = MindfulInterceptState(
            isVisible = true,
            targetName = targetName,
            targetSubtitle = targetSubtitle,
            usedMinutes = usedMinutes,
            limitMinutes = limitMinutes,
            isStrictLocked = isStrictLocked,
            keepDmsAvailable = keepDmsAvailable
        )
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
            postBanner("Mindful victory over $target! Saved ~8m and earned +15 Leaf Coins.")
        }
    }

    fun grantEmergencyFiveMinBypass() {
        val target = _interceptState.value.targetName
        _interceptState.update { it.copy(isVisible = false) }
        viewModelScope.launch {
            val progress = uiState.value.userProgress
            repository.upsertUserProgress(
                progress.copy(mindfulBreathsCompleted = progress.mindfulBreathsCompleted + 1)
            )
            postBanner("Mindful pause complete. Granted a 5-minute intentional window for $target.")
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
            postBanner("Claimed +${quest.rewardCoins} Leaf Coins from '${quest.title}'!")
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
            postBanner("Joined '${room.name}' with ${room.activeMembersCount} mindful study companions.")
        }
    }

    fun toggleStudyRoomFocusTicker() {
        val current = _studyRoomState.value
        if (current.isInRoomFocus) {
            roomTickerJob?.cancel()
            soundEngine.stop()
            _studyRoomState.update { it.copy(isInRoomFocus = false) }
            val mins = (current.roomSessionSeconds / 60)
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
                postBanner("Paused study room session (+${mins}m added to focus statistics).")
            } else {
                postBanner("Paused study room timer (${current.roomSessionSeconds}s recorded).")
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
        postBanner("Sent a silent Zen Leaf Cheer to your study room companions!")
    }

    // --- 1-PROMPT CAPPED AI PRODUCTIVITY COACH ---

    fun generateDailyAiCoachInsight(preferOnlineGemini: Boolean = false) {
        val state = uiState.value
        val existingToday = state.todayAiInsight
        if (existingToday != null && existingToday.requestsUsedToday >= 1) {
            postBanner("Daily 1-Prompt Free Quota (1/1) already used today. Showing cached insight.")
            return
        }
        if (_isGeneratingAi.value) return

        viewModelScope.launch {
            _isGeneratingAi.value = true
            try {
                delay(450L) // Brief calm animation transition
                val insight = aiCoach.generateDailyCappedInsight(
                    dateKey = state.todayDateKey,
                    apps = state.appLimits,
                    shields = state.shortFormShields,
                    sessions = state.focusSessions,
                    progress = state.userProgress,
                    preferOnlineGeminiIfKeyPresent = preferOnlineGemini
                )
                repository.upsertAiInsight(insight)
                postBanner("Generated today's personalized AI Coaching Insight (1/1 Daily Quota Used).")
            } finally {
                _isGeneratingAi.value = false
            }
        }
    }

    fun resetDailyAiQuotaForTesting() {
        val state = uiState.value
        val today = state.todayAiInsight ?: return
        viewModelScope.launch {
            repository.upsertAiInsight(today.copy(requestsUsedToday = 0))
            postBanner("Reset today's 1-Prompt AI Quota to 0/1 so you can test fresh insights.")
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
            VirtualStudyPeer("p8", "Noah C.", "GB", "Kotlin Compose UI", 30, 2, "Shipping v1.0 offline"),
            VirtualStudyPeer("p9", "Chloe L.", "FR", "Editorial Illustration", 20, 1, "Sketching in silence"),
            VirtualStudyPeer("p10", "Mateo G.", "ES", "Novel Draft Ch. 1", 10, 1, "Starting morning flow")
        )
    )
)

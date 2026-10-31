package com.example

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ai.SinglePromptAiCoach
import com.example.blocker.RegainOverlayWindowManager
import com.example.data.AppLimitEntity
import com.example.data.FocusSessionEntity
import com.example.data.RegainDatabase
import com.example.data.UserProgressEntity
import com.example.ui.ActiveTimerUiState
import com.example.ui.FocusTimerMode
import com.example.ui.RegainUiState
import com.example.ui.WindowWidthSizeClass
import com.example.ui.components.BREAK_DURATION_OPTIONS_MINUTES
import com.example.ui.components.isBreakOptionGlowing
import com.example.ui.computeAdaptiveLayoutInfo
import com.example.ui.formatExactDurationSeconds
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Regain Zen", appName)
    }

    @Test
    fun `adaptive layout info handles compact medium and expanded viewports`() {
        val compact = computeAdaptiveLayoutInfo(maxWidth = 360.dp, maxHeight = 800.dp)
        assertEquals(WindowWidthSizeClass.COMPACT, compact.widthSizeClass)
        assertFalse(compact.useNavigationRail)

        val foldable = computeAdaptiveLayoutInfo(maxWidth = 720.dp, maxHeight = 840.dp)
        assertEquals(WindowWidthSizeClass.MEDIUM, foldable.widthSizeClass)
        assertTrue(foldable.useNavigationRail)
        assertTrue(foldable.isTwoPaneCapable)

        val tablet = computeAdaptiveLayoutInfo(maxWidth = 1080.dp, maxHeight = 800.dp)
        assertEquals(WindowWidthSizeClass.EXPANDED, tablet.widthSizeClass)
        assertEquals(3, tablet.gridColumns)
    }

    @Test
    fun `single prompt ai coach enforces 1 request quota and works offline`() = runBlocking {
        val coach = SinglePromptAiCoach()
        val insight = coach.generateDailyCappedInsight(
            dateKey = "2026-10-30",
            apps = emptyList(),
            shields = emptyList(),
            sessions = emptyList(),
            progress = UserProgressEntity()
        )
        assertEquals(1, insight.requestsUsedToday)
        assertEquals("OFFLINE_ZEN_ENGINE", insight.sourceMode)
        assertTrue(insight.coachHeadline.isNotBlank())
    }

    @Test
    fun `study timer records exact seconds studied instead of preset target duration`() {
        assertEquals("0s", formatExactDurationSeconds(0))
        assertEquals("47s", formatExactDurationSeconds(47))
        assertEquals("2m 15s", formatExactDurationSeconds(135))

        // Even when a 50-minute preset was selected, saving after 95 seconds records 1m 35s
        val recordedSession = FocusSessionEntity(
            title = "Deep Study (Deep 50/10)",
            tag = "Deep Study",
            mode = FocusTimerMode.POMODORO_50.name,
            durationMinutes = 1,
            durationSeconds = 95,
            ambientSound = "Kyoto Temple Rain",
            coinsEarned = 1
        )
        assertEquals("1m 35s", recordedSession.exactDurationFormatted)

        val uiState = RegainUiState(
            focusSessions = listOf(recordedSession),
            timerState = ActiveTimerUiState(
                mode = FocusTimerMode.POMODORO_25,
                totalTargetSeconds = 25 * 60,
                elapsedSeconds = 25,
                isRunning = true
            )
        )
        assertEquals(120, uiState.totalFocusedSecondsToday)
        assertEquals("2m 0s", uiState.exactTotalFocusedFormatted)
    }

    @Test
    fun `background overlay blocker tracks grace windows and synchronous database rules`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = RegainDatabase.getDatabase(context)

        db.insertAppLimits(
            listOf(
                AppLimitEntity(
                    packageName = "com.zhiliaoapp.musically",
                    appName = "TikTok",
                    category = "Short-Form Video",
                    iconKey = "tiktok",
                    dailyLimitMinutes = 20,
                    usedTodayMinutes = 0,
                    pickupsToday = 0,
                    blockedCountToday = 0,
                    isStrictLocked = true,
                    isEnabled = true,
                    isBlockedCompletely = true
                )
            )
        )

        val rule = db.getBlockingRuleForPackageSync("com.zhiliaoapp.musically")
        assertNotNull(rule)
        assertEquals("TikTok", rule?.appName)

        // Verify YouTube with Strict Mode enabled (even if usedTodayMinutes is 0) matches immediately
        db.insertAppLimits(
            listOf(
                AppLimitEntity(
                    packageName = "com.google.android.youtube",
                    appName = "YouTube",
                    category = "Video & Shorts",
                    iconKey = "youtube",
                    dailyLimitMinutes = 45,
                    usedTodayMinutes = 0,
                    pickupsToday = 0,
                    blockedCountToday = 0,
                    isStrictLocked = true,
                    isEnabled = true,
                    isBlockedCompletely = false
                )
            )
        )
        val youtubeStrictRule = db.getBlockingRuleForPackageSync("com.google.android.youtube")
        assertNotNull(youtubeStrictRule)
        assertTrue(youtubeStrictRule?.isStrictLocked == true)

        RegainOverlayWindowManager.clearAllGraceWindows()
        assertFalse(RegainOverlayWindowManager.isPackageInGraceWindow("com.zhiliaoapp.musically"))
        RegainOverlayWindowManager.grantBreakMinutes("com.zhiliaoapp.musically", 15)
        assertTrue(RegainOverlayWindowManager.isPackageInGraceWindow("com.zhiliaoapp.musically"))
        assertTrue(RegainOverlayWindowManager.getRemainingGraceSeconds("com.zhiliaoapp.musically") > 600)
    }

    @Test
    fun `break duration options 5 10 15 30 min only glow when remaining app time is sufficient`() {
        assertEquals(listOf(5, 10, 15, 30), BREAK_DURATION_OPTIONS_MINUTES)

        // Case 1: 12 minutes left (used 18m out of 30m limit) -> 5m and 10m glow; 15m and 30m do NOT glow
        assertTrue(isBreakOptionGlowing(optionMinutes = 5, usedMinutes = 18, limitMinutes = 30))
        assertTrue(isBreakOptionGlowing(optionMinutes = 10, usedMinutes = 18, limitMinutes = 30))
        assertFalse(isBreakOptionGlowing(optionMinutes = 15, usedMinutes = 18, limitMinutes = 30))
        assertFalse(isBreakOptionGlowing(optionMinutes = 30, usedMinutes = 18, limitMinutes = 30))

        // Case 2: 30 minutes left (used 0m out of 30m limit) -> All 4 options (5, 10, 15, 30) glow
        BREAK_DURATION_OPTIONS_MINUTES.forEach { option ->
            assertTrue(isBreakOptionGlowing(optionMinutes = option, usedMinutes = 0, limitMinutes = 30))
        }

        // Case 3: Strict Mode locked -> No options glow even if time remains
        BREAK_DURATION_OPTIONS_MINUTES.forEach { option ->
            assertFalse(
                isBreakOptionGlowing(
                    optionMinutes = option,
                    usedMinutes = 0,
                    limitMinutes = 30,
                    isStrictLocked = true
                )
            )
        }
    }
}

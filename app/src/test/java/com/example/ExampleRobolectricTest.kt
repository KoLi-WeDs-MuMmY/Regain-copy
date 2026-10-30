package com.example

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ai.SinglePromptAiCoach
import com.example.data.UserProgressEntity
import com.example.ui.WindowWidthSizeClass
import com.example.ui.computeAdaptiveLayoutInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}

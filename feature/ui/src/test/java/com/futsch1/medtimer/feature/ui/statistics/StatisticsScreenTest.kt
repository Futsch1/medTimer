package com.futsch1.medtimer.feature.ui.statistics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.statistics.levels.LevelCurve
import com.futsch1.medtimer.feature.ui.statistics.levels.LevelSeries
import com.futsch1.medtimer.feature.ui.statistics.levels.LevelsState
import com.futsch1.medtimer.feature.ui.statistics.levels.UnitConflict
import com.futsch1.medtimer.feature.ui.statistics.levels.UnitCount
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Smoke tests for the stateless [StatisticsScreen] overload. No ViewModel or Hilt required —
 * state is constructed directly via [MutableStatisticsScreenState].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StatisticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `all view chips are displayed`() {
        setScreen(tableState())

        // Chips are icon-only; their localized labels are exposed as icon content descriptions.
        composeTestRule.onNodeWithContentDescription("Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Tabular view").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Calendar").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Levels").assertIsDisplayed()
    }

    @Test
    fun `levels view shows range dropdown and empty hint without configured medicines`() {
        setScreen(MutableStatisticsScreenState().apply {
            activeView = StatisticFragment.LEVELS
            levels = LevelsState(series = persistentListOf(), days = 7, historyDays = 7, projectionDays = 0, ignoredDoses = 0, unitConflicts = persistentListOf(), nowHours = 0.0, midnightLabels = persistentListOf())
        })

        assertTrue(composeTestRule.onAllNodes(hasText("7 days")).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeTestRule.onAllNodes(hasText("half-life", substring = true)).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun `table view has filter field and column headers in the semantic tree`() {
        setScreen(tableState())

        // Use tree presence (not assertIsDisplayed) — fillMaxSize() content may not
        // be in the Robolectric viewport but IS in the semantic tree and that's sufficient.
        assertTrue(composeTestRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeTestRule.onAllNodes(hasText("Name")).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeTestRule.onAllNodes(hasText("Dosage")).fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeTestRule.onAllNodes(hasText("Reminded")).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun `range dropdown hidden when table view is active`() {
        setScreen(tableState())

        assertTrue(composeTestRule.onAllNodes(hasText("7 days")).fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodes(hasText("2 days")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `tapping a chip fires onSelectView with the correct fragment`() {
        var selected: StatisticFragment? = null
        setScreen(tableState(), onSelectView = { selected = it })

        composeTestRule.onNodeWithContentDescription("Calendar").performClick()

        assertEquals(StatisticFragment.CALENDAR, selected)
    }

    @Test
    fun `levels view explains medicines left out because of mixed units`() {
        setScreen(MutableStatisticsScreenState().apply {
            activeView = StatisticFragment.LEVELS
            levels = LevelsState(
                series = persistentListOf(), days = 7, historyDays = 7, projectionDays = 0, ignoredDoses = 0,
                unitConflicts = persistentListOf(UnitConflict("Medicine A", persistentListOf(UnitCount("mg", 2), UnitCount("", 1)))),
                nowHours = 0.0, midnightLabels = persistentListOf(),
            )
        })

        assertTrue(composeTestRule.onAllNodes(hasText("Medicine A", substring = true) and hasText("mg (2), no unit (1)", substring = true))
            .fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeTestRule.onAllNodes(hasText("half-life", substring = true)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `pinching the levels chart shows a custom range until a range is picked`() {
        val curve = LevelCurve((0..48).map { it.toDouble() }.toImmutableList(), (0..48).map { (it % 24).toDouble() }.toImmutableList())
        setScreen(MutableStatisticsScreenState().apply {
            activeView = StatisticFragment.LEVELS
            levels = LevelsState(
                series = persistentListOf(LevelSeries("Medicine A", null, curve, curve)),
                days = 2, historyDays = 7, projectionDays = 1, ignoredDoses = 0, unitConflicts = persistentListOf(), nowHours = 150.0,
                midnightLabels = (0..8).map { "day $it" }.toImmutableList(),
            )
        })
        assertTrue(composeTestRule.onAllNodes(hasText("7 days")).fetchSemanticsNodes().isNotEmpty())

        composeTestRule.onNodeWithTag(StatisticsTestTags.LEVELS_CHART).performTouchInput {
            pinch(center - Offset(20f, 0f), center - Offset(100f, 0f), center + Offset(20f, 0f), center + Offset(100f, 0f))
        }
        composeTestRule.waitForIdle()

        assertTrue(composeTestRule.onAllNodes(hasText("Custom")).fetchSemanticsNodes().isNotEmpty())

        composeTestRule.onNodeWithTag(StatisticsTestTags.RANGE_DROPDOWN).performClick()
        composeTestRule.onNode(hasText("7 days") and hasAnyAncestor(hasTestTag(StatisticsTestTags.RANGE_MENU))).performClick()
        composeTestRule.waitForIdle()

        // Picking the same range again still restores it
        assertTrue(composeTestRule.onAllNodes(hasText("Custom")).fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodes(hasText("7 days")).fetchSemanticsNodes().isNotEmpty())
    }

    private fun setScreen(
        state: StatisticsScreenState,
        onSelectView: (StatisticFragment) -> Unit = {},
    ) {
        composeTestRule.setContent {
            MedTimerTheme {
                // Fixed size gives fillMaxSize() children a defined constraint under Robolectric
                Box(modifier = Modifier.size(400.dp, 800.dp)) {
                    StatisticsScreen(
                        state = state,
                        onSelectView = onSelectView,
                        onSelectRange = {},
                        onSearchQueryChange = {},
                        onEditEvent = {},
                    )
                }
            }
        }
    }

    private fun tableState() = MutableStatisticsScreenState().apply {
        activeView = StatisticFragment.TABLE
    }
}

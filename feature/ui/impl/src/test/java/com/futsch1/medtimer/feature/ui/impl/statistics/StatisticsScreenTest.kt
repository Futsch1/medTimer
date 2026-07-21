package com.futsch1.medtimer.feature.ui.impl.statistics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
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
    fun `all three view chips are displayed`() {
        setScreen(tableState())

        // Chips are icon-only; their localized labels are exposed as icon content descriptions.
        composeTestRule.onNodeWithContentDescription("Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Tabular view").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Calendar").assertIsDisplayed()
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

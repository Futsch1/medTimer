package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.core.ui.list.SelectionListController
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.core.ui.time.SystemDateTimeFormat
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import com.futsch1.medtimer.feature.ui.overview.model.groupOverviewEvents
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OverviewEventListGroupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val nine = LocalDate.of(2026, 5, 28).atTime(9, 0).atZone(ZoneId.systemDefault()).toEpochSecond()
    private val ten = nine + 3_600

    private fun setList(
        events: List<OverviewEvent>,
        groupByReminderTime: Boolean = true,
    ) {
        composeTestRule.setContent {
            MedTimerTheme {
                OverviewEventList(
                    groups = groupOverviewEvents(events, groupByReminderTime = groupByReminderTime),
                    selection = SelectionListController { it.id },
                    onEventClick = {},
                    onEnterSelectionMode = {},
                    onAction = { _, _ -> },
                )
            }
        }
    }

    @Test
    fun `two events of the same time share one header and stack into one card`() {
        setList(
            listOf(
                TestOverviewEvent(id = 1, timestamp = nine, medicineName = "One"),
                TestOverviewEvent(id = 2, timestamp = nine, medicineName = "Two"),
                TestOverviewEvent(id = 3, timestamp = ten, medicineName = "Three"),
            ),
        )

        composeTestRule.onAllNodesWithTag(OverviewTestTags.GROUP_TIME, useUnmergedTree = true).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(OverviewTestTags.EVENT_CARD).assertCountEquals(3)
        assertHeaderTime(groupIndex = 0, timestamp = nine)

        val firstHeader = bounds(OverviewTestTags.group(0))
        val first = bounds(OverviewTestTags.event(0))
        val second = bounds(OverviewTestTags.event(1))
        val third = bounds(OverviewTestTags.event(2))

        assertTrue(firstHeader.bottom <= first.top, "The header sits above the first row of its group")
        assertEquals(first.bottom, second.top, "The rows of one group stack without a gap")
        assertTrue(third.top - second.bottom >= EVENT_SPACING, "The next group keeps the event spacing")
    }

    @Test
    fun `two lone events at different times show no header and keep the event spacing`() {
        setList(
            listOf(
                TestOverviewEvent(id = 1, timestamp = nine, medicineName = "One"),
                TestOverviewEvent(id = 2, timestamp = ten, medicineName = "Two"),
            ),
        )

        composeTestRule.onAllNodesWithTag(OverviewTestTags.GROUP_TIME, useUnmergedTree = true).assertCountEquals(0)
        assertTrue(
            bounds(OverviewTestTags.event(1)).top - bounds(OverviewTestTags.event(0)).bottom >= EVENT_SPACING,
            "Lone events keep the spacing between groups",
        )
    }

    @Test
    fun `a group header keeps its index among all groups when a lone event comes first`() {
        setList(
            listOf(
                TestOverviewEvent(id = 1, timestamp = nine, medicineName = "One"),
                TestOverviewEvent(id = 2, timestamp = ten, medicineName = "Two"),
                TestOverviewEvent(id = 3, timestamp = ten, medicineName = "Three"),
            ),
        )

        composeTestRule.onNodeWithTag(OverviewTestTags.group(0)).assertDoesNotExist()
        assertHeaderTime(groupIndex = 1, timestamp = ten)
        assertTrue(bounds(OverviewTestTags.event(1)).top - bounds(OverviewTestTags.event(0)).bottom >= EVENT_SPACING)
        assertTrue(bounds(OverviewTestTags.group(1)).bottom <= bounds(OverviewTestTags.event(1)).top)
    }

    @Test
    fun `without grouping by reminder time events of the same time show no header`() {
        setList(
            listOf(
                TestOverviewEvent(id = 1, timestamp = nine, medicineName = "One"),
                TestOverviewEvent(id = 2, timestamp = nine, medicineName = "Two"),
            ),
            groupByReminderTime = false,
        )

        composeTestRule.onAllNodesWithTag(OverviewTestTags.EVENT_CARD).assertCountEquals(2)
        composeTestRule.onAllNodesWithTag(OverviewTestTags.GROUP_TIME, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `a lone event renders as one card without a header`() {
        setList(listOf(TestOverviewEvent(id = 1, timestamp = nine, medicineName = "One")))

        composeTestRule.onAllNodesWithTag(OverviewTestTags.GROUP_TIME, useUnmergedTree = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag(OverviewTestTags.EVENT_CARD).assertCountEquals(1)
        assertTrue(bounds(OverviewTestTags.event(0)).top == 0.dp, "The first row needs no spacing above it")
    }

    private fun assertHeaderTime(groupIndex: Int, timestamp: Long) {
        val expected = SystemDateTimeFormat.time(ApplicationProvider.getApplicationContext(), Instant.ofEpochSecond(timestamp))
        composeTestRule.onNode(groupChild(groupIndex, OverviewTestTags.GROUP_TIME)).assertTextEquals(expected)
    }

    private fun groupChild(groupIndex: Int, tag: String) =
        hasTestTag(tag) and hasAnyAncestor(hasTestTag(OverviewTestTags.group(groupIndex)))

    private fun bounds(tag: String) = composeTestRule.onNodeWithTag(tag).getBoundsInRoot()
}

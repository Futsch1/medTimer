package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.core.ui.list.SelectionListController
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OverviewEventListScrollTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val preferencesDataSource: PreferencesDataSource = mock<PreferencesDataSource>().also {
        whenever(it.preferences).thenReturn(MutableStateFlow(UserPreferences.default()))
    }

    private fun takenEvents(count: Int): ImmutableList<OverviewEvent> = (1..count).map { id ->
        PastReminderEvent(
            preferencesDataSource,
            mock<PersistentDataDataSource>(),
            mock<TimeAccess>(),
            ReminderEvent.default().copy(
                reminderEventId = id,
                medicineName = "Medicine A",
                status = ReminderEvent.ReminderStatus.TAKEN,
                remindedTimestamp = Instant.ofEpochSecond(id * 60L),
            )
        )
    }.toPersistentList()

    private var scrolledToTargetCount = 0

    private fun setList(events: () -> ImmutableList<OverviewEvent>, targets: () -> ImmutableSet<Int>) {
        composeTestRule.setContent {
            MedTimerTheme {
                Box(Modifier.height(300.dp)) {
                    OverviewEventList(
                        events = events(),
                        selection = SelectionListController { it.id },
                        onEventClick = {},
                        onEnterSelectionMode = {},
                        onAction = { _, _ -> },
                        scrollTargetEventIds = targets(),
                        onScrolledToTarget = { scrolledToTargetCount++ },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun isComposed(index: Int) =
        composeTestRule.onAllNodesWithTag(OverviewTestTags.event(index)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `scrolls to the first target event and reports it`() {
        setList({ takenEvents(40) }, { persistentSetOf(36, 31) })

        assertTrue(isComposed(30), "Event with id 31 at index 30 should be scrolled into view")
        assertEquals(1, scrolledToTargetCount)
    }

    @Test
    fun `waits for the target events to arrive before scrolling`() {
        var events by mutableStateOf<ImmutableList<OverviewEvent>>(persistentListOf())
        setList({ events }, { persistentSetOf(35) })
        assertEquals(0, scrolledToTargetCount)

        events = takenEvents(40)
        composeTestRule.waitForIdle()

        assertTrue(isComposed(34), "Event with id 35 at index 34 should be scrolled into view")
        assertEquals(1, scrolledToTargetCount)
    }

    @Test
    fun `stays at the top without a target`() {
        setList({ takenEvents(40) }, { persistentSetOf() })

        assertTrue(isComposed(0))
        assertEquals(0, scrolledToTargetCount)
    }
}

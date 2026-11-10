package com.futsch1.medtimer.feature.ui.overview.actions

import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderEvent.ReminderStatus
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.feature.ui.overview.model.OverviewGroup
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupActionsTest {

    private val remindedAt = Instant.ofEpochSecond(32_400)

    private val preferencesDataSource: PreferencesDataSource = mock()
    private val persistentDataDataSource: PersistentDataDataSource = mock()
    private val timeAccess: TimeAccess = mock()

    init {
        whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default()))
        whenever(persistentDataDataSource.getPendingLocationSnoozes()).thenReturn(emptyList())
        // A minute past the reminder time, so that a RAISED event shows as raised and not as pending.
        whenever(timeAccess.now()).thenReturn(remindedAt.plusSeconds(60))
    }

    @Test
    fun `should confirm every event of an unresolved group`() {
        val actions = GroupActions(group(event(1), event(2)))

        assertEquals(listOf(1, 2), actions.confirmAllEvents.map { it.id })
        assertEquals(listOf(Button.TAKEN, Button.SKIPPED, Button.RESCHEDULE), actions.menuButtons)
    }

    @Test
    fun `should confirm only the unresolved events of a part-resolved group`() {
        val actions = GroupActions(group(event(1, ReminderStatus.TAKEN), event(2)))

        assertEquals(listOf(2), actions.confirmAllEvents.map { it.id })
        assertEquals(listOf(Button.SKIPPED), actions.menuButtons)
    }

    @Test
    fun `should drop Skipped from the menu when one member cannot be skipped`() {
        val actions = GroupActions(group(event(1), event(2, cannotBeSkipped = true)))

        assertEquals(listOf(1, 2), actions.confirmAllEvents.map { it.id })
        assertEquals(listOf(Button.TAKEN, Button.RESCHEDULE), actions.menuButtons)
    }

    @Test
    fun `should leave every action to the state button of a one-event group`() {
        val actions = GroupActions(group(event(1)))

        assertTrue(actions.confirmAllEvents.isEmpty())
        assertTrue(actions.menuButtons.isEmpty())
    }

    @Test
    fun `should offer nothing for a fully resolved group`() {
        val actions = GroupActions(group(event(1, ReminderStatus.TAKEN), event(2, ReminderStatus.SKIPPED)))

        assertTrue(actions.confirmAllEvents.isEmpty())
        assertTrue(actions.menuButtons.isEmpty())
    }

    private fun group(vararg events: PastReminderEvent) =
        OverviewGroup(remindedAt.epochSecond, events.toList().toPersistentList())

    private fun event(id: Int, status: ReminderStatus = ReminderStatus.RAISED, cannotBeSkipped: Boolean = false) =
        PastReminderEvent(
            preferencesDataSource,
            persistentDataDataSource,
            timeAccess,
            ReminderEvent.default().copy(
                reminderEventId = id,
                reminderId = id,
                status = status,
                remindedTimestamp = remindedAt,
                cannotBeSkipped = cannotBeSkipped,
            ),
        )
}

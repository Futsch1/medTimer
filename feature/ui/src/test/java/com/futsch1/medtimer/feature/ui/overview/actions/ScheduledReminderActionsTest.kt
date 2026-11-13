package com.futsch1.medtimer.feature.ui.overview.actions

import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.model.SimulatedReminder
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.feature.ui.overview.model.SimulatedReminderEvent
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals

/** Coverage for the buttons of an upcoming dose: Taken at sits next to Taken, also when the dose asks for the dosage. */
class ScheduledReminderActionsTest {

    private val preferencesDataSource: PreferencesDataSource = mock<PreferencesDataSource>().also {
        whenever(it.preferences).thenReturn(MutableStateFlow(UserPreferences.default()))
    }

    @Test
    fun `should offer Taken at for a dose that asks for the dosage`() {
        val actions = actionsFor(Reminder.default().copy(variableAmount = true))

        assertEquals(listOf(Button.TAKEN, Button.TAKEN_AT, Button.SKIPPED, Button.RESCHEDULE), actions.visibleButtons)
    }

    @Test
    fun `should offer neither Taken nor Taken at for a stock reminder`() {
        val actions = actionsFor(Reminder.default().copy(outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE))

        assertEquals(listOf(Button.ACKNOWLEDGED, Button.RESCHEDULE), actions.visibleButtons)
    }

    private fun actionsFor(reminder: Reminder) = ScheduledReminderActions(
        SimulatedReminderEvent(
            preferencesDataSource,
            SimulatedReminder(ScheduledReminder(Medicine.default(), reminder, Instant.ofEpochSecond(3600)), 0.0, 0.0)
        )
    )
}

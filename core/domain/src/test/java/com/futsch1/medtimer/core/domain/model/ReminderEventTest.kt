package com.futsch1.medtimer.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderEventTest {

    @Test
    fun `allStatusValues contains all ReminderStatus entries`() {
        assertEquals(ReminderEvent.ReminderStatus.entries.toList(), ReminderEvent.allStatusValues)
    }

    @Test
    fun `statusValuesWithoutDelete excludes DELETED`() {
        assertEquals(
            listOf(
                ReminderEvent.ReminderStatus.RAISED,
                ReminderEvent.ReminderStatus.TAKEN,
                ReminderEvent.ReminderStatus.SKIPPED,
                ReminderEvent.ReminderStatus.ACKNOWLEDGED
            ),
            ReminderEvent.statusValuesWithoutDelete
        )
    }

    @Test
    fun `statusValuesTakenOrSkipped contains only TAKEN and SKIPPED`() {
        assertEquals(
            listOf(
                ReminderEvent.ReminderStatus.TAKEN,
                ReminderEvent.ReminderStatus.SKIPPED
            ),
            ReminderEvent.statusValuesTakenOrSkipped
        )
    }
}

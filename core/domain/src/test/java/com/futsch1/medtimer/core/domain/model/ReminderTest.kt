package com.futsch1.medtimer.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class ReminderTest {

    private fun reminderOf(
        linkedReminderId: Int = 0,
        intervalStart: Instant = Instant.EPOCH,
        windowedInterval: Boolean = false,
        outOfStockReminderType: Reminder.OutOfStockReminderType = Reminder.OutOfStockReminderType.OFF,
        expirationReminderType: Reminder.ExpirationReminderType = Reminder.ExpirationReminderType.OFF
    ): Reminder = Reminder.default().copy(
        linkedReminderId = linkedReminderId,
        intervalStart = intervalStart,
        windowedInterval = windowedInterval,
        outOfStockReminderType = outOfStockReminderType,
        expirationReminderType = expirationReminderType
    )

    @Test
    fun `reminderType is TIME_BASED when no special conditions are set`() {
        assertEquals(ReminderType.TIME_BASED, reminderOf().reminderType)
    }

    @Test
    fun `reminderType is LINKED when linkedReminderId is set`() {
        assertEquals(ReminderType.LINKED, reminderOf(linkedReminderId = 5).reminderType)
    }

    @Test
    fun `reminderType is CONTINUOUS_INTERVAL when intervalStart is set`() {
        val r = reminderOf(intervalStart = Instant.EPOCH.plusSeconds(3600))
        assertEquals(ReminderType.CONTINUOUS_INTERVAL, r.reminderType)
    }

    @Test
    fun `reminderType is WINDOWED_INTERVAL when windowedInterval is true`() {
        assertEquals(ReminderType.WINDOWED_INTERVAL, reminderOf(windowedInterval = true).reminderType)
    }

    @Test
    fun `reminderType is OUT_OF_STOCK for all out of stock variants`() {
        for (type in Reminder.OutOfStockReminderType.entries) {
            if (type == Reminder.OutOfStockReminderType.OFF) continue
            assertEquals("ONCE variant", ReminderType.OUT_OF_STOCK, reminderOf(outOfStockReminderType = type).reminderType)
        }
    }

    @Test
    fun `reminderType is EXPIRATION_DATE for all expiration variants`() {
        for (type in Reminder.ExpirationReminderType.entries) {
            if (type == Reminder.ExpirationReminderType.OFF) continue
            assertEquals("ONCE variant", ReminderType.EXPIRATION_DATE, reminderOf(expirationReminderType = type).reminderType)
        }
    }

    @Test
    fun `LINKED takes priority over CONTINUOUS_INTERVAL`() {
        val r = reminderOf(linkedReminderId = 5, intervalStart = Instant.EPOCH.plusSeconds(3600))
        assertEquals(ReminderType.LINKED, r.reminderType)
    }

    @Test
    fun `LINKED takes priority over WINDOWED_INTERVAL`() {
        val r = reminderOf(linkedReminderId = 5, windowedInterval = true)
        assertEquals(ReminderType.LINKED, r.reminderType)
    }

    @Test
    fun `LINKED takes priority over OUT_OF_STOCK`() {
        val r = reminderOf(
            linkedReminderId = 5,
            outOfStockReminderType = Reminder.OutOfStockReminderType.ALWAYS
        )
        assertEquals(ReminderType.LINKED, r.reminderType)
    }

    @Test
    fun `LINKED takes priority over EXPIRATION_DATE`() {
        val r = reminderOf(
            linkedReminderId = 5,
            expirationReminderType = Reminder.ExpirationReminderType.ONCE
        )
        assertEquals(ReminderType.LINKED, r.reminderType)
    }

    @Test
    fun `CONTINUOUS_INTERVAL takes priority over OUT_OF_STOCK`() {
        val r = reminderOf(
            intervalStart = Instant.EPOCH.plusSeconds(3600),
            outOfStockReminderType = Reminder.OutOfStockReminderType.ALWAYS
        )
        assertEquals(ReminderType.CONTINUOUS_INTERVAL, r.reminderType)
    }

    @Test
    fun `WINDOWED_INTERVAL takes priority over OUT_OF_STOCK`() {
        val r = reminderOf(
            windowedInterval = true,
            outOfStockReminderType = Reminder.OutOfStockReminderType.ALWAYS
        )
        assertEquals(ReminderType.WINDOWED_INTERVAL, r.reminderType)
    }

    @Test
    fun `OUT_OF_STOCK takes priority over EXPIRATION_DATE`() {
        val r = reminderOf(
            outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE,
            expirationReminderType = Reminder.ExpirationReminderType.ONCE
        )
        assertEquals(ReminderType.OUT_OF_STOCK, r.reminderType)
    }

    @Test
    fun `isInterval is true only for continuous and windowed intervals`() {
        assertEquals(false, reminderOf().isInterval)
        assertEquals(false, reminderOf(linkedReminderId = 5).isInterval)
        assertEquals(true, reminderOf(intervalStart = Instant.EPOCH.plusSeconds(3600)).isInterval)
        assertEquals(true, reminderOf(windowedInterval = true).isInterval)
        assertEquals(false, reminderOf(
            outOfStockReminderType = Reminder.OutOfStockReminderType.ALWAYS
        ).isInterval)
        assertEquals(false, reminderOf(
            expirationReminderType = Reminder.ExpirationReminderType.ONCE
        ).isInterval)
    }

    @Test
    fun `isLinkedOrTimeBased is true only for LINKED and TIME_BASED`() {
        assertEquals(true, reminderOf().isLinkedOrTimeBased)
        assertEquals(true, reminderOf(linkedReminderId = 5).isLinkedOrTimeBased)
        assertEquals(false, reminderOf(intervalStart = Instant.EPOCH.plusSeconds(3600)).isLinkedOrTimeBased)
        assertEquals(false, reminderOf(windowedInterval = true).isLinkedOrTimeBased)
        assertEquals(false, reminderOf(
            outOfStockReminderType = Reminder.OutOfStockReminderType.ALWAYS
        ).isLinkedOrTimeBased)
        assertEquals(false, reminderOf(
            expirationReminderType = Reminder.ExpirationReminderType.ONCE
        ).isLinkedOrTimeBased)
    }

    @Test
    fun `isOutOfStockOrExpirationReminder is true only for OUT_OF_STOCK and EXPIRATION_DATE`() {
        assertEquals(false, reminderOf().isOutOfStockOrExpirationReminder)
        assertEquals(false, reminderOf(linkedReminderId = 5).isOutOfStockOrExpirationReminder)
        assertEquals(false, reminderOf(intervalStart = Instant.EPOCH.plusSeconds(3600)).isOutOfStockOrExpirationReminder)
        assertEquals(false, reminderOf(windowedInterval = true).isOutOfStockOrExpirationReminder)
        assertEquals(true, reminderOf(
            outOfStockReminderType = Reminder.OutOfStockReminderType.ALWAYS
        ).isOutOfStockOrExpirationReminder)
        assertEquals(true, reminderOf(
            expirationReminderType = Reminder.ExpirationReminderType.ONCE
        ).isOutOfStockOrExpirationReminder)
    }

    @Test
    fun `usesTimeInMinutes is true for LINKED, TIME_BASED, EXPIRATION, and DAILY stock`() {
        assertEquals(true, reminderOf().usesTimeInMinutes)
        assertEquals(true, reminderOf(linkedReminderId = 5).usesTimeInMinutes)
        assertEquals(false, reminderOf(intervalStart = Instant.EPOCH.plusSeconds(3600)).usesTimeInMinutes)
        assertEquals(false, reminderOf(windowedInterval = true).usesTimeInMinutes)
        assertEquals(true, reminderOf(
            expirationReminderType = Reminder.ExpirationReminderType.ONCE
        ).usesTimeInMinutes)
        assertEquals(true, reminderOf(
            outOfStockReminderType = Reminder.OutOfStockReminderType.DAILY
        ).usesTimeInMinutes)
        assertEquals(false, reminderOf(
            outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE
        ).usesTimeInMinutes)
    }
}

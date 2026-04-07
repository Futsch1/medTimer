package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ReminderTime
import com.futsch1.medtimer.model.ScheduledReminder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMedicine(medicine: Medicine) {
    private var _medicine: Medicine = medicine
    val reminders: MutableList<Reminder> = mutableListOf()

    val medicine: Medicine get() = _medicine

    var amount: Double
        get() = _medicine.amount
        set(value) {
            _medicine = _medicine.copy(amount = value)
        }

    var expirationDate: LocalDate
        get() = _medicine.expirationDate
        set(value) {
            _medicine = _medicine.copy(expirationDate = value)
        }

    fun toMedicine(): Medicine = _medicine.copy(reminders = reminders.toList())
}

object TestHelper {
    fun buildReminder(
        medicineId: Int,
        reminderId: Int,
        amount: String,
        timeInMinutes: Int,
        daysBetweenReminders: Int
    ): Reminder {
        return Reminder.default().copy(
            id = reminderId,
            medicineRelId = medicineId,
            amount = amount,
            time = ReminderTime(timeInMinutes),
            pauseDays = daysBetweenReminders - 1
        )
    }

    fun buildTestMedicine(medicineId: Int, medicineName: String): TestMedicine {
        return TestMedicine(Medicine.default().copy(id = medicineId, name = medicineName))
    }

    fun on(day: Long): LocalDate {
        return LocalDate.ofEpochDay(day - 1)
    }

    fun on(day: Long, minutes: Long): Instant {
        return Instant.ofEpochSecond((day - 1) * 86400 + minutes * 60)
    }

    fun onTZ(day: Long, minutes: Long, zoneId: String): Instant {
        val localDate = LocalDate.ofEpochDay(day - 1)
        val localTime = LocalTime.of(minutes.toInt() / 60, minutes.toInt() % 60)
        return Instant.ofEpochSecond(
            localDate.atTime(localTime).atZone(ZoneId.of(zoneId)).toEpochSecond()
        )
    }

    fun buildReminderEvent(reminderId: Int, remindedTimestamp: Instant): ReminderEvent {
        return ReminderEvent.default().copy(reminderId = reminderId, remindedTimestamp = remindedTimestamp)
    }

    fun buildReminderEvent(
        reminderId: Int,
        remindedTimestamp: Long,
        reminderEventId: Int
    ): ReminderEvent {
        return ReminderEvent.default()
            .copy(reminderId = reminderId, remindedTimestamp = Instant.ofEpochSecond(remindedTimestamp), reminderEventId = reminderEventId)
    }

    fun assertReminded(
        scheduledReminders: List<ScheduledReminder>,
        timestamp: Instant,
        medicine: Medicine,
        reminder: Reminder
    ) {
        assertRemindedAtIndex(scheduledReminders, timestamp, medicine, reminder, 0)
    }

    fun assertRemindedAtIndex(
        scheduledReminders: List<ScheduledReminder>,
        timestamp: Instant,
        medicine: Medicine,
        reminder: Reminder,
        index: Int
    ) {
        assertTrue(scheduledReminders.size > index)
        assertEquals(timestamp, scheduledReminders[index].timestamp)
        assertEquals(medicine, scheduledReminders[index].medicine)
        assertEquals(reminder, scheduledReminders[index].reminder)
    }
}

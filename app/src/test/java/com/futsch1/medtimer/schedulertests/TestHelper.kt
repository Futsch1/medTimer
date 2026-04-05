package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.model.ReminderEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object TestHelper {
    fun buildReminder(
        medicineId: Int,
        reminderId: Int,
        amount: String,
        timeInMinutes: Int,
        daysBetweenReminders: Int
    ): ReminderEntity {
        val reminder = ReminderEntity(medicineId)
        reminder.reminderId = reminderId
        reminder.amount = amount
        reminder.timeInMinutes = timeInMinutes
        reminder.pauseDays = daysBetweenReminders - 1
        reminder.consecutiveDays = 1
        reminder.createdTimestamp = 0
        reminder.cycleStartDay = 0
        reminder.days = mutableListOf(true, true, true, true, true, true, true)
        return reminder
    }

    fun buildFullMedicine(medicineId: Int, medicineName: String): FullMedicineEntity {
        val medicineWithReminders = FullMedicineEntity()
        medicineWithReminders.medicine = MedicineEntity(medicineName)
        medicineWithReminders.medicine.medicineId = medicineId
        medicineWithReminders.reminders = mutableListOf()
        return medicineWithReminders
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
        medicine: MedicineEntity,
        reminder: ReminderEntity
    ) {
        assertRemindedAtIndex(scheduledReminders, timestamp, medicine, reminder, 0)
    }

    fun assertRemindedAtIndex(
        scheduledReminders: List<ScheduledReminder>,
        timestamp: Instant,
        medicine: MedicineEntity,
        reminder: ReminderEntity,
        index: Int
    ) {
        assertTrue(scheduledReminders.size > index)
        assertEquals(timestamp, scheduledReminders[index].timestamp)
        assertEquals(medicine, scheduledReminders[index].medicine.medicine)
        assertEquals(reminder, scheduledReminders[index].reminder)
    }
}

package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import org.junit.jupiter.api.Assertions
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object TestHelper {
    fun buildReminder(
        medicineId: Int,
        reminderId: Int,
        amount: String,
        timeInMinutes: Int,
        daysBetweenReminders: Int
    ): Reminder {
        val reminder = Reminder(medicineId)
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

    fun buildFullMedicine(medicineId: Int, medicineName: String): FullMedicine {
        val medicineWithReminders = FullMedicine()
        medicineWithReminders.medicine = Medicine(medicineName)
        medicineWithReminders.medicine.medicineId = medicineId
        medicineWithReminders.reminders = ArrayList<Reminder>()
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

    fun buildReminderEvent(reminderId: Int, remindedTimestamp: Long): ReminderEvent {
        val reminderEvent = ReminderEvent()
        reminderEvent.reminderId = reminderId
        reminderEvent.remindedTimestamp = remindedTimestamp
        return reminderEvent
    }

    fun buildReminderEvent(
        reminderId: Int,
        remindedTimestamp: Long,
        reminderEventId: Int
    ): ReminderEvent {
        val reminderEvent = ReminderEvent()
        reminderEvent.reminderId = reminderId
        reminderEvent.remindedTimestamp = remindedTimestamp
        reminderEvent.reminderEventId = reminderEventId
        return reminderEvent
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
        Assertions.assertTrue(scheduledReminders.size > index)
        Assertions.assertEquals(timestamp, scheduledReminders[index].timestamp)
        Assertions.assertEquals(medicine, scheduledReminders[index].medicine.medicine)
        Assertions.assertEquals(reminder, scheduledReminders[index].reminder)
    }
}

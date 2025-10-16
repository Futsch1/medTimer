package com.futsch1.medtimer.reminders.scheduling

import android.content.SharedPreferences
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import java.time.LocalDate
import java.time.ZoneId
import java.util.stream.Collectors

class ReminderScheduler(val timeAccess: TimeAccess, val sharedPreferences: SharedPreferences) {
    fun schedule(fullMedicineWithTagsAndReminders: List<FullMedicine>, reminderEvents: List<ReminderEvent>): List<ScheduledReminder> {
        val reminders = getReminders(fullMedicineWithTagsAndReminders)
        val scheduledReminders = ArrayList<ScheduledReminder>()

        for (reminder in reminders) {
            val scheduling = SchedulingFactory().create(reminder, reminderEvents, this.timeAccess, sharedPreferences)
            val reminderScheduledTime = scheduling.getNextScheduledTime()

            if (reminderScheduledTime != null) {
                scheduledReminders.add(ScheduledReminder(getMedicine(reminder, fullMedicineWithTagsAndReminders), reminder, reminderScheduledTime))
            }
        }

        scheduledReminders.sortWith(Comparator.comparing(ScheduledReminder::timestamp))

        return scheduledReminders
    }

    // Stream.toList() not available in SDK version selected
    private fun getReminders(fullMedicineWithTagsAndReminders: List<FullMedicine>): ArrayList<Reminder> {
        val reminders = ArrayList<Reminder>()
        for (medicineWithReminder in fullMedicineWithTagsAndReminders
        ) {
            reminders.addAll(medicineWithReminder.reminders.stream().filter { r: Reminder? -> r!!.active }.collect(Collectors.toList()))
        }
        return reminders
    }

    private fun getMedicine(reminder: Reminder, fullMedicineWithTagsAndReminders: List<FullMedicine>): FullMedicine {
        val medicineId = reminder.medicineRelId

        val medicineOptional = fullMedicineWithTagsAndReminders.stream().filter { mwr: FullMedicine? -> mwr!!.medicine.medicineId == medicineId }.findFirst()
        if (medicineOptional.isPresent) {
            return medicineOptional.get()
        } else {
            throw NoSuchElementException()
        }
    }

    interface TimeAccess {
        fun systemZone(): ZoneId

        fun localDate(): LocalDate
    }
}

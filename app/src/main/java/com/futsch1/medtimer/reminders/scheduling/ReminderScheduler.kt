package com.futsch1.medtimer.reminders.scheduling

import android.content.SharedPreferences
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.ReminderEvent
import java.time.LocalDate
import java.time.ZoneId

class ReminderScheduler(val timeAccess: TimeAccess, val sharedPreferences: SharedPreferences) {
    fun schedule(fullMedicineWithTagsAndReminders: List<FullMedicine>, reminderEvents: List<ReminderEvent>): List<ScheduledReminder> {
        val scheduledReminders = ArrayList<ScheduledReminder>()

        for (fullMedicine in fullMedicineWithTagsAndReminders) {

            for (reminder in fullMedicine.reminders) {
                if (!reminder.active) {
                    continue
                }

                val scheduling = SchedulingFactory().create(reminder, fullMedicine.medicine, reminderEvents, this.timeAccess, sharedPreferences)
                val reminderScheduledTime = scheduling.getNextScheduledTime()

                if (reminderScheduledTime != null) {
                    scheduledReminders.add(ScheduledReminder(fullMedicine, reminder, reminderScheduledTime))
                }
            }
        }

        scheduledReminders.sortWith(Comparator.comparing(ScheduledReminder::timestamp))

        return scheduledReminders
    }

    interface TimeAccess {
        fun systemZone(): ZoneId

        fun localDate(): LocalDate
    }
}

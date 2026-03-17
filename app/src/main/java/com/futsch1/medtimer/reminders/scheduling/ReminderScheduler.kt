package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess

class ReminderScheduler(val timeAccess: TimeAccess, val dataSource: PreferencesDataSource) {
    fun schedule(fullMedicineWithTagsAndReminders: List<FullMedicine>, reminderEvents: List<ReminderEvent>): List<ScheduledReminder> {
        val scheduledReminders = mutableListOf<ScheduledReminder>()

        for (fullMedicine in fullMedicineWithTagsAndReminders) {

            for (reminder in fullMedicine.reminders) {
                if (!reminder.active) {
                    continue
                }

                val scheduling = SchedulingFactory().create(reminder, fullMedicine.medicine, reminderEvents, timeAccess, dataSource)
                val reminderScheduledTime = scheduling.getNextScheduledTime()

                if (reminderScheduledTime != null) {
                    scheduledReminders.add(ScheduledReminder(fullMedicine, reminder, reminderScheduledTime))
                }
            }
        }

        scheduledReminders.sortWith(Comparator.comparing(ScheduledReminder::timestamp))

        return scheduledReminders
    }

}

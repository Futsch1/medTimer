package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess

class ReminderScheduler(private val timeAccess: TimeAccess, private val dataSource: PreferencesDataSource) {
    fun schedule(fullMedicineWithTagsAndReminders: List<FullMedicineEntity>, reminderEvents: List<ReminderEvent>): List<ScheduledReminder> {
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

package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.reminders.TimeAccess

class ReminderScheduler(private val timeAccess: TimeAccess, private val dataSource: PreferencesDataSource) {
    fun schedule(medicines: List<Medicine>, reminderEvents: List<ReminderEvent>): List<ScheduledReminder> {
        val scheduledReminders = mutableListOf<ScheduledReminder>()

        for (medicine in medicines) {
            for (reminder in medicine.reminders) {
                if (!reminder.active) {
                    continue
                }

                val scheduling = SchedulingFactory().create(reminder, medicine, reminderEvents, timeAccess, dataSource)
                val reminderScheduledTime = scheduling.getNextScheduledTime()

                if (reminderScheduledTime != null) {
                    scheduledReminders.add(ScheduledReminder(medicine, reminder, reminderScheduledTime))
                }
            }
        }

        scheduledReminders.sortWith(Comparator.comparing(ScheduledReminder::timestamp))

        return scheduledReminders
    }

}

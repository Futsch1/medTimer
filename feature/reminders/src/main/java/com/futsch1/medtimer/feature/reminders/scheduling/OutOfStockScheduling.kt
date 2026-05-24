package com.futsch1.medtimer.feature.reminders.scheduling

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.feature.reminders.TimeAccess
import java.time.Instant

class OutOfStockScheduling(
    reminder: Reminder,
    val medicine: Medicine,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : SchedulingBase(reminder, reminderEventList, timeAccess) {
    override fun getNextScheduledTime(): Instant? {
        if (reminder.outOfStockReminderType == Reminder.OutOfStockReminderType.DAILY && medicine.amount <= reminder.outOfStockThreshold) {
            return getNextNotRemindedDay()
        }
        return null
    }
}
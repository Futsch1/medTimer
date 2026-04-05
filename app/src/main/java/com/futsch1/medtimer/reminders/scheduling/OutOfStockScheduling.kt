package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant

class OutOfStockScheduling(
    reminder: ReminderEntity,
    val medicine: MedicineEntity,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : SchedulingBase(reminder, reminderEventList, timeAccess) {
    override fun getNextScheduledTime(): Instant? {
        if (reminder.outOfStockReminderType == ReminderEntity.OutOfStockReminderType.DAILY && medicine.amount <= reminder.outOfStockThreshold) {
            return getNextNotRemindedDay()
        }
        return null
    }
}
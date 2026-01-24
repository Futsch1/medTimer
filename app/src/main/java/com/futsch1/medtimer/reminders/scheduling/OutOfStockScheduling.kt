package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.Instant
import java.time.LocalDate

class OutOfStockScheduling(
    reminder: Reminder,
    val medicine: Medicine,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : SchedulingBase(reminder, reminderEventList, timeAccess) {
    override fun getNextScheduledTime(): Instant? {
        if (reminder.stockReminderType == Reminder.StockReminderType.DAILY && medicine.amount <= reminder.stockThreshold) {
            for (day in 0..6) {
                val day = today() + day
                if (!isRaisedOn(day)) {
                    return localDateToReminderInstant(LocalDate.ofEpochDay(day))
                }
            }
        }
        return null
    }
}
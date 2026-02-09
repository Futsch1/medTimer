package com.futsch1.medtimer.reminders

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler

/**
 * [Worker] responsible for calculating and scheduling the next medicine reminder notification.
 *
 * This worker retrieves all medicines and their history from the [MedicineRepository],
 * determines the next upcoming reminder(s) using [ReminderScheduler], and updates the
 * system alarm via [AlarmProcessor].
 *
 * It respects user preferences regarding notification combining:
 * - If notifications are combined, it schedules an alarm for all reminders occurring at the next time slot.
 * - If not combined, it schedules an alarm only for the single next reminder.
 *
 * If no future reminders are found, any existing next reminder alarm is cancelled.
 */
class ScheduleNextReminderNotificationWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {

        ScheduleNextReminderNotificationProcessor(context).scheduleNextReminder()

        return Result.success()
    }
}

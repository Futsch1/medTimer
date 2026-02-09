package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData

/**
 * [Worker] implementation responsible for processing and displaying medicine reminder notifications.
 *
 * This worker:
 * 1. Retrieves reminder data from the worker input.
 * 2. Filters out reminders that have already been processed.
 * 3. Handles medicines marked as "automatically taken" by updating their status without showing a notification.
 * 4. Displays system notifications for the remaining reminders.
 * 5. Schedules repeating notifications if configured in the app preferences.
 * 6. Triggers the scheduling of the next upcoming medication reminder.
 */
class ReminderNotificationWorker(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private lateinit var medicineRepository: MedicineRepository

    override fun doWork(): Result {
        val inputData = getInputData()

        medicineRepository = MedicineRepository(applicationContext as Application)

        val reminderNotificationData = ReminderNotificationData.fromInputData(inputData)
        val r = ReminderNotificationProcessor(reminderNotificationData, applicationContext, medicineRepository).processReminders()

        // Reminder shown, now schedule next reminder
        ReminderWorkerReceiver.requestScheduleNextNotification(context)

        return if (r) {
            Result.success()
        } else {
            Result.failure()
        }
    }
}


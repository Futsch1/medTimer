package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData.Companion.fromInputData

/**
 * Worker that schedules a repeat of the current reminder.
 */
class RepeatReminderWork(context: Context, workerParams: WorkerParameters) : SnoozeWork(context, workerParams) {
    override fun doWork(): Result {
        val inputData = getInputData()

        val reminderNotificationData = fromInputData(inputData)
        val repeatTimeSeconds = inputData.getInt(ActivityCodes.EXTRA_REPEAT_TIME_SECONDS, 0)
        reminderNotificationData.delayBy(repeatTimeSeconds)

        enqueueNotification(reminderNotificationData)

        for (reminderEventId in reminderNotificationData.reminderEventIds) {
            decreaseRemainingRepeats(reminderEventId)
        }

        return Result.success()
    }

    private fun decreaseRemainingRepeats(reminderEventId: Int) {
        val medicineRepository = MedicineRepository(applicationContext as Application)
        val reminderEvent = medicineRepository.getReminderEvent(reminderEventId)
        if (reminderEvent != null) {
            reminderEvent.remainingRepeats = reminderEvent.remainingRepeats - 1
            medicineRepository.updateReminderEvent(reminderEvent)
        }
    }
}

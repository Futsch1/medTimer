package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData.Companion.fromInputData
import java.time.Instant

/**
 * [RepeatWorker] is a background worker responsible for rescheduling a reminder notification
 * after a specified delay.
 *
 * It retrieves the reminder data and repeat interval from the [inputData], calculates the
 * next trigger time, and schedules a new alarm using [AlarmProcessor]. Additionally, it
 * decrements the remaining repeat count for each associated reminder event in the database.
 *
 * Input data expectations:
 * - [ActivityCodes.EXTRA_REPEAT_TIME_SECONDS]: The delay in seconds before the next reminder.
 * - Reminder data serialized into the [inputData] (handled by [fromInputData]).
 */
class RepeatWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    val alarmSetter = AlarmProcessor(context)

    override fun doWork(): Result {
        val reminderNotificationData = fromInputData(inputData)
        val repeatTimeSeconds = inputData.getInt(ActivityCodes.EXTRA_REPEAT_TIME_SECONDS, 0)
        reminderNotificationData.remindInstant = Instant.now().plusSeconds(repeatTimeSeconds.toLong())

        Log.d(LogTags.REMINDER, "Repeating reminder $reminderNotificationData")
        alarmSetter.setAlarmForReminderNotification(reminderNotificationData, inputData)

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

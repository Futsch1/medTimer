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
 * Worker that schedules a repeat of the current reminder.
 */
class RepeatWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    val alarmSetter = SetAlarmForReminderNotification(context)

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

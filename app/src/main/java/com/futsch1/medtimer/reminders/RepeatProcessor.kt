package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import javax.inject.Inject
import kotlin.time.Duration

/**
 * [RepeatProcessor] is responsible for rescheduling a reminder notification
 * after a specified delay.
 *
 *
 */
class RepeatProcessor @Inject constructor(val reminderContext: ReminderContext, val alarmProcessor: AlarmProcessor) {
    suspend fun processRepeat(reminderNotificationData: ReminderNotificationData, repeatTimeSeconds: Duration) {
        reminderNotificationData.remindInstant = reminderContext.timeAccess.now().plusSeconds(repeatTimeSeconds.inWholeSeconds)

        Log.d(LogTags.REMINDER, "Repeating reminder $reminderNotificationData")
        alarmProcessor.setAlarmForReminderNotification(reminderNotificationData)

        for (reminderEventId in reminderNotificationData.reminderEventIds) {
            decreaseRemainingRepeats(reminderEventId)
        }
    }

    private suspend fun decreaseRemainingRepeats(reminderEventId: Int) {
        val reminderEvent = reminderContext.medicineRepository.getReminderEvent(reminderEventId)
        if (reminderEvent != null) {
            reminderEvent.remainingRepeats = reminderEvent.remainingRepeats - 1
            reminderContext.medicineRepository.updateReminderEvent(reminderEvent)
        }
    }
}

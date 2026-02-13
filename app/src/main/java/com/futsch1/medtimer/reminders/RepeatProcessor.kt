package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData

/**
 * [RepeatProcessor] is responsible for rescheduling a reminder notification
 * after a specified delay.
 *
 *
 */
class RepeatProcessor(val reminderContext: ReminderContext) {
    val alarmSetter = AlarmProcessor(reminderContext)

    fun processRepeat(reminderNotificationData: ReminderNotificationData, repeatTimeSeconds: Int) {
        reminderNotificationData.remindInstant = reminderContext.timeAccess.now().plusSeconds(repeatTimeSeconds.toLong())

        Log.d(LogTags.REMINDER, "Repeating reminder $reminderNotificationData")
        alarmSetter.setAlarmForReminderNotification(reminderNotificationData)

        for (reminderEventId in reminderNotificationData.reminderEventIds) {
            decreaseRemainingRepeats(reminderEventId)
        }
    }

    private fun decreaseRemainingRepeats(reminderEventId: Int) {
        val reminderEvent = reminderContext.medicineRepository.getReminderEvent(reminderEventId)
        if (reminderEvent != null) {
            reminderEvent.remainingRepeats = reminderEvent.remainingRepeats - 1
            reminderContext.medicineRepository.updateReminderEvent(reminderEvent)
        }
    }
}

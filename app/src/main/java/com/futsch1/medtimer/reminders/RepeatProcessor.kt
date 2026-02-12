package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import java.time.Instant

/**
 * [RepeatProcessor] is responsible for rescheduling a reminder notification
 * after a specified delay.
 *
 *
 */
class RepeatProcessor(context: Context) {
    private val medicineRepository = MedicineRepository(context.applicationContext as Application?)
    val alarmSetter = AlarmProcessor(context)

    fun processRepeat(reminderNotificationData: ReminderNotificationData, repeatTimeSeconds: Int) {
        reminderNotificationData.remindInstant = Instant.now().plusSeconds(repeatTimeSeconds.toLong())

        Log.d(LogTags.REMINDER, "Repeating reminder $reminderNotificationData")
        alarmSetter.setAlarmForReminderNotification(reminderNotificationData)

        for (reminderEventId in reminderNotificationData.reminderEventIds) {
            decreaseRemainingRepeats(reminderEventId)
        }
    }

    private fun decreaseRemainingRepeats(reminderEventId: Int) {
        val reminderEvent = medicineRepository.getReminderEvent(reminderEventId)
        if (reminderEvent != null) {
            reminderEvent.remainingRepeats = reminderEvent.remainingRepeats - 1
            medicineRepository.updateReminderEvent(reminderEvent)
        }
    }
}

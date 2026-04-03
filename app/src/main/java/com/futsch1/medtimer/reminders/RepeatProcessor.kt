package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import javax.inject.Inject
import kotlin.time.Duration

class RepeatProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val medicineRepository: MedicineRepository,
    private val timeAccess: TimeAccess
) {
    suspend fun processRepeat(reminderNotificationData: ReminderNotificationData, repeatDelay: Duration) {
        reminderNotificationData.remindInstant = timeAccess.now().plusSeconds(repeatDelay.inWholeSeconds)

        Log.d(LogTags.REMINDER, "Repeating reminder $reminderNotificationData")
        alarmProcessor.setAlarmForReminderNotification(reminderNotificationData)

        for (reminderEventId in reminderNotificationData.reminderEventIds) {
            decreaseRemainingRepeats(reminderEventId)
        }
    }

    private suspend fun decreaseRemainingRepeats(reminderEventId: Int) {
        val reminderEvent = medicineRepository.getReminderEvent(reminderEventId)
        if (reminderEvent != null) {
            reminderEvent.remainingRepeats = reminderEvent.remainingRepeats - 1
            medicineRepository.updateReminderEvent(reminderEvent)
        }
    }
}

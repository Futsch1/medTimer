package com.futsch1.medtimer.feature.reminders.impl

import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import javax.inject.Inject
import kotlin.time.Duration

class RepeatProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val reminderEventRepository: ReminderEventRepository,
    private val timeAccess: TimeAccess
) {
    suspend fun processRepeat(reminderNotificationData: ReminderNotificationData, repeatDelay: Duration) {
        reminderNotificationData.remindInstant = timeAccess.now().plusSeconds(repeatDelay.inWholeSeconds)

        Log.d(LogTags.REMINDER, "Repeating reminder $reminderNotificationData")
        alarmProcessor.setSecondaryAlarm(reminderNotificationData)

        for (reminderEventId in reminderNotificationData.reminderEventIds) {
            reminderEventRepository.decreaseRepeats(reminderEventId)
        }
    }
}

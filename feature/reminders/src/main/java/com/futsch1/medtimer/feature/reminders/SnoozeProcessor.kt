package com.futsch1.medtimer.feature.reminders

import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.location.GeofenceRegistrar
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.feature.reminders.notificationData.toPendingSnooze
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Handles the snooze functionality for reminders.
 *
 * This class calculates a new reminder time based on the provided snooze duration,
 * cancels any existing notifications or pending alarms for the reminder, and

 * schedules a new alarm for the future.
 */
open class SnoozeProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val notificationProcessor: NotificationProcessor,
    private val persistentDataDataSource: PersistentDataDataSource,
    private val geofenceRegistrar: GeofenceRegistrar
) {

    fun processSnooze(reminderNotificationData: ReminderNotificationData, snoozeTime: Duration) {
        reminderNotificationData.remindInstant = Instant.now().plusSeconds(snoozeTime.inWholeSeconds)
        Log.d(LogTags.REMINDER, "Snoozing reminder: $reminderNotificationData")

        alarmProcessor.setSecondaryAlarm(reminderNotificationData)

        notificationProcessor.cancelNotification(reminderNotificationData.notificationId)
    }

    fun processLocationSnooze(reminderNotificationData: ReminderNotificationData) {
        Log.d(LogTags.REMINDER, "Snoozing reminder until home: $reminderNotificationData")

        alarmProcessor.cancelPendingReminderNotifications(reminderNotificationData)
        persistentDataDataSource.addPendingLocationSnooze(reminderNotificationData.toPendingSnooze())
        geofenceRegistrar.registerHomeGeofence()
        notificationProcessor.cancelNotification(reminderNotificationData.notificationId)
    }
}

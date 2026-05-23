package com.futsch1.medtimer.feature.reminders

import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.feature.reminders.location.GeofenceRegistrar
import com.futsch1.medtimer.feature.reminders.notificationData.toReminderNotificationData
import javax.inject.Inject

class LocationSnoozeProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val persistentDataDataSource: PersistentDataDataSource,
    private val geofenceRegistrar: GeofenceRegistrar
) {
    suspend fun processLocationSnooze() {
        val pending = persistentDataDataSource.getPendingLocationSnoozes()
        Log.d(LogTags.REMINDER, "In home location, restoring ${pending.size} snoozed reminders")
        for (data in pending) {
            alarmProcessor.setAlarmForReminderNotification(data.toReminderNotificationData())
        }
        persistentDataDataSource.clearAllPendingLocationSnoozes()
        geofenceRegistrar.unregisterHomeGeofence()
    }
}
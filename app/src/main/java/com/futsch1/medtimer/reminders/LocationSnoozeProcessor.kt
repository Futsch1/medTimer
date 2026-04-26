package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.location.GeofenceRegistrar
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import javax.inject.Inject

class LocationSnoozeProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val persistentDataDataSource: PersistentDataDataSource,
    private val geofenceRegistrar: GeofenceRegistrar
) {
    fun processLocationSnooze() {
        val pending = persistentDataDataSource.getPendingLocationSnoozes()
        Log.d(LogTags.REMINDER, "In home location, restoring ${pending.size} snoozed reminders")
        for (data in pending) {
            alarmProcessor.setAlarmForReminderNotification(data)
        }
        persistentDataDataSource.clearAllPendingLocationSnoozes()
        geofenceRegistrar.unregisterHomeGeofence()
    }
}
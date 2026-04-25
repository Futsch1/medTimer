package com.futsch1.medtimer.reminders

import com.futsch1.medtimer.location.GeofenceRegistrar
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import java.time.Instant
import javax.inject.Inject

class LocationSnoozeProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val persistentDataDataSource: PersistentDataDataSource,
    private val geofenceRegistrar: GeofenceRegistrar
) {
    fun processLocationSnooze() {
        val pending = persistentDataDataSource.getPendingLocationSnoozes()
        for (data in pending) {
            data.remindInstant = Instant.now()
            alarmProcessor.setAlarmForReminderNotification(data)
        }
        persistentDataDataSource.clearAllPendingLocationSnoozes()
        geofenceRegistrar.unregisterHomeGeofence()
    }
}
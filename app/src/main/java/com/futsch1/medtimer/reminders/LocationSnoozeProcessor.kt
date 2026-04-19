package com.futsch1.medtimer.reminders

import com.futsch1.medtimer.location.GeofenceRegistrar
import com.futsch1.medtimer.preferences.HomeLocationDataSource
import java.time.Instant
import javax.inject.Inject

class LocationSnoozeProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val homeLocationDataSource: HomeLocationDataSource,
    private val geofenceRegistrar: GeofenceRegistrar
) {
    fun processLocationSnooze() {
        val pending = homeLocationDataSource.getPendingLocationSnoozes()
        for (data in pending) {
            data.remindInstant = Instant.now()
            alarmProcessor.setAlarmForReminderNotification(data)
        }
        homeLocationDataSource.clearAllPendingLocationSnoozes()
        geofenceRegistrar.unregisterHomeGeofence()
    }
}
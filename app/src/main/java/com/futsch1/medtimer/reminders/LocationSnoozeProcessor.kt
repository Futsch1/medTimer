package com.futsch1.medtimer.reminders

import com.futsch1.medtimer.location.GeofenceRegistrar
import com.futsch1.medtimer.preferences.HomeLocationStore
import java.time.Instant
import javax.inject.Inject

class LocationSnoozeProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val homeLocationStore: HomeLocationStore,
    private val geofenceRegistrar: GeofenceRegistrar
) {
    fun processLocationSnooze() {
        val pending = homeLocationStore.getPendingLocationSnoozes()
        for (data in pending) {
            data.remindInstant = Instant.now()
            alarmProcessor.setAlarmForReminderNotification(data)
        }
        homeLocationStore.clearAllPendingLocationSnoozes()
        geofenceRegistrar.unregisterHomeGeofence()
    }
}
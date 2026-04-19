package com.futsch1.medtimer.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationSnoozeProcessor: LocationSnoozeProcessor

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        handleGeofencingEvent(event)
    }

    internal fun handleGeofencingEvent(event: GeofencingEvent) {
        if (event.hasError()) {
            Log.e(LogTags.REMINDER, "Geofence error code: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            locationSnoozeProcessor.processLocationSnooze()
        }
    }
}

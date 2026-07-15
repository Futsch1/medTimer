package com.futsch1.medtimer.core.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.ProcessorCode
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        handleGeofencingEvent(context, event)
    }

    fun handleGeofencingEvent(context: Context, event: GeofencingEvent) {
        if (event.hasError()) {
            Log.e(LogTags.LOCATION, "Geofence error code: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            val forward = Intent(ProcessorCode.GeofenceEntered.action)
                .setClassName(context.packageName, REMINDER_RECEIVER_CLASS)
            context.sendBroadcast(forward, REMINDER_RECEIVER_PERMISSION)
        }
    }

    companion object {
        private const val REMINDER_RECEIVER_CLASS =
            "com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver"
        private const val REMINDER_RECEIVER_PERMISSION =
            "com.futsch1.medtimer.NOTIFICATION_PROCESSED"
    }
}

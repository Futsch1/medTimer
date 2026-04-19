package com.futsch1.medtimer.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.futsch1.medtimer.LogTags
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GeofenceRegistrar @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val geofencingClient: GeofencingClient,
    private val homeLocationStore: HomeLocationStore,
    private val googleApiAvailability: GoogleApiAvailability
) {
    fun isLocationServiceAvailable(): Boolean =
        googleApiAvailability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    fun registerHomeGeofence(): Boolean {
        if (!isLocationServiceAvailable()) {
            Log.w(LogTags.REMINDER, "Google Play Services unavailable, cannot register home geofence")
            return false
        }
        val homeLocation = homeLocationStore.getHomeLocation() ?: run {
            Log.w(LogTags.REMINDER, "No home location saved, cannot register geofence")
            return false
        }
        if (!hasRequiredPermissions()) {
            Log.w(LogTags.REMINDER, "Location permissions not granted, cannot register geofence")
            return false
        }

        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(homeLocation.latitude, homeLocation.longitude, homeLocation.radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        return try {
            geofencingClient.addGeofences(request, buildGeofencePendingIntent())
                .addOnSuccessListener { Log.i(LogTags.REMINDER, "Home geofence registered successfully") }
                .addOnFailureListener { Log.e(LogTags.REMINDER, "Failed to add home geofence: ${it.message}") }
            true
        } catch (e: SecurityException) {
            Log.e(LogTags.REMINDER, "Security exception registering home geofence: ${e.message}")
            false
        }
    }

    fun unregisterHomeGeofence() {
        geofencingClient.removeGeofences(listOf(GEOFENCE_ID))
            .addOnSuccessListener { Log.i(LogTags.REMINDER, "Home geofence removed") }
            .addOnFailureListener { Log.w(LogTags.REMINDER, "Failed to remove home geofence: ${it.message}") }
    }

    private fun buildGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return fineLocation && backgroundLocation
    }

    companion object {
        const val GEOFENCE_ID = "medtimer_home"
    }
}

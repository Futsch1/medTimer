package com.futsch1.medtimer.core.location

interface GeofenceRegistrar {
    val isSupported: Boolean
    fun isLocationServiceAvailable(): Boolean
    fun registerHomeGeofence(onSuccess: (() -> Unit)? = null, onFailure: (() -> Unit)? = null): Boolean
    fun unregisterHomeGeofence()
    fun hasRequiredPermissions(): Boolean

    companion object {
        const val GEOFENCE_ID = "medtimer_home"
    }
}

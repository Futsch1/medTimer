package com.futsch1.medtimer.location

import javax.inject.Inject

class NoOpGeofenceRegistrar @Inject constructor() : GeofenceRegistrar {
    override val isSupported: Boolean = false
    override fun isLocationServiceAvailable() = false
    override fun registerHomeGeofence(onSuccess: (() -> Unit)?, onFailure: (() -> Unit)?) = false
    override fun unregisterHomeGeofence() {
        // Intentionally empty
    }
    override fun hasRequiredPermissions() = false
}

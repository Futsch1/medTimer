package com.futsch1.medtimer.location

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import javax.inject.Inject

class GmsLocationProvider @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationProvider {

    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(onSuccess: (Location?) -> Unit, onFailure: () -> Unit): () -> Unit {
        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { onSuccess(it) }
            .addOnFailureListener { onFailure() }
        return { cts.cancel() }
    }
}

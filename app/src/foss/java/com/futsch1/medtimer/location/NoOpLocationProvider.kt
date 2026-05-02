package com.futsch1.medtimer.location

import android.location.Location
import javax.inject.Inject

class NoOpLocationProvider @Inject constructor() : LocationProvider {
    override fun getCurrentLocation(onSuccess: (Location?) -> Unit, onFailure: () -> Unit): () -> Unit {
        onFailure()
        return {}
    }
}

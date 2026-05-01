package com.futsch1.medtimer.location

import android.location.Location

interface LocationProvider {
    fun getCurrentLocation(onSuccess: (Location?) -> Unit, onFailure: () -> Unit): () -> Unit
}

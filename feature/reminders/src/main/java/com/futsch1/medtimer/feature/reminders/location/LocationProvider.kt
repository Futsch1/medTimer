package com.futsch1.medtimer.feature.reminders.location

import android.location.Location

fun interface LocationProvider {
    fun getCurrentLocation(onSuccess: (Location?) -> Unit, onFailure: () -> Unit): () -> Unit
}

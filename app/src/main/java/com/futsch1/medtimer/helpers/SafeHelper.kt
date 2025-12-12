package com.futsch1.medtimer.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

fun safeStartActivity(context: Context?, intent: Intent) {
    try {
        context?.startActivity(intent)
    } catch (_: IllegalStateException) {
        // Intentionally empty
    } catch (_: ActivityNotFoundException) {
        // Intentionally empty
    }
}
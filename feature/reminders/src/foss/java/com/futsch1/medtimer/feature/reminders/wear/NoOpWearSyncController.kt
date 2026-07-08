package com.futsch1.medtimer.feature.reminders.wear

import javax.inject.Inject

/** The `foss` flavor stays free of Google Play Services Wearable, so there is no watch sync. */
class NoOpWearSyncController @Inject constructor() : WearSyncController {
    override fun start() {
        // Intentionally empty.
    }
}

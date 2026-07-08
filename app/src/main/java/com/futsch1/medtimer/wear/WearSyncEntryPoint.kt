package com.futsch1.medtimer.wear

import com.futsch1.medtimer.feature.reminders.wear.WearSyncController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * [WearSyncController] is bound differently per flavor (real Play Services impl in `full`, no-op
 * in `foss`), and [com.futsch1.medtimer.MedTimerApplication] needs it before any
 * Activity/Fragment/Service exists to inject into - an entry point is the standard way to pull a
 * Hilt-managed singleton straight out of the application-level component.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WearSyncEntryPoint {
    fun wearSyncController(): WearSyncController
}

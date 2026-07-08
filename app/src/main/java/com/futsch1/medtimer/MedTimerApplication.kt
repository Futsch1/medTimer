package com.futsch1.medtimer

import android.app.Application
import com.futsch1.medtimer.wear.WearSyncEntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MedTimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EntryPoints.get(this, WearSyncEntryPoint::class.java).wearSyncController().start()
    }
}

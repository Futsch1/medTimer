package com.futsch1.medtimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.feature.reminders.command.ReminderCommandBus
import com.futsch1.medtimer.core.location.GeofenceRegistrar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Autostart : BroadcastReceiver() {

    companion object {
        var hasRestored = false
    }

    @Inject
    lateinit var autostartService: AutostartService

    @Inject
    lateinit var geofenceRegistrar: GeofenceRegistrar

    @Inject
    lateinit var persistentDataDataSource: PersistentDataDataSource

    @Inject
    lateinit var commandBus: ReminderCommandBus

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope
    override fun onReceive(context: Context, intent: Intent) {
        if (hasRestored || intent.action == null) {
            return
        }

        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        hasRestored = true
        applicationScope.launch {
            autostartService.restoreNotifications()
            if (persistentDataDataSource.getPendingLocationSnoozes().isNotEmpty()) {
                geofenceRegistrar.registerHomeGeofence()
            }
            Log.i(LogTags.AUTOSTART, "Requesting reschedule")
            commandBus.scheduleNextNotification()
        }
    }
}

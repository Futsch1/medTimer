package com.futsch1.medtimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Autostart : BroadcastReceiver() {

    companion object {
        const val TAG = "Autostart"
        var hasRestored = false
    }

    @Inject
    lateinit var autostartService: AutostartService

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
            Log.i(TAG, "Requesting reschedule")
            ReminderProcessorBroadcastReceiver.requestScheduleNextNotification(context)
        }
    }
}

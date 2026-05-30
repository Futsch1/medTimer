package com.futsch1.medtimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIME_CHANGED && intent.action != Intent.ACTION_TIMEZONE_CHANGED) {
            return
        }
        Log.i(LogTags.AUTOSTART, "Time or timezone changed (${intent.action}), requesting reschedule")
        ReminderProcessorBroadcastReceiver.requestScheduleNextNotification(context)
    }
}

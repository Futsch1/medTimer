package com.futsch1.medtimer.reminders

import android.app.PendingIntent
import android.content.Context

class PendingIntentBuilder(private val context: Context) {
    private var reminderEventId = 0

    fun setReminderEventId(reminderEventId: Int): PendingIntentBuilder {
        this.reminderEventId = reminderEventId
        return this
    }

    fun build(): PendingIntent {
        val reminderIntent = getReminderAction(context)
        // Use the reminderEventId as request code to ensure unique PendingIntent for each reminder event
        return PendingIntent.getBroadcast(context, reminderEventId, reminderIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

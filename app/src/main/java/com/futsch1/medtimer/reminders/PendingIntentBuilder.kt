package com.futsch1.medtimer.reminders

import android.app.PendingIntent
import android.content.Context
import java.time.LocalDateTime

class PendingIntentBuilder(private val context: Context) {
    private var reminderId = 0
    private var reminderEventId = 0
    private var reminderDateTime: LocalDateTime? = null

    fun setReminderEventId(reminderEventId: Int): PendingIntentBuilder {
        this.reminderEventId = reminderEventId
        return this
    }

    fun build(): PendingIntent {
        val reminderIntent = ReminderProcessor.getReminderAction(context, intArrayOf(reminderId), intArrayOf(reminderEventId), reminderDateTime)
        // Use the reminderEventId as request code to ensure unique PendingIntent for each reminder event
        return PendingIntent.getBroadcast(context, reminderEventId, reminderIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

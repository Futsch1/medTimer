package com.futsch1.medtimer.reminders.notifications

import android.content.Intent
import android.os.Bundle
import androidx.work.Data
import com.futsch1.medtimer.ActivityCodes

class ProcessedNotification(val reminderEventIds: List<Int>) {
    fun toIntent(actionIntent: Intent) {
        actionIntent.putExtra(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, reminderEventIds.toIntArray())
    }

    companion object {
        fun fromRaisedNotification(notification: Notification): ProcessedNotification {
            return ProcessedNotification(notification.reminderEventIds.toList())
        }

        fun fromData(data: Data): ProcessedNotification {
            return ProcessedNotification(data.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)!!.toList())
        }

        fun forwardToBuilder(bundle: Bundle, builder: Data.Builder) {
            builder.putIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, bundle.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)!!)
        }
    }
}
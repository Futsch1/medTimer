package com.futsch1.medtimer.reminders.notificationData

import android.content.Intent
import android.os.Bundle
import androidx.work.Data
import com.futsch1.medtimer.ActivityCodes

class ProcessedNotificationData(val reminderEventIds: List<Int>) {
    fun toIntent(actionIntent: Intent) {
        actionIntent.putExtra(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, reminderEventIds.toIntArray())
    }

    fun toBuilder(builder: Data.Builder) {
        builder.putIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, reminderEventIds.toIntArray())
    }

    override fun toString(): String {
        return "ProcessedNotificationData: $reminderEventIds"
    }

    companion object {
        fun fromReminderNotificationData(reminderNotificationData: ReminderNotificationData): ProcessedNotificationData {
            return ProcessedNotificationData(reminderNotificationData.reminderEventIds.toList())
        }

        fun fromData(data: Data): ProcessedNotificationData {
            return ProcessedNotificationData(data.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)!!.toList())
        }

        fun forwardToBuilder(bundle: Bundle, builder: Data.Builder) {
            builder.putIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, bundle.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)!!)
        }
    }
}
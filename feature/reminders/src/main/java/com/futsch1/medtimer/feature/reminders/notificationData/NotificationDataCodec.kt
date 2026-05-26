package com.futsch1.medtimer.feature.reminders.notificationData

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.futsch1.medtimer.core.common.ActivityCodes
import com.futsch1.medtimer.feature.reminders.getReminderAction
import java.time.Instant

fun ReminderNotificationData.writeTo(intent: Intent) {
    intent.putExtra(ActivityCodes.EXTRA_REMINDER_ID_LIST, reminderIds.toIntArray())
    intent.putExtra(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, reminderEventIds.toIntArray())
    intent.putExtra(ActivityCodes.EXTRA_REMIND_INSTANT, remindInstant.epochSecond)
    intent.putExtra(ActivityCodes.EXTRA_NOTIFICATION_ID, notificationId)
}

fun ReminderNotificationData.writeTo(bundle: Bundle) {
    bundle.putIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST, reminderIds.toIntArray())
    bundle.putIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, reminderEventIds.toIntArray())
    bundle.putLong(ActivityCodes.EXTRA_REMIND_INSTANT, remindInstant.epochSecond)
    bundle.putInt(ActivityCodes.EXTRA_NOTIFICATION_ID, notificationId)
}

fun ReminderNotificationData.toPendingIntent(context: Context): PendingIntent {
    val reminderIntent = getReminderAction(context)
    writeTo(reminderIntent)
    return PendingIntent.getBroadcast(
        context,
        reminderEventIds[0],
        reminderIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun Bundle.toReminderNotificationData(): ReminderNotificationData {
    val reminderIds = reminderIdsFromBundle(this)
    val reminderEventIds = reminderEventIdsFromBundle(this)
    val remindInstant = Instant.ofEpochSecond(getLong(ActivityCodes.EXTRA_REMIND_INSTANT))
    val notificationId = getInt(ActivityCodes.EXTRA_NOTIFICATION_ID, -1)
    return ReminderNotificationData(remindInstant, reminderIds, reminderEventIds, notificationId)
}

fun reminderIdsFromBundle(bundle: Bundle): List<Int> =
    bundle.getIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST)?.toList() ?: listOf()

fun reminderEventIdsFromBundle(bundle: Bundle): List<Int> =
    bundle.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)?.toList() ?: listOf()

fun writeReminderEventIds(intent: Intent, reminderEventIds: List<Int>) {
    intent.putExtra(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, reminderEventIds.toIntArray())
}

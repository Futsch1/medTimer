package com.futsch1.medtimer.reminders

import android.content.Context
import android.content.Intent
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.MainActivity
import com.futsch1.medtimer.ProcessorCode
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import kotlin.time.Duration

/**
 * Provides factory methods for creating [Intent]s used to trigger reminder-related actions
 * via [ReminderProcessorBroadcastReceiver] or to navigate to specific screens in [MainActivity].
 */

/**
 * Creates an [Intent] for the general reminder action.
 */
fun getReminderAction(context: Context): Intent {
    val reminderIntent = Intent(ProcessorCode.Reminder.action)
    reminderIntent.setClass(context, ReminderProcessorBroadcastReceiver::class.java)
    return reminderIntent
}

/**
 * Creates an [Intent] to trigger a snooze action with a specified duration.
 *
 * @param reminderNotificationData The data of the reminder to be snoozed.
 * @param snoozeDuration The duration for which the reminder should be snoozed.
 */
fun getSnoozeIntent(context: Context, reminderNotificationData: ReminderNotificationData, snoozeDuration: Duration): Intent {
    val snoozeIntent = Intent(ProcessorCode.Snooze.action)
    reminderNotificationData.toIntent(snoozeIntent)
    snoozeIntent.putExtra(ActivityCodes.EXTRA_SNOOZE_TIME, snoozeDuration.inWholeSeconds)
    snoozeIntent.setClass(context, ReminderProcessorBroadcastReceiver::class.java)
    return snoozeIntent
}

fun getShowReminderNotificationIntent(context: Context, reminderNotificationData: ReminderNotificationData): Intent {
    val reminderIntent = Intent(ProcessorCode.ShowReminderNotification.action)
    reminderNotificationData.toIntent(reminderIntent)
    reminderIntent.setClass(context, ReminderProcessorBroadcastReceiver::class.java)
    return reminderIntent
}

private fun buildActionIntent(context: Context, processedNotificationData: ProcessedNotificationData, actionName: String): Intent {
    val actionIntent = Intent(actionName)
    actionIntent.setClass(context, ReminderProcessorBroadcastReceiver::class.java)
    processedNotificationData.toIntent(actionIntent)
    return actionIntent
}

fun getCustomSnoozeActionIntent(context: Context, reminderNotificationData: ReminderNotificationData): Intent {
    val actionIntent = Intent(ActivityCodes.CUSTOM_SNOOZE_ACTIVITY)
    actionIntent.setClass(context, MainActivity::class.java)
    actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    reminderNotificationData.toIntent(actionIntent)
    return actionIntent
}

fun getTakenActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(context, processedNotificationData, ProcessorCode.Taken.action)
}

fun getAcknowledgedActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(context, processedNotificationData, ProcessorCode.Acknowledged.action)
}

fun getRefillActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(context, processedNotificationData, ProcessorCode.Refill.action)
}

fun getRefillIntent(context: Context, medicineId: Int): Intent {
    val intent = Intent(ProcessorCode.Refill.action)
    intent.setClass(context, ReminderProcessorBroadcastReceiver::class.java)
    intent.putExtra(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
    return intent
}

fun getVariableAmountActivityIntent(context: Context, reminderNotificationData: ReminderNotificationData): Intent {
    val intent = Intent(ActivityCodes.VARIABLE_AMOUNT_ACTIVITY)
    intent.setClass(context, MainActivity::class.java)
    reminderNotificationData.toIntent(intent)
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return intent
}

fun getStockHandlingIntent(context: Context, amount: Double, medicineId: Int, processedEpochSeconds: Long): Intent {
    val intent = Intent(ProcessorCode.StockHandling.action)
    intent.setClass(context, ReminderProcessorBroadcastReceiver::class.java)
    intent.putExtra(ActivityCodes.EXTRA_AMOUNT, amount)
    intent.putExtra(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
    intent.putExtra(ActivityCodes.EXTRA_REMIND_INSTANT, processedEpochSeconds)
    return intent
}

fun getSkippedActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(context, processedNotificationData, ProcessorCode.Dismissed.action)
}

fun getRequestScheduleIntent(context: Context): Intent {
    val intent = Intent(ProcessorCode.Schedule.action)
    intent.setClass(context, ReminderProcessorBroadcastReceiver::class.java)
    return intent
}

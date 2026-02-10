package com.futsch1.medtimer.reminders

import android.content.Context
import android.content.Intent
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.MainActivity
import com.futsch1.medtimer.ProcessorCode
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData

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
 * @param snoozeTime The snooze duration in minutes.
 */
fun getSnoozeIntent(context: Context, reminderNotificationData: ReminderNotificationData, snoozeTime: Int): Intent {
    val snoozeIntent = Intent(ProcessorCode.Snooze.action)
    reminderNotificationData.toIntent(snoozeIntent)
    snoozeIntent.putExtra(ActivityCodes.EXTRA_SNOOZE_TIME, snoozeTime)
    snoozeIntent.setClass(context, ReminderProcessorBroadcastReceiver::class.java)
    return snoozeIntent
}

fun getShowReminderNotificationIntent(context: Context, reminderNotificationData: ReminderNotificationData): Intent {
    val reminderIntent = Intent(ProcessorCode.ShowReminderNotification.action)
    reminderNotificationData.toIntent(reminderIntent)
    reminderIntent.setClass(context, ReminderProcessorBroadcastReceiver::class.java)
    return reminderIntent
}

private fun buildActionIntent(context: Context, processedNotificationData: ProcessedNotificationData, actionName: String?): Intent {
    val actionIntent = Intent(context, ReminderProcessorBroadcastReceiver::class.java)
    processedNotificationData.toIntent(actionIntent)
    actionIntent.setAction(actionName)
    return actionIntent
}

fun getCustomSnoozeActionIntent(context: Context?, reminderNotificationData: ReminderNotificationData): Intent {
    val actionIntent = Intent(context, MainActivity::class.java)
    actionIntent.setAction(ActivityCodes.CUSTOM_SNOOZE_ACTIVITY)
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
    val intent = Intent(context, ReminderProcessorBroadcastReceiver::class.java)
    intent.setAction(ProcessorCode.Refill.action)
    intent.putExtra(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
    return intent
}

fun getVariableAmountActivityIntent(context: Context, reminderNotificationData: ReminderNotificationData): Intent {
    val intent = Intent(context, MainActivity::class.java)
    intent.setAction(ActivityCodes.VARIABLE_AMOUNT_ACTIVITY)
    reminderNotificationData.toIntent(intent)
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return intent
}

fun getStockHandlingIntent(context: Context, amount: Double, medicineId: Int, processedEpochSeconds: Long): Intent {
    val intent = Intent(context, ReminderProcessorBroadcastReceiver::class.java)
    intent.setAction(ProcessorCode.StockHandling.action)
    intent.putExtra(ActivityCodes.EXTRA_AMOUNT, amount)
    intent.putExtra(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
    intent.putExtra(ActivityCodes.EXTRA_REMIND_INSTANT, processedEpochSeconds)
    return intent
}

fun getRepeatIntent(context: Context, reminderNotificationData: ReminderNotificationData, repeatTimeSeconds: Int): Intent {
    val intent = Intent(context, ReminderProcessorBroadcastReceiver::class.java)
    intent.setAction(ProcessorCode.Repeat.action)
    reminderNotificationData.toIntent(intent)
    intent.putExtra(ActivityCodes.EXTRA_REPEAT_TIME_SECONDS, repeatTimeSeconds)
    return intent
}

fun getSkippedActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(context, processedNotificationData, ProcessorCode.Dismissed.action)
}

fun getRequestScheduleIntent(context: Context): Intent {
    val intent = Intent(context, ReminderProcessorBroadcastReceiver::class.java)
    intent.setAction(ProcessorCode.Schedule.action)
    return intent
}

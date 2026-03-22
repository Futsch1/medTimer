package com.futsch1.medtimer.reminders

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
fun getReminderAction(reminderContext: ReminderContext): Intent {
    val reminderIntent = Intent(ProcessorCode.Reminder.action)
    reminderContext.setIntentClass(reminderIntent, ReminderProcessorBroadcastReceiver::class.java)
    return reminderIntent
}

/**
 * Creates an [Intent] to trigger a snooze action with a specified duration.
 *
 * @param reminderNotificationData The data of the reminder to be snoozed.
 * @param snoozeDuration The duration for which the reminder should be snoozed.
 */
fun getSnoozeIntent(reminderContext: ReminderContext, reminderNotificationData: ReminderNotificationData, snoozeDuration: Duration): Intent {
    val snoozeIntent = Intent(ProcessorCode.Snooze.action)
    reminderNotificationData.toIntent(snoozeIntent)
    snoozeIntent.putExtra(ActivityCodes.EXTRA_SNOOZE_TIME, snoozeDuration.inWholeSeconds)
    reminderContext.setIntentClass(snoozeIntent, ReminderProcessorBroadcastReceiver::class.java)
    return snoozeIntent
}

fun getShowReminderNotificationIntent(reminderContext: ReminderContext, reminderNotificationData: ReminderNotificationData): Intent {
    val reminderIntent = Intent(ProcessorCode.ShowReminderNotification.action)
    reminderNotificationData.toIntent(reminderIntent)
    reminderContext.setIntentClass(reminderIntent, ReminderProcessorBroadcastReceiver::class.java)
    return reminderIntent
}

private fun buildActionIntent(reminderContext: ReminderContext, processedNotificationData: ProcessedNotificationData, actionName: String): Intent {
    val actionIntent = Intent(actionName)
    reminderContext.setIntentClass(actionIntent, ReminderProcessorBroadcastReceiver::class.java)
    processedNotificationData.toIntent(actionIntent)
    return actionIntent
}

fun getCustomSnoozeActionIntent(reminderContext: ReminderContext, reminderNotificationData: ReminderNotificationData): Intent {
    val actionIntent = Intent(ActivityCodes.CUSTOM_SNOOZE_ACTIVITY)
    reminderContext.setIntentClass(actionIntent, MainActivity::class.java)
    actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    reminderNotificationData.toIntent(actionIntent)
    return actionIntent
}

fun getTakenActionIntent(reminderContext: ReminderContext, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(reminderContext, processedNotificationData, ProcessorCode.Taken.action)
}

fun getAcknowledgedActionIntent(reminderContext: ReminderContext, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(reminderContext, processedNotificationData, ProcessorCode.Acknowledged.action)
}

fun getRefillActionIntent(reminderContext: ReminderContext, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(reminderContext, processedNotificationData, ProcessorCode.Refill.action)
}

fun getRefillIntent(reminderContext: ReminderContext, medicineId: Int): Intent {
    val intent = Intent(ProcessorCode.Refill.action)
    reminderContext.setIntentClass(intent, ReminderProcessorBroadcastReceiver::class.java)
    intent.putExtra(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
    return intent
}

fun getVariableAmountActivityIntent(reminderContext: ReminderContext, reminderNotificationData: ReminderNotificationData): Intent {
    val intent = Intent(ActivityCodes.VARIABLE_AMOUNT_ACTIVITY)
    reminderContext.setIntentClass(intent, MainActivity::class.java)
    reminderNotificationData.toIntent(intent)
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return intent
}

fun getStockHandlingIntent(reminderContext: ReminderContext, amount: Double, medicineId: Int, processedEpochSeconds: Long): Intent {
    val intent = Intent(ProcessorCode.StockHandling.action)
    reminderContext.setIntentClass(intent, ReminderProcessorBroadcastReceiver::class.java)
    intent.putExtra(ActivityCodes.EXTRA_AMOUNT, amount)
    intent.putExtra(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
    intent.putExtra(ActivityCodes.EXTRA_REMIND_INSTANT, processedEpochSeconds)
    return intent
}

fun getSkippedActionIntent(reminderContext: ReminderContext, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(reminderContext, processedNotificationData, ProcessorCode.Dismissed.action)
}

fun getRequestScheduleIntent(reminderContext: ReminderContext): Intent {
    val intent = Intent(ProcessorCode.Schedule.action)
    reminderContext.setIntentClass(intent, ReminderProcessorBroadcastReceiver::class.java)
    return intent
}

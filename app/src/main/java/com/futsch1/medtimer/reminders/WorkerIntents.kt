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
fun getReminderAction(reminderContext: ReminderContext): Intent {
    val reminderIntent = Intent(ProcessorCode.Reminder.action)
    reminderContext.setIntentClass(reminderIntent, ReminderProcessorBroadcastReceiver::class.java)
    return reminderIntent
}

/**
 * Creates an [Intent] to trigger a snooze action with a specified duration.
 *
 * @param reminderNotificationData The data of the reminder to be snoozed.
 * @param snoozeTime The snooze duration in minutes.
 */
fun getSnoozeIntent(reminderContext: ReminderContext, reminderNotificationData: ReminderNotificationData, snoozeTime: Int): Intent {
    val snoozeIntent = Intent(ProcessorCode.Snooze.action)
    reminderNotificationData.toIntent(snoozeIntent)
    snoozeIntent.putExtra(ActivityCodes.EXTRA_SNOOZE_TIME, snoozeTime)
    reminderContext.setIntentClass(snoozeIntent, ReminderProcessorBroadcastReceiver::class.java)
    return snoozeIntent
}

fun getSnoozeIntent(context: Context, reminderNotificationData: ReminderNotificationData, snoozeTime: Int): Intent =
    getSnoozeIntent(ReminderContext(context), reminderNotificationData, snoozeTime)


fun getShowReminderNotificationIntent(reminderContext: ReminderContext, reminderNotificationData: ReminderNotificationData): Intent {
    val reminderIntent = Intent(ProcessorCode.ShowReminderNotification.action)
    reminderNotificationData.toIntent(reminderIntent)
    reminderContext.setIntentClass(reminderIntent, ReminderProcessorBroadcastReceiver::class.java)
    return reminderIntent
}

fun getShowReminderNotificationIntent(context: Context, reminderNotificationData: ReminderNotificationData): Intent =
    getShowReminderNotificationIntent(ReminderContext(context), reminderNotificationData)


private fun buildActionIntent(reminderContext: ReminderContext, processedNotificationData: ProcessedNotificationData, actionName: String?): Intent {
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

fun getTakenActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent =
    getTakenActionIntent(ReminderContext(context), processedNotificationData)


fun getAcknowledgedActionIntent(reminderContext: ReminderContext, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(reminderContext, processedNotificationData, ProcessorCode.Acknowledged.action)
}

fun getAcknowledgedActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent =
    getAcknowledgedActionIntent(ReminderContext(context), processedNotificationData)


fun getRefillActionIntent(reminderContext: ReminderContext, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(reminderContext, processedNotificationData, ProcessorCode.Refill.action)
}

fun getRefillIntent(reminderContext: ReminderContext, medicineId: Int): Intent {
    val intent = Intent(ProcessorCode.Refill.action)
    reminderContext.setIntentClass(intent, ReminderProcessorBroadcastReceiver::class.java)
    intent.putExtra(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
    return intent
}

fun getRefillIntent(context: Context, medicineId: Int): Intent = getRefillIntent(ReminderContext(context), medicineId)


fun getVariableAmountActivityIntent(reminderContext: ReminderContext, reminderNotificationData: ReminderNotificationData): Intent {
    val intent = Intent(ActivityCodes.VARIABLE_AMOUNT_ACTIVITY)
    reminderContext.setIntentClass(intent, MainActivity::class.java)
    reminderNotificationData.toIntent(intent)
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return intent
}

fun getVariableAmountActivityIntent(context: Context, reminderNotificationData: ReminderNotificationData): Intent =
    getVariableAmountActivityIntent(ReminderContext(context), reminderNotificationData)


fun getStockHandlingIntent(reminderContext: ReminderContext, amount: Double, medicineId: Int, processedEpochSeconds: Long): Intent {
    val intent = Intent(ProcessorCode.StockHandling.action)
    reminderContext.setIntentClass(intent, ReminderProcessorBroadcastReceiver::class.java)
    intent.putExtra(ActivityCodes.EXTRA_AMOUNT, amount)
    intent.putExtra(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
    intent.putExtra(ActivityCodes.EXTRA_REMIND_INSTANT, processedEpochSeconds)
    return intent
}

fun getStockHandlingIntent(context: Context, amount: Double, medicineId: Int, processedEpochSeconds: Long): Intent =
    getStockHandlingIntent(ReminderContext(context), amount, medicineId, processedEpochSeconds)


fun getSkippedActionIntent(reminderContext: ReminderContext, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(reminderContext, processedNotificationData, ProcessorCode.Dismissed.action)
}

fun getSkippedActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent =
    getSkippedActionIntent(ReminderContext(context), processedNotificationData)


fun getRequestScheduleIntent(reminderContext: ReminderContext): Intent {
    val intent = Intent(ProcessorCode.Schedule.action)
    reminderContext.setIntentClass(intent, ReminderProcessorBroadcastReceiver::class.java)
    return intent
}

fun getRequestScheduleIntent(context: Context): Intent = getRequestScheduleIntent(ReminderContext(context))

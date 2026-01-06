package com.futsch1.medtimer.reminders

import android.content.Context
import android.content.Intent
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.MainActivity
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData

/**
 * Provides factory methods for creating [Intent]s used to trigger reminder-related actions
 * via [ReminderWorkerReceiver] or to navigate to specific screens in [MainActivity].
 */

/**
 * Creates an [Intent] for the general reminder action.
 */
fun getReminderAction(context: Context): Intent {
    val reminderIntent = Intent(ActivityCodes.REMINDER_ACTION)
    reminderIntent.setClass(context, ReminderWorkerReceiver::class.java)
    return reminderIntent
}

/**
 * Creates an [Intent] to trigger a snooze action with a specified duration.
 *
 * @param reminderNotificationData The data of the reminder to be snoozed.
 * @param snoozeTime The snooze duration in minutes.
 */
fun getSnoozeIntent(context: Context, reminderNotificationData: ReminderNotificationData, snoozeTime: Int): Intent {
    val snoozeIntent = Intent(ActivityCodes.SNOOZE_ACTION)
    reminderNotificationData.toIntent(snoozeIntent)
    snoozeIntent.putExtra(ActivityCodes.EXTRA_SNOOZE_TIME, snoozeTime)
    snoozeIntent.setClass(context, ReminderWorkerReceiver::class.java)
    return snoozeIntent
}

private fun buildActionIntent(context: Context, processedNotificationData: ProcessedNotificationData, actionName: String?): Intent {
    val actionIntent = Intent(context, ReminderWorkerReceiver::class.java)
    processedNotificationData.toIntent(actionIntent)
    actionIntent.setAction(actionName)
    return actionIntent
}

/**
 * Creates an [Intent] to open the UI for entering a variable dosage amount.
 *
 * @param reminderEventId The ID of the reminder event.
 * @param amount The current/default amount.
 * @param name The name of the medicine.
 */
fun getVariableAmountActionIntent(context: Context?, reminderEventId: Int, amount: String?, name: String): Intent {
    val actionIntent = Intent(context, MainActivity::class.java)
    actionIntent.setAction("VARIABLE_AMOUNT")
    actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    actionIntent.putExtra(ActivityCodes.EXTRA_REMINDER_EVENT_ID, reminderEventId)
    actionIntent.putExtra(ActivityCodes.EXTRA_AMOUNT, amount)
    actionIntent.putExtra(ActivityCodes.EXTRA_MEDICINE_NAME, name)
    return actionIntent
}

/**
 * Creates an [Intent] to open the UI for selecting a custom snooze duration.
 */
fun getCustomSnoozeActionIntent(context: Context?, reminderNotificationData: ReminderNotificationData): Intent {
    val actionIntent = Intent(context, MainActivity::class.java)
    actionIntent.setAction("CUSTOM_SNOOZE")
    actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    reminderNotificationData.toIntent(actionIntent)
    return actionIntent
}

/**
 * Creates an [Intent] to mark a reminder as "Taken".
 */
fun getTakenActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(context, processedNotificationData, ActivityCodes.TAKEN_ACTION)
}

/**
 * Creates an [Intent] to mark a reminder as "Skipped".
 */
fun getSkippedActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent {
    return buildActionIntent(context, processedNotificationData, ActivityCodes.DISMISSED_ACTION)
}

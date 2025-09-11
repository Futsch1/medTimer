package com.futsch1.medtimer.reminders.notificationFactory

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.text.SpannableStringBuilder
import androidx.core.app.NotificationCompat
import androidx.core.text.bold
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ReminderAlarmActivity
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.reminders.ReminderProcessor
import java.util.stream.Collectors


data class ReminderNotificationData(
    val remindTime: String,
    val medicine: FullMedicine,
    val reminder: Reminder,
    val reminderEvent: ReminderEvent,
    val hasSameTimeReminders: Boolean
)

fun getReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    reminderNotificationData: ReminderNotificationData
): ReminderNotificationFactory {
    val defaultPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    return if (defaultPreferences.getBoolean("big_notifications", false)) {
        BigReminderNotificationFactory(
            context, notificationId, reminderNotificationData
        )
    } else {
        SimpleReminderNotificationFactory(
            context, notificationId, reminderNotificationData
        )
    }
}

abstract class ReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    reminderNotificationData: ReminderNotificationData
) : NotificationFactory(context, notificationId, reminderNotificationData.medicine.medicine) {
    val defaultSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val baseString: SpannableStringBuilder

    val remindTime = reminderNotificationData.remindTime
    val medicine = reminderNotificationData.medicine
    val reminder = reminderNotificationData.reminder
    val reminderEvent = reminderNotificationData.reminderEvent
    val hasSameTimeReminders = reminderNotificationData.hasSameTimeReminders

    val pendingSnooze = getSnoozePendingIntent()
    val pendingSkipped = getSkippedPendingIntent()
    val pendingTaken = getTakenPendingIntent()
    val pendingAllTaken = getAllTakenPendingIntent()

    val dismissNotificationAction: String? = defaultSharedPreferences.getString("dismiss_notification_action", "0")

    init {
        val contentIntent: PendingIntent? = getStartAppIntent()

        builder.setSmallIcon(R.drawable.capsule).setContentTitle(context.getString(R.string.notification_title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setCategory(Notification.CATEGORY_REMINDER).setContentIntent(contentIntent)

        val medicineNameString = MedicineHelper.getMedicineName(context, medicine.medicine, true)
        baseString = SpannableStringBuilder().bold { append(medicineNameString) }.append(if (reminder.amount.isNotEmpty()) " (${reminder.amount})" else "")

        addDismissNotification()

        // Later than Android 14, make notification ongoing so that it cannot be dismissed from the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && defaultSharedPreferences.getBoolean(
                "sticky_on_lockscreen", false
            )
        ) {
            builder.setOngoing(true)
        }
        // If shown as alarm, add a full screen intent
        if (medicine.medicine.showNotificationAsAlarm) {
            addFullScreenIntent()
        }
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun addFullScreenIntent() {
        val intent = Intent(context, ReminderAlarmActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        builder.setCategory(Notification.CATEGORY_ALARM)
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        builder.setFullScreenIntent(pendingIntent, true)
    }

    private fun addDismissNotification() {
        when (dismissNotificationAction) {
            "0" -> {
                builder.setDeleteIntent(pendingSkipped)
            }

            "1" -> {
                builder.setDeleteIntent(pendingSnooze)
            }

            else -> {
                builder.setDeleteIntent(pendingTaken)
            }
        }

    }

    abstract fun build()

    override fun create(): Notification {
        build()
        return builder.build()
    }

    fun getTakenPendingIntent(
    ): PendingIntent? {
        return if (reminder.variableAmount) {
            val notifyTaken = ReminderProcessor.getVariableAmountActionIntent(
                context, reminderEvent.reminderEventId, reminder.amount
            )
            PendingIntent.getActivity(
                context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            val notifyTaken = ReminderProcessor.getTakenActionIntent(context, reminderEvent.reminderEventId)
            PendingIntent.getBroadcast(
                context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    fun getAllTakenPendingIntent(): PendingIntent {
        val notifyTaken = ReminderProcessor.getAllTakenActionIntent(context, reminderEvent.reminderEventId)
        return PendingIntent.getBroadcast(
            context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun getInstructions(): String {
        var instructions = reminder.instructions
        if (instructions == null) {
            instructions = ""
        }
        return addLineBreakIfNotEmpty(instructions)
    }

    fun addLineBreakIfNotEmpty(string: String): String {
        return if (string.isEmpty()) {
            string
        } else {
            "$string\n"
        }
    }

    fun getNotificationString(): SpannableStringBuilder {
        val builder = SpannableStringBuilder(baseString).append("\n${getInstructions()}")
        if (medicine.medicine.isStockManagementActive) {
            builder.append(MedicineHelper.getStockText(context, medicine.medicine))
            if (showOutOfStockIcon()) {
                builder.append(MedicineHelper.getOutOfStockText(context, medicine.medicine))
            }
            builder.append("\n")
        }

        builder.append("$remindTime\n${getTagNames()}")
        return builder
    }

    open fun showOutOfStockIcon(): Boolean {
        return true
    }

    fun getTagNames(): String {
        val tagNames = medicine.tags.stream().map { t: Tag? -> t!!.name }.collect(Collectors.toList())
        return java.lang.String.join(", ", tagNames)
    }

    fun getSkippedPendingIntent(): PendingIntent {
        val notifySkipped = ReminderProcessor.getSkippedActionIntent(context, reminderEvent.reminderEventId)
        return PendingIntent.getBroadcast(
            context, notificationId, notifySkipped, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun getSnoozePendingIntent(): PendingIntent {
        val snoozeTime = defaultSharedPreferences.getString("snooze_duration", "15")!!.toInt()

        fun getSnoozeCustomTimeIntent(): PendingIntent {
            val snooze = ReminderProcessor.getCustomSnoozeActionIntent(
                context, reminder.reminderId, reminderEvent.reminderEventId, notificationId
            )
            return PendingIntent.getActivity(
                context, notificationId, snooze, PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun getStandardSnoozeIntent(): PendingIntent {
            val snooze = ReminderProcessor.getSnoozeIntent(
                context, reminder.reminderId, reminderEvent.reminderEventId, notificationId, snoozeTime
            )
            return PendingIntent.getBroadcast(
                context, notificationId, snooze, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return if (snoozeTime == -1) {
            getSnoozeCustomTimeIntent()
        } else {
            getStandardSnoozeIntent()
        }
    }


    fun buildActions(
    ) {
        val dismissNotificationAction: String? = defaultSharedPreferences.getString("dismiss_notification_action", "0")

        when (dismissNotificationAction) {
            "0" -> {
                addTakenAction()
                addSnoozeAction()
            }

            "1" -> {
                addTakenAction()
                addSkippedAction()
            }

            else -> {
                addSkippedAction()
                addSnoozeAction()
            }
        }
        if (hasSameTimeReminders) {
            builder.addAction(
                R.drawable.check2_all, context.getString(R.string.all_taken, remindTime), pendingAllTaken
            )
        }
    }

    private fun addSkippedAction() {
        builder.addAction(
            R.drawable.x_circle, context.getString(R.string.skipped), pendingSkipped
        )
    }

    private fun addSnoozeAction() {
        builder.addAction(
            R.drawable.hourglass_split, context.getString(R.string.snooze), pendingSnooze
        )
    }

    private fun addTakenAction() {
        builder.addAction(
            R.drawable.check2_circle, context.getString(R.string.taken), pendingTaken
        )
    }

}
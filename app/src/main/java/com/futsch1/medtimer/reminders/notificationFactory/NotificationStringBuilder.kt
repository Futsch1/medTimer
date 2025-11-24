package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.bold
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.reminders.NotificationReminderEvent
import com.futsch1.medtimer.reminders.notifications.Notification
import java.util.stream.Collectors

class NotificationStringBuilder(
    val context: Context,
    val notification: Notification,
    val showOutOfStockIcon: Boolean = true
) {
    val baseString = buildBaseString(notification.notificationReminderEvents)
    val notificationString = buildNotificationString(notification.notificationReminderEvents)

    private fun buildBaseString(notificationReminderEvents: List<NotificationReminderEvent>): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        for (notificationReminderEvent in notificationReminderEvents) {
            builder.append(buildSingleBaseString(notificationReminderEvent))
            builder.append("\n")
        }
        return builder
    }

    private fun buildNotificationString(notificationReminderEvents: List<NotificationReminderEvent>): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        for (notificationReminderEvent in notificationReminderEvents) {
            builder.append(buildSingleNotificationString(notificationReminderEvent))
            builder.append("\n")
        }
        return builder
    }

    private fun buildSingleNotificationString(notificationReminderEvent: NotificationReminderEvent): SpannableStringBuilder {
        val builder = SpannableStringBuilder(baseString).append("\n${getInstructions(notificationReminderEvent.reminder)}")
        if (notificationReminderEvent.medicine.medicine.isStockManagementActive) {
            builder.append(MedicineHelper.getStockText(context, notificationReminderEvent.medicine.medicine))
            if (showOutOfStockIcon) {
                builder.append(MedicineHelper.getOutOfStockText(context, notificationReminderEvent.medicine.medicine))
            }
            builder.append("\n")
        }

        builder.append("${notification.getRemindTime(context)}\n${getTagNames(notificationReminderEvent.medicine.tags)}")
        return builder
    }

    private fun buildSingleBaseString(notificationReminderEvent: NotificationReminderEvent): SpannableStringBuilder {
        val medicineNameString = MedicineHelper.getMedicineName(context, notificationReminderEvent.medicine.medicine, true)
        return SpannableStringBuilder().bold { append(medicineNameString) }
            .append(if (notificationReminderEvent.reminder.amount.isNotEmpty()) " (${notificationReminderEvent.reminder.amount})" else "")
    }

    private fun getTagNames(tags: List<Tag>): String {
        val tagNames = tags.stream().map { t: Tag? -> t!!.name }.collect(Collectors.toList())
        return java.lang.String.join(", ", tagNames)
    }

    private fun getInstructions(reminder: Reminder): String {
        var instructions = reminder.instructions
        if (instructions == null) {
            instructions = ""
        }
        return addLineBreakIfNotEmpty(instructions)
    }

    private fun addLineBreakIfNotEmpty(string: String): String {
        return if (string.isEmpty()) {
            string
        } else {
            "$string\n"
        }
    }
}
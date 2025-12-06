package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.bold
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationPart
import java.util.stream.Collectors

class NotificationStringBuilder(
    val context: Context,
    val reminderNotification: ReminderNotification,
    val showOutOfStockIcon: Boolean = true
) {
    val baseString = buildBaseString(reminderNotification.reminderNotificationParts)
    val notificationString = buildNotificationString(reminderNotification.reminderNotificationParts)

    private fun buildBaseString(reminderNotificationParts: List<ReminderNotificationPart>): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        for (reminderNotificationPart in reminderNotificationParts) {
            builder.append(buildSingleBaseString(reminderNotificationPart))
            builder.append("\n")
        }
        return builder
    }

    private fun buildNotificationString(reminderNotificationParts: List<ReminderNotificationPart>): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        for (reminderNotificationPart in reminderNotificationParts) {
            builder.append(buildSingleNotificationString(reminderNotificationPart, reminderNotificationParts.size > 3))
            builder.append("\n")
        }
        builder.append(reminderNotification.getRemindTime(context))
        return builder
    }

    private fun buildSingleNotificationString(reminderNotificationPart: ReminderNotificationPart, concise: Boolean = false): SpannableStringBuilder {
        val builder =
            SpannableStringBuilder(buildSingleBaseString(reminderNotificationPart))
        val instructions = getInstructions(reminderNotificationPart.reminder)
        val separatorChar = if (concise) ", " else "\n"
        if (instructions.isNotEmpty()) {
            builder.append("$separatorChar$instructions")
        }

        if (reminderNotificationPart.medicine.medicine.isStockManagementActive) {
            builder.append(separatorChar)
            builder.append(MedicineHelper.getStockText(context, reminderNotificationPart.medicine.medicine))
            if (showOutOfStockIcon) {
                builder.append(MedicineHelper.getOutOfStockText(context, reminderNotificationPart.medicine.medicine))
            }
        }

        if (!concise) {
            builder.append(getTagNames(reminderNotificationPart.medicine.tags))
        }

        return builder
    }

    private fun buildSingleBaseString(reminderNotificationPart: ReminderNotificationPart): SpannableStringBuilder {
        val medicineNameString = MedicineHelper.getMedicineName(context, reminderNotificationPart.medicine.medicine, true)
        return SpannableStringBuilder().bold { append(medicineNameString) }
            .append(if (reminderNotificationPart.reminder.amount.isNotEmpty()) " (${reminderNotificationPart.reminder.amount})" else "")
    }

    private fun getTagNames(tags: List<Tag>): String {
        val tagNames = tags.stream().map { t: Tag? -> t!!.name }.collect(Collectors.toList())
        return "\n" + java.lang.String.join(", ", tagNames)
    }

    private fun getInstructions(reminder: Reminder): String {
        var instructions = reminder.instructions
        if (instructions == null) {
            instructions = ""
        }
        return instructions
    }
}
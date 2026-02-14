package com.futsch1.medtimer.reminders.notificationFactory

import android.text.SpannableStringBuilder
import androidx.core.text.bold
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationPart
import java.util.stream.Collectors

class NotificationStringBuilder(
    val reminderContext: ReminderContext,
    val reminderNotification: ReminderNotification,
    val showStockIcons: Boolean = true
) {
    val baseString = buildBaseString(reminderNotification.reminderNotificationParts)
    val notificationString = buildNotificationString(reminderNotification.reminderNotificationParts)

    private fun buildBaseString(reminderNotificationParts: List<ReminderNotificationPart>): SpannableStringBuilder {
        val builder = reminderContext.getStringBuilder()
        for (reminderNotificationPart in reminderNotificationParts) {
            builder.append(buildSingleBaseString(reminderNotificationPart))
            builder.append("\n")
        }
        return builder
    }

    private fun buildNotificationString(reminderNotificationParts: List<ReminderNotificationPart>): SpannableStringBuilder {
        val builder = reminderContext.getStringBuilder()
        for (reminderNotificationPart in reminderNotificationParts) {
            builder.append(buildSingleNotificationString(reminderNotificationPart, reminderNotificationParts.size > 3))
            builder.append("\n")
        }
        builder.append(reminderNotification.getRemindTime(reminderContext))
        return builder
    }

    private fun buildSingleNotificationString(reminderNotificationPart: ReminderNotificationPart, concise: Boolean = false): SpannableStringBuilder {
        val builder = reminderContext.getStringBuilder()
        builder.append(buildSingleBaseString(reminderNotificationPart))
        val instructions = reminderNotificationPart.reminder.instructions
        val separatorChar = if (concise) ", " else "\n"
        if (instructions?.isNotEmpty() == true) {
            builder.append("$separatorChar$instructions")
        }

        if (reminderNotificationPart.medicine.isStockManagementActive) {
            builder.append(separatorChar)
            builder.append(MedicineHelper.getStockText(reminderContext, reminderNotificationPart.medicine.medicine))
            if (showStockIcons) {
                builder.append(MedicineHelper.getStockIcons(reminderNotificationPart.medicine))
            }
        }

        if (!concise) {
            builder.append(getTagNames(reminderNotificationPart.medicine.tags))
        }

        return builder
    }

    private fun buildSingleBaseString(reminderNotificationPart: ReminderNotificationPart): SpannableStringBuilder {
        val medicineNameString = MedicineHelper.getMedicineName(reminderContext, reminderNotificationPart.medicine.medicine, true)
        return reminderContext.getStringBuilder().bold { append(medicineNameString) }
            .append(if (reminderNotificationPart.reminder.amount.isNotEmpty()) " (${reminderNotificationPart.reminder.amount})" else "")
    }

    private fun getTagNames(tags: List<Tag>): String {
        val tagNames = tags.stream().map { t: Tag? -> t!!.name }.collect(Collectors.toList())
        return "\n" + java.lang.String.join(", ", tagNames)
    }
}
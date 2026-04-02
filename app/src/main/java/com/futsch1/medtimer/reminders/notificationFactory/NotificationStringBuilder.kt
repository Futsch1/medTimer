package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.bold
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.TagEntity
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationPart
import java.util.stream.Collectors

class NotificationStringBuilder(
    private val context: Context,
    private val preferencesDataSource: PreferencesDataSource,
    private val timeFormatter: TimeFormatter,
    val reminderNotification: ReminderNotification,
    val showStockIcons: Boolean = true
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
        builder.append(reminderNotification.getRemindTime(timeFormatter))
        return builder
    }

    private fun buildSingleNotificationString(reminderNotificationPart: ReminderNotificationPart, concise: Boolean = false): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        builder.append(buildSingleBaseString(reminderNotificationPart))
        val instructions = reminderNotificationPart.reminder.instructions
        val separatorChar = if (concise) ", " else "\n"
        if (instructions?.isNotEmpty() == true) {
            builder.append("$separatorChar$instructions")
        }

        val fullMedicine = reminderNotificationPart.medicine
        if (fullMedicine.isStockManagementActive) {
            builder.append(separatorChar)
            builder.append(
                context.getString(
                    R.string.medicine_stock_string,
                    MedicineHelper.formatAmount(fullMedicine.medicine.amount, fullMedicine.medicine.unit)
                )
            )
            if (showStockIcons) {
                builder.append(MedicineHelper.getStockIcons(fullMedicine))
            }
        }

        if (!concise) {
            builder.append(getTagNames(fullMedicine.tags))
        }

        return builder
    }

    private fun buildSingleBaseString(reminderNotificationPart: ReminderNotificationPart): SpannableStringBuilder {
        val medicineNameString =
            MedicineHelper.getMedicineName(reminderNotificationPart.medicine.medicine, true, preferencesDataSource.preferences.value)
        return SpannableStringBuilder().bold { append(medicineNameString) }
            .append(if (reminderNotificationPart.reminder.amount.isNotEmpty()) " (${reminderNotificationPart.reminder.amount})" else "")
    }

    private fun getTagNames(tags: List<TagEntity>): String {
        val tagNames = tags.stream().map { t: TagEntity? -> t!!.name }.collect(Collectors.toList())
        return "\n" + java.lang.String.join(", ", tagNames)
    }
}

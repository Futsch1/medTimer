package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.bold
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.MedicineHelper
import java.util.stream.Collectors

class NotificationStringBuilder(
    val context: Context,
    tuples: List<NotificationStringTuple>,
    val remindTime: String,
    val showOutOfStockIcon: Boolean = true
) {
    val baseString = buildBaseString(tuples)
    val notificationString = buildNotificationString(tuples)

    private fun buildBaseString(tuples: List<NotificationStringTuple>): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        for (tuple in tuples) {
            builder.append(buildSingleBaseString(tuple))
            builder.append("\n")
        }
        return builder
    }

    private fun buildNotificationString(tuples: List<NotificationStringTuple>): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        for (tuple in tuples) {
            builder.append(buildSingleNotificationString(tuple))
            builder.append("\n")
        }
        return builder
    }

    private fun buildSingleNotificationString(tuple: NotificationStringTuple): SpannableStringBuilder {
        val builder = SpannableStringBuilder(baseString).append("\n${getInstructions(tuple.reminder)}")
        if (tuple.medicine.medicine.isStockManagementActive) {
            builder.append(MedicineHelper.getStockText(context, tuple.medicine.medicine))
            if (showOutOfStockIcon) {
                builder.append(MedicineHelper.getOutOfStockText(context, tuple.medicine.medicine))
            }
            builder.append("\n")
        }

        builder.append("${remindTime}\n${getTagNames(tuple.medicine.tags)}")
        return builder
    }

    private fun buildSingleBaseString(tuple: NotificationStringTuple): SpannableStringBuilder {
        val medicineNameString = MedicineHelper.getMedicineName(context, tuple.medicine.medicine, true)
        return SpannableStringBuilder().bold { append(medicineNameString) }
            .append(if (tuple.reminder.amount.isNotEmpty()) " (${tuple.reminder.amount})" else "")
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
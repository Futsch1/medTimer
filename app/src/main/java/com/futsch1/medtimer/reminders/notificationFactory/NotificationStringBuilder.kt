package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.bold
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.MedicineHelper
import java.util.stream.Collectors

class NotificationStringBuilder(
    val context: Context,
    val fullMedicine: FullMedicine,
    val reminder: Reminder,
    val remindTime: String,
    val showOutOfStockIcon: Boolean = true
) {
    val baseString = getBaseString()
    val notificationString = getNotificationString()

    private fun getNotificationString(): SpannableStringBuilder {
        val builder = SpannableStringBuilder(baseString).append("\n${getInstructions(reminder)}")
        if (fullMedicine.medicine.isStockManagementActive) {
            builder.append(MedicineHelper.getStockText(context, fullMedicine.medicine))
            if (showOutOfStockIcon) {
                builder.append(MedicineHelper.getOutOfStockText(context, fullMedicine.medicine))
            }
            builder.append("\n")
        }

        builder.append("${remindTime}\n${getTagNames(fullMedicine.tags)}")
        return builder
    }

    private fun getBaseString(): SpannableStringBuilder {
        val medicineNameString = MedicineHelper.getMedicineName(context, fullMedicine.medicine, true)
        return SpannableStringBuilder().bold { append(medicineNameString) }.append(if (reminder.amount.isNotEmpty()) " (${reminder.amount})" else "")
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
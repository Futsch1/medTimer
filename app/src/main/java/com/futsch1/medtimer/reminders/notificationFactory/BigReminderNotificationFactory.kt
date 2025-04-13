package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import android.text.SpannableStringBuilder
import android.widget.RemoteViews
import androidx.core.text.bold
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.MedicineHelper

class BigReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    val remindTime: String,
    medicine: FullMedicine,
    reminder: Reminder,
    reminderEvent: ReminderEvent
) : ReminderNotificationFactory(
    context,
    notificationId,
    medicine,
    reminder,
    reminderEvent
) {
    val views: RemoteViews = RemoteViews(context.packageName, R.layout.notification)

    override fun build() {
        val medicineNameString =
            MedicineHelper.getMedicineName(context, medicine.medicine, true)
        val stockString =
            addLineBreakIfNotEmpty(MedicineHelper.getStockText(context, medicine.medicine))
        val baseString = SpannableStringBuilder().bold { append(medicineNameString) }
            .append(" (${reminder.amount})")
        val notificationString = SpannableStringBuilder().append(baseString)
            .append("\n${getInstructions()}$stockString$remindTime")

        views.setTextViewText(
            R.id.notificationTitle,
            notificationString
        )

        views.setOnClickPendingIntent(R.id.takenButton, getTakenPendingIntent())
        views.setOnClickPendingIntent(R.id.skippedButton, getSkippedPendingIntent())
        views.setOnClickPendingIntent(R.id.snoozeButton, getSnoozePendingIntent())
        views.setTextViewCompoundDrawablesRelative(
            R.id.notificationTitle,
            if (medicine.medicine.isOutOfStock) R.drawable.box_seam else 0,
            0,
            0,
            0
        )

        builder.setCustomBigContentView(views)
        builder.setContentText(baseString)
    }
}
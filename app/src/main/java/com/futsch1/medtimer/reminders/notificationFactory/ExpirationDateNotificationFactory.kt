package com.futsch1.medtimer.reminders.notificationFactory

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification

class ExpirationDateNotificationFactory(context: Context, val reminderNotification: ReminderNotification) :
    NotificationFactory(
        context,
        reminderNotification.reminderNotificationData.notificationId,
        reminderNotification.reminderNotificationParts.map { it.medicine.medicine }) {

    init {
        val contentIntent = getStartAppIntent()
        val medicine = reminderNotification.reminderNotificationParts[0].medicine.medicine

        val medicineNameString = MedicineHelper.getMedicineName(context, medicine, true)
        val notificationMessage = context.getString(
            R.string.expiration_date_notification,
            medicineNameString,
            TimeHelper.daysSinceEpochToDateString(context, medicine.expirationDate)
        )
        val intentBuilder = StockIntentBuilder(context, reminderNotification)

        builder.setSmallIcon(R.drawable.ban)
            .setContentTitle(context.getString(R.string.expiration_reminder))
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentText(notificationMessage)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setDeleteIntent(intentBuilder.pendingAcknowledged)
    }

    override fun create(): Notification {
        return builder.build()
    }
}
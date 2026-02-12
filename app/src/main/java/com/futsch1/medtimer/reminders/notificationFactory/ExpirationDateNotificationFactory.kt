package com.futsch1.medtimer.reminders.notificationFactory

import android.app.Notification
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification

class ExpirationDateNotificationFactory(reminderContext: ReminderContext, val reminderNotification: ReminderNotification) :
    NotificationFactory(
        reminderContext,
        reminderNotification.reminderNotificationData.notificationId,
        reminderNotification.reminderNotificationParts.map { it.medicine.medicine }) {

    init {
        val contentIntent = getStartAppIntent()
        val medicine = reminderNotification.reminderNotificationParts[0].medicine.medicine

        val medicineNameString = MedicineHelper.getMedicineName(reminderContext, medicine, true)
        val notificationMessage = reminderContext.getString(
            R.string.expiration_date_notification,
            medicineNameString,
            reminderContext.daysSinceEpochToDateString(medicine.expirationDate)
        )
        val intentBuilder = StockIntentBuilder(reminderContext, reminderNotification)

        builder.setSmallIcon(R.drawable.ban)
            .setContentTitle(reminderContext.getString(R.string.expiration_reminder))
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentText(notificationMessage)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setDeleteIntent(intentBuilder.pendingAcknowledged)
    }

    override fun create(): Notification {
        reminderNotification.reminderNotificationData.toBundle(builder.extras)
        return builder.build()
    }
}
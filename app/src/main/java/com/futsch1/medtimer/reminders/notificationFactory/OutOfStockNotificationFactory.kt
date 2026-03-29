package com.futsch1.medtimer.reminders.notificationFactory

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.MedicineHelper.formatAmount
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext

class OutOfStockNotificationFactory @AssistedInject constructor(
    medicineIcons: MedicineIcons,
    @param:ApplicationContext private val context: Context,
    @Assisted val reminderNotification: ReminderNotification,
    notificationManager: NotificationManager,
    preferencesDataSource: PreferencesDataSource
) :
    NotificationFactory(
        medicineIcons,
        context,
        reminderNotification.reminderNotificationData.notificationId,
        reminderNotification.reminderNotificationParts.map { it.medicine.medicine },
        notificationManager
    ) {

    init {
        val contentIntent = getStartAppIntent()
        val medicine = reminderNotification.reminderNotificationParts[0].medicine.medicine

        val medicineNameString = MedicineHelper.getMedicineName(medicine, true, preferencesDataSource.preferences.value)
        val notificationMessage = context.getString(
            R.string.out_of_stock_notification,
            medicineNameString,
            formatAmount(medicine.amount, medicine.unit)
        )
        val intentBuilder = StockIntentBuilder(context, reminderNotification)

        builder.setSmallIcon(R.drawable.box_seam)
            .setContentTitle(context.getString(R.string.out_of_stock_notification_title))
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentText(notificationMessage)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setDeleteIntent(intentBuilder.pendingAcknowledged)
            .addAction(
                R.drawable.cart2,
                context.getString(R.string.refill_amount, formatAmount(medicine.refillSize, medicine.unit)),
                intentBuilder.pendingRefill
            )
    }

    @AssistedFactory
    fun interface Factory {
        fun create(reminderNotification: ReminderNotification): OutOfStockNotificationFactory
    }

    override fun create(): Notification {
        reminderNotification.reminderNotificationData.toBundle(builder.extras)
        return builder.build()
    }
}

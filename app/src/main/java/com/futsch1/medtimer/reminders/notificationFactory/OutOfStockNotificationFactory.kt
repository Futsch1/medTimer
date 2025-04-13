package com.futsch1.medtimer.reminders.notificationFactory

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.MedicineHelper.formatAmount

class OutOfStockNotificationFactory(context: Context, notificationId: Int, medicine: Medicine) :
    NotificationFactory(context, notificationId, medicine) {

    init {
        val contentIntent = getStartAppIntent()

        builder.setSmallIcon(R.drawable.box_seam)
            .setContentTitle(context.getString(R.string.out_of_stock_notification_title))
            .setContentText(
                context.getString(
                    R.string.out_of_stock_notification,
                    medicine.name,
                    formatAmount(medicine.amount, medicine.unit)
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
    }

    override fun create(): Notification {
        return builder.build()
    }
}
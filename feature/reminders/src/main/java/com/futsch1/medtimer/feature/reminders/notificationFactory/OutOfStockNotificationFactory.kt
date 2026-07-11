package com.futsch1.medtimer.feature.reminders.notificationFactory

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.MedicineHelper.formatAmount
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.ui.MedicineIcons
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext

class OutOfStockNotificationFactory @AssistedInject constructor(
    medicineIcons: MedicineIcons,
    @param:ApplicationContext private val context: Context,
    @Assisted val reminderNotification: ReminderNotification,
    notificationManager: NotificationManager,
    private val preferencesDataSource: PreferencesDataSource
) :
    NotificationFactory(
        medicineIcons,
        context,
        reminderNotification.reminderNotificationData.notificationId,
        reminderNotification.reminderNotificationParts.map { it.medicine },
        notificationManager,
        Medicine.ReminderChannel.OUT_OF_STOCK
    ) {

    init {
        val contentIntent = getStartAppIntent()
        val medicine = reminderNotification.reminderNotificationParts[0].medicine

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

        if (preferencesDataSource.preferences.value.prescriptionContact.isNotBlank()) {
            builder.addAction(
                R.drawable.clipboard_plus,
                context.getString(R.string.request_prescription),
                getRequestPrescriptionPendingIntent()
            )
        }
    }

    private fun getRequestPrescriptionPendingIntent(): PendingIntent {
        // Explicit intent targeting feature:ui's NfcActionActivity by class name string, not by
        // ::class.java reference - feature:ui already depends on feature:reminders, so a
        // compile-time reference the other way round would be a module dependency cycle.
        val requestPrescriptionIntent = Intent(Intent.ACTION_VIEW).apply {
            setClassName(context.packageName, "com.futsch1.medtimer.feature.ui.nfc.NfcActionActivity")
            data = "medtimer://requestPrescription".toUri()
        }
        return PendingIntent.getActivity(
            context,
            reminderNotification.reminderNotificationData.notificationId,
            requestPrescriptionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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

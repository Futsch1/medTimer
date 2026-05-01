package com.futsch1.medtimer.reminders.notificationFactory

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext

class BigReminderNotificationFactory @AssistedInject constructor(
    medicineIcons: MedicineIcons,
    @param:ApplicationContext private val context: Context,
    @Assisted reminderNotification: ReminderNotification,
    notificationManager: NotificationManager,
    intentsFactory: NotificationIntentBuilder.Factory,
    preferencesDataSource: PreferencesDataSource,
    timeFormatter: TimeFormatter
) : ReminderNotificationFactory(
    medicineIcons,
    context,
    reminderNotification,
    notificationManager,
    intentsFactory,
    preferencesDataSource,
    timeFormatter
) {
    @AssistedFactory
    fun interface Factory {
        fun create(reminderNotification: ReminderNotification): BigReminderNotificationFactory
    }

    val views: RemoteViews = RemoteViews(context.packageName, R.layout.notification)

    override fun build() {
        views.setTextViewText(
            R.id.notificationTitle,
            notificationStrings.notificationString
        )

        views.setOnClickPendingIntent(R.id.takenButton, intents.pendingTaken)
        views.setOnClickPendingIntent(R.id.skippedButton, intents.pendingSkipped)
        views.setOnClickPendingIntent(R.id.snoozeButton, intents.pendingSnooze)
        if (intents.pendingLocationSnooze != null) {
            views.setOnClickPendingIntent(R.id.locationSnoozeButton, intents.pendingLocationSnooze)
        } else {
            views.setViewVisibility(R.id.locationSnoozeButton, View.GONE)
        }
        val isAnyOutOfStock = reminderNotification.reminderNotificationParts.any { it.medicine.isOutOfStock() }
        views.setTextViewCompoundDrawablesRelative(
            R.id.notificationTitle,
            if (isAnyOutOfStock) R.drawable.exclamation_triangle_fill else 0,
            0,
            0,
            0
        )

        builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        builder.setCustomBigContentView(views)
        builder.setContentText(notificationStrings.baseString)
        builder.setLargeIcon(null as Bitmap?)

        buildActions()
    }
}

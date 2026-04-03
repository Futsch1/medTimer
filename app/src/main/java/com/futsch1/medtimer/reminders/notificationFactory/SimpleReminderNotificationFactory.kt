package com.futsch1.medtimer.reminders.notificationFactory

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext

class SimpleReminderNotificationFactory @AssistedInject constructor(
    medicineIcons: MedicineIcons,
    @ApplicationContext context: Context,
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
        fun create(reminderNotification: ReminderNotification): SimpleReminderNotificationFactory
    }
    override fun build() {
        val notificationMessage = notificationStrings.notificationString

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
        builder.setContentText(notificationMessage)

        buildActions()
    }
}

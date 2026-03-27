package com.futsch1.medtimer.reminders.notificationFactory

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext

class SimpleReminderNotificationFactory @AssistedInject constructor(
    reminderContext: ReminderContext,
    @ApplicationContext context: Context,
    @Assisted reminderNotification: ReminderNotification,
    notificationManager: NotificationManager,
    intentsFactory: NotificationIntentBuilder.Factory
) : ReminderNotificationFactory(
    reminderContext,
    context,
    reminderNotification,
    notificationManager,
    intentsFactory
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
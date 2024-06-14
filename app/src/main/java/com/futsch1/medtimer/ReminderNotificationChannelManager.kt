package com.futsch1.medtimer

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager

class ReminderNotificationChannelManager {
    companion object {
        private val notificationChannels: MutableMap<Importance, ReminderNotificationChannel> =
            mutableMapOf()

        fun getNotificationChannel(
            context: Context,
            importance: Importance
        ): ReminderNotificationChannel {
            var channel = notificationChannels[importance]
            if (channel == null) {
                channel = createChannel(context, importance, getNotificationRingtone(context))
            }
            return channel
        }

        private fun createChannel(
            context: Context,
            importance: Importance,
            ringtone: Uri?
        ): ReminderNotificationChannel {
            val channel = ReminderNotificationChannel(
                context,
                ringtone,
                importance.value
            )
            notificationChannels[importance] = channel
            return channel
        }

        private fun getNotificationRingtone(context: Context): Uri {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            val ringtoneUri = sharedPref.getString(
                "notification_ringtone",
                "content://settings/system/notification_sound"
            )
            return Uri.parse(ringtoneUri)
        }

        fun updateNotificationChannelRingtone(context: Context, ringtone: Uri?) {
            for (importance in Importance.entries) {
                createChannel(context, importance, ringtone)
            }
        }

    }

    enum class Importance(val value: Int) {
        DEFAULT(NotificationManager.IMPORTANCE_DEFAULT),
        HIGH(NotificationManager.IMPORTANCE_HIGH)
    }
}

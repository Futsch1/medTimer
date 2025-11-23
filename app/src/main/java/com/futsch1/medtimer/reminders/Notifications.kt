package com.futsch1.medtimer.reminders

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.reminders.notificationFactory.OutOfStockNotificationFactory
import com.futsch1.medtimer.reminders.notificationFactory.ReminderNotificationData
import com.futsch1.medtimer.reminders.notificationFactory.getReminderNotificationFactory

@SuppressLint("DefaultLocale")
class Notifications(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE)

    fun showNotification(remindTime: String, triplets: List<NotificationTriplet>): Int {
        val notificationId = this.nextNotificationId

        val factory = getReminderNotificationFactory(
            context,
            notificationId,
            ReminderNotificationData(remindTime, triplets)
        )

        notify(notificationId, factory.create())
        Log.d(LogTags.REMINDER, String.format("Show notification nID %d", notificationId))

        return notificationId
    }

    private val nextNotificationId: Int
        get() {
            sharedPreferences.edit().apply()
            val notificationId = sharedPreferences.getInt("notificationId", 1)
            sharedPreferences.edit().putInt("notificationId", notificationId + 1).apply()

            return notificationId
        }


    private fun notify(notificationId: Int, notification: Notification?) {
        val notificationManager = context.getSystemService<NotificationManager>(NotificationManager::class.java)
        val soundManager = NotificationSoundManager(context)

        notificationManager.notify(notificationId, notification)

        soundManager.restore()
    }


    fun showOutOfStockNotification(medicine: Medicine) {
        val notificationId = this.nextNotificationId

        val factory = OutOfStockNotificationFactory(context, notificationId, medicine)

        notify(notificationId, factory.create())
        Log.d(LogTags.REMINDER, String.format("Show out of stock notification nID %d", notificationId))
    }
}

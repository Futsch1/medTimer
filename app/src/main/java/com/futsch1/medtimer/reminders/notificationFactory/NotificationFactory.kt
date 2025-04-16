package com.futsch1.medtimer.reminders.notificationFactory

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.MainActivity
import com.futsch1.medtimer.ReminderNotificationChannelManager.Companion.getNotificationChannel
import com.futsch1.medtimer.ReminderNotificationChannelManager.Importance
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.MedicineIcons


abstract class NotificationFactory(
    val context: Context,
    val notificationId: Int,
    medicine: Medicine
) {
    val builder: NotificationCompat.Builder

    init {
        val importance =
            if (medicine.notificationImportance == Importance.HIGH.value) Importance.HIGH else Importance.DEFAULT
        val notificationChannelId = getNotificationChannel(context, importance).id
        builder = NotificationCompat.Builder(context, notificationChannelId)

        val color = if (medicine.useColor) Color.valueOf(medicine.color) else null
        if (medicine.iconId != 0) {
            val icons = MedicineIcons(context)
            builder.setLargeIcon(icons.getIconBitmap(medicine.iconId))
        }
        if (color != null) {
            builder.setColor(color.toArgb()).setColorized(true)
        }

    }

    fun getStartAppIntent(): PendingIntent? {
        val startApp = Intent(context, MainActivity::class.java)
        startApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            notificationId,
            startApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    abstract fun create(): Notification
}
package com.futsch1.medtimer.reminders.notificationFactory

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.MainActivity
import com.futsch1.medtimer.ReminderNotificationChannelManager.Companion.getNotificationChannel
import com.futsch1.medtimer.ReminderNotificationChannelManager.Importance
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.reminders.ReminderContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


abstract class NotificationFactory(
    val reminderContext: ReminderContext,
    val notificationId: Int,
    medicines: List<Medicine>
) {
    val builder: NotificationCompat.Builder

    init {
        val importance = getHighestImportance(medicines)
        val notificationChannelId = getNotificationChannel(reminderContext, importance).id
        builder = reminderContext.getNotificationBuilder(notificationChannelId)

        val color = getColor(medicines)
        val icon = getIcon(medicines)

        if (icon != null) {
            builder.setLargeIcon(icon)
        }
        if (color != null) {
            builder.setColor(color.toArgb()).setColorized(true)
        }
        builder.setSilent(shouldBeSilent())
    }

    private fun getIcon(medicines: List<Medicine>): Bitmap? {
        val iconIds: List<Int> = medicines.stream().map { medicine -> medicine.iconId }.filter { it != 0 }.toList()
        return reminderContext.icons.getIconsBitmap(iconIds)
    }

    private fun getHighestImportance(medicines: List<Medicine>): Importance {
        for (medicine in medicines) {
            if (medicine.notificationImportance == Importance.HIGH.value)
                return Importance.HIGH
        }
        return Importance.DEFAULT
    }

    private fun getColor(medicines: List<Medicine>): Color? {
        var color: Color? = null
        for (medicine in medicines) {
            if (medicine.useColor) {
                if (color != null) {
                    // If more than one medicine is colored, do not use a color at all
                    return null
                }
                color = Color.valueOf(medicine.color)
            }
        }
        return color
    }

    fun getStartAppIntent(): PendingIntent {
        val startApp = reminderContext.getIntent()
        reminderContext.setIntentClass(startApp, MainActivity::class.java)
        startApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return reminderContext.getPendingIntentActivity(
            notificationId,
            startApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @OptIn(ExperimentalTime::class)
    companion object {
        var lastNotificationTime: Instant = Instant.fromEpochMilliseconds(0)

        fun shouldBeSilent(): Boolean {
            val now = Clock.System.now()
            val shouldBeSilent = now - lastNotificationTime <= 10.seconds
            lastNotificationTime = now

            return shouldBeSilent
        }
    }

    abstract fun create(): Notification
}
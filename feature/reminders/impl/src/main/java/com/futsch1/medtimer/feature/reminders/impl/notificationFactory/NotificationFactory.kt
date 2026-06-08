package com.futsch1.medtimer.feature.reminders.impl.notificationFactory

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.ui.MedicineIcons
import com.futsch1.medtimer.feature.reminders.impl.ReminderNotificationChannelManager
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


abstract class NotificationFactory(
    private val medicineIcons: MedicineIcons,
    private val context: Context,
    protected val notificationId: Int,
    medicines: List<Medicine>,
    notificationManager: NotificationManager,
    reminderChannel: Medicine.ReminderChannel
) {
    val builder: NotificationCompat.Builder

    init {
        val notificationChannelId = ReminderNotificationChannelManager.getNotificationChannel(
            context,
            notificationManager,
            reminderChannel
        ).id
        builder = NotificationCompat.Builder(context, notificationChannelId)

        val color = getColor(medicines)
        val icon = getIcon(medicines)

        if (icon != null) {
            builder.setLargeIcon(icon)
        }
        if (color != null) {
            builder.setColor(color.toArgb()).setColorized(true)
        }
        builder.setGroup(notificationId.toString())
        builder.setSilent(shouldBeSilent())
    }

    private fun getIcon(medicines: List<Medicine>): Bitmap? {
        val iconIds: List<Int> =
            medicines.stream().map { medicine -> medicine.iconId }.filter { it != 0 }.distinct()
                .toList()
        return medicineIcons.getIconsBitmap(iconIds)
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
        val startApp = Intent().setClassName(context, "com.futsch1.medtimer.MainActivity").apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
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
package com.futsch1.medtimer.reminders.notificationFactory

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
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
    medicines: List<Medicine?>
) {
    val builder: NotificationCompat.Builder

    init {
        val importance = getHighestImportance(medicines)
        val notificationChannelId = getNotificationChannel(reminderContext, importance).id
        builder = reminderContext.getNotificationBuilder(notificationChannelId)

        val color = getColor(medicines)
        val iconId = getIconId(medicines)

        if (iconId != 0) {
            builder.setLargeIcon(reminderContext.icons.getIconBitmap(iconId))
        }
        if (color != null) {
            builder.setColor(color.toArgb()).setColorized(true)
        }
        builder.setSilent(shouldBeSilent())
    }

    private fun getIconId(medicines: List<Medicine?>): Int {
        for (medicine in medicines) {
            if (medicine?.iconId != 0)
                return medicine!!.iconId
        }
        return 0
    }

    private fun getHighestImportance(medicines: List<Medicine?>): Importance {
        for (medicine in medicines) {
            if (medicine?.notificationImportance == Importance.HIGH.value)
                return Importance.HIGH
        }
        return Importance.DEFAULT
    }

    private fun getColor(medicines: List<Medicine?>): Color? {
        for (medicine in medicines) {
            if (medicine?.useColor == true)
                return Color.valueOf(medicine.color)
        }
        return null
    }

    fun getStartAppIntent(): PendingIntent {
        val startApp = Intent()
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
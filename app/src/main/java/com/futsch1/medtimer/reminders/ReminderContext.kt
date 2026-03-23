package com.futsch1.medtimer.reminders

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class ReminderContext @Inject constructor(
    @param:ApplicationContext val context: Context,
    val medicineRepository: MedicineRepository,
    val preferencesDataSource: PreferencesDataSource,
    val persistentDataDataSource: PersistentDataDataSource
) {
    val icons = MedicineIcons(context)
    val timeAccess = object : TimeAccess {
        override fun systemZone(): ZoneId = ZoneId.systemDefault()
        override fun localDate(): LocalDate = LocalDate.now(systemZone())
        override fun now(): Instant = Instant.now()
    }

    fun getPendingIntentBroadcast(requestCode: Int, intent: Intent, flags: Int): PendingIntent {
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    fun getPendingIntentActivity(requestCode: Int, intent: Intent, flags: Int): PendingIntent {
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    fun setIntentClass(intent: Intent, cls: Class<*>) {
        intent.setClass(context, cls)
    }

    fun sendBroadcast(intent: Intent, receiverPermission: String) {
        context.sendBroadcast(intent, receiverPermission)
    }

    fun buildNotificationChannel(id: String, name: CharSequence, importance: Int): NotificationChannel = NotificationChannel(id, name, importance)

    fun getString(id: Int, vararg formatArgs: Any): String = context.getString(id, *formatArgs)

    fun getNotificationBuilder(channel: String): NotificationCompat.Builder = NotificationCompat.Builder(context, channel)
    fun getStringBuilder(): SpannableStringBuilder = SpannableStringBuilder()

    fun minutesToTimeString(minutes: Long): String = TimeHelper.minutesToTimeString(context, minutes)
    fun daysSinceEpochToDateString(days: Long): String = TimeHelper.daysSinceEpochToDateString(context, days)
}

interface TimeAccess {
    fun systemZone(): ZoneId

    fun localDate(): LocalDate

    fun now(): Instant
}
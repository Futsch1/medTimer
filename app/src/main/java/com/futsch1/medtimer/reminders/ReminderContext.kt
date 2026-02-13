package com.futsch1.medtimer.reminders

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.TimeHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ReminderContext(private val context: Context) {
    val medicineRepository = MedicineRepository(context.applicationContext as Application)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val localPreferences: SharedPreferences = context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE)
    val icons = MedicineIcons(context)
    val packageName: String = context.packageName
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

    fun getIntent(action: String): Intent = Intent(action)

    fun setIntentClass(intent: Intent, cls: Class<*>) {
        intent.setClass(context, cls)
    }

    fun sendBroadcast(intent: Intent, receiverPermission: String) {
        context.sendBroadcast(intent, receiverPermission)
    }

    fun buildNotificationChannel(id: String, name: CharSequence, importance: Int): NotificationChannel = NotificationChannel(id, name, importance)

    fun hasPermission(permission: String): Boolean = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    fun getString(id: Int, vararg formatArgs: Any): String = context.getString(id, *formatArgs)

    fun getNotificationBuilder(channel: String): NotificationCompat.Builder = NotificationCompat.Builder(context, channel)
    fun minutesToTimeString(minutes: Long): String = TimeHelper.minutesToTimeString(context, minutes)
    fun daysSinceEpochToDateString(days: Long): String = TimeHelper.daysSinceEpochToDateString(context, days)
}

interface TimeAccess {
    fun systemZone(): ZoneId

    fun localDate(): LocalDate

    fun now(): Instant
}
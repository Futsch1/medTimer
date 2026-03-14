package com.futsch1.medtimer.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.text.SpannableStringBuilder
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

// TODO: potential memory leak - analyze what the context is used for and try to use DI to remove the need of storing the context
class ReminderContext @Inject constructor(@param:ApplicationContext val context: Context) {
    val medicineRepository = MedicineRepository(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val icons = MedicineIcons(context)
    val packageName: String = context.packageName
    val timeAccess = object : TimeAccess {
        override fun systemZone(): ZoneId = ZoneId.systemDefault()
        override fun localDate(): LocalDate = LocalDate.now(systemZone())
        override fun now(): Instant = Instant.now()
    }
    val sdkInt = Build.VERSION.SDK_INT

    fun getPendingIntentBroadcast(requestCode: Int, intent: Intent, flags: Int): PendingIntent {
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    fun getPendingIntentActivity(requestCode: Int, intent: Intent, flags: Int): PendingIntent {
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    fun getIntent(action: String? = null): Intent = Intent(action)

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
    fun getStringBuilder(): SpannableStringBuilder = SpannableStringBuilder()

    fun minutesToTimeString(minutes: Long): String = TimeHelper.minutesToTimeString(context, minutes)
    fun daysSinceEpochToDateString(days: Long): String = TimeHelper.daysSinceEpochToDateString(context, days)
    fun startActivity(intent: Intent) {
        context.startActivity(intent)
    }

    val preferencesDataSource: PreferencesDataSource by lazy {
        // Bridge from non-Hilt to Hilt code
        EntryPointAccessors.fromApplication(context, DataSourcesEntryPoint::class.java).getPreferencesDataSource()
    }

    val persistentDataDataSource: PersistentDataDataSource by lazy {
        // Bridge from non-Hilt to Hilt code
        EntryPointAccessors.fromApplication(context, DataSourcesEntryPoint::class.java).getPersistentDataDataSource()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataSourcesEntryPoint {
    fun getPreferencesDataSource(): PreferencesDataSource
    fun getPersistentDataDataSource(): PersistentDataDataSource
}

interface TimeAccess {
    fun systemZone(): ZoneId

    fun localDate(): LocalDate

    fun now(): Instant
}
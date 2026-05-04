package com.futsch1.medtimer.preferences

import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceDataStore
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.di.DefaultPreferences
import com.futsch1.medtimer.model.BackupInterval
import com.futsch1.medtimer.model.DismissNotificationAction
import com.futsch1.medtimer.model.HomeLocation
import com.futsch1.medtimer.model.ThemeSetting
import com.futsch1.medtimer.model.UserPreferences
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PreferencesDataSource @Inject constructor(
    @param:DefaultPreferences private val sharedPreferences: SharedPreferences,
    @param:ApplicationScope private val scope: kotlinx.coroutines.CoroutineScope,
    private val gson: Gson
) : PreferenceDataStore() {
    val preferences: StateFlow<UserPreferences> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(getSettings())
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.stateIn(scope, started = SharingStarted.Eagerly, initialValue = getSettings())

    fun setWeekendTime(value: LocalTime) {
        sharedPreferences.edit { putInt(WEEKEND_TIME, value.toSecondOfDay() / 60) }
    }

    fun setAutomaticBackupInterval(backupInterval: BackupInterval) {
        sharedPreferences.edit { putString(AUTOMATIC_BACKUP_INTERVAL, backupInterval.ordinal.toString()) }
    }

    fun setAutomaticBackupDirectory(uri: Uri) {
        sharedPreferences.edit { putString(AUTOMATIC_BACKUP_DIRECTORY, uri.toString()) }
    }

    fun saveHomeLocation(location: HomeLocation) {
        sharedPreferences.edit { putString(HOME_LOCATION, gson.toJson(location)) }
    }

    private fun getHomeLocation(): HomeLocation? {
        val json = sharedPreferences.getString(HOME_LOCATION, null) ?: return null
        return gson.fromJson(json, HomeLocation::class.java)
    }

    fun clearHomeLocation() {
        sharedPreferences.edit { remove(HOME_LOCATION) }
    }

    // Functions for compatibility with PreferencesFragment
    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defValue)
    }

    override fun putBoolean(key: String?, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    override fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>? {
        return sharedPreferences.getStringSet(key, defValues)
    }

    override fun putStringSet(key: String?, values: Set<String?>?) {
        sharedPreferences.edit { putStringSet(key, values) }
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return sharedPreferences.getInt(key, defValue)
    }

    override fun putInt(key: String?, value: Int) {
        sharedPreferences.edit { putInt(key, value) }
    }

    override fun getString(key: String?, defValue: String?): String? {
        return sharedPreferences.getString(key, defValue)
    }

    override fun putString(key: String?, value: String?) {
        sharedPreferences.edit { putString(key, value) }
    }

    private fun getSettings(): UserPreferences {
        val default = UserPreferences.default()
        return UserPreferences(
            weekendTime = LocalTime.of(
                sharedPreferences.getInt(WEEKEND_TIME, default.weekendTime.toSecondOfDay() / 60) / 60,
                sharedPreferences.getInt(WEEKEND_TIME, default.weekendTime.toSecondOfDay() / 60) % 60
            ),
            weekendMode = sharedPreferences.getBoolean(WEEKEND_MODE, default.weekendMode),
            weekendDays = sharedPreferences.getStringSet(WEEKEND_DAYS, default.weekendDays) ?: emptySet(),
            exactReminders = sharedPreferences.getBoolean(EXACT_REMINDERS, default.exactReminders),
            repeatReminders = sharedPreferences.getBoolean(REPEAT_REMINDERS, default.repeatReminders),
            numberOfRepetitions = sharedPreferences.getString(
                NUMBER_OF_REPETITIONS,
                default.numberOfRepetitions.toString()
            )
                ?.toInt()
                ?: 3,
            repeatDelay = (sharedPreferences.getString(REPEAT_DELAY, default.repeatDelay.inWholeMinutes.toString())
                ?.toInt()
                ?: 10).toDuration(DurationUnit.MINUTES),
            snoozeDuration = (sharedPreferences.getString(
                SNOOZE_DURATION,
                default.snoozeDuration.inWholeMinutes.toString()
            )
                ?.toInt()
                ?: 15).toDuration(DurationUnit.MINUTES),
            overrideDnd = sharedPreferences.getBoolean(OVERRIDE_DND, default.overrideDnd),
            stickyOnLockscreen = sharedPreferences.getBoolean(STICKY_ON_LOCKSCREEN, default.stickyOnLockscreen),
            dismissNotificationAction = when (sharedPreferences.getString(DISMISS_NOTIFICATION_ACTION, "0")) {
                "0" -> DismissNotificationAction.SKIP
                "1" -> DismissNotificationAction.SNOOZE
                else -> DismissNotificationAction.TAKE
            },
            bigNotifications = sharedPreferences.getBoolean(BIG_NOTIFICATIONS, default.bigNotifications),
            combineNotifications = sharedPreferences.getBoolean(COMBINE_NOTIFICATIONS, default.combineNotifications),
            useRelativeDateTime = sharedPreferences.getBoolean(USE_RELATIVE_DATE_TIME, default.useRelativeDateTime),
            showTakenTimeInOverview = sharedPreferences.getBoolean(
                SHOW_TAKEN_TIME_IN_OVERVIEW,
                default.showTakenTimeInOverview
            ),
            systemLocale = sharedPreferences.getBoolean(SYSTEM_LOCALE, default.systemLocale),
            theme = when (sharedPreferences.getString(THEME, "0")) {
                "0" -> ThemeSetting.DEFAULT
                else -> ThemeSetting.ALTERNATIVE
            },
            hideMedicineName = sharedPreferences.getBoolean(HIDE_MED_NAME, default.hideMedicineName),
            appAuthentication = sharedPreferences.getBoolean(APP_AUTHENTICATION, default.appAuthentication),
            useSecureWindow = sharedPreferences.getBoolean(SECURE_WINDOW, default.useSecureWindow),
            disableWidget = sharedPreferences.getBoolean(DISABLE_WIDGET, default.disableWidget),
            alarmRingtone = sharedPreferences.getString(ALARM_RINGTONE, null)?.toUri() ?: default.alarmRingtone,
            noAlarmSoundWhenSilent = sharedPreferences.getBoolean(
                NO_ALARM_SOUND_WHEN_SILENT,
                default.noAlarmSoundWhenSilent
            ),
            noVibrationWhenSilent = sharedPreferences.getBoolean(NO_VIBRATION_WHEN_SILENT, default.noVibrationWhenSilent),
            automaticBackupInterval = sharedPreferences.getString(AUTOMATIC_BACKUP_INTERVAL, "0")
                .let { v ->
                    when (v) {
                        "never" -> BackupInterval.NEVER
                        "daily" -> BackupInterval.DAILY
                        "weekly" -> BackupInterval.WEEKLY
                        "monthly" -> BackupInterval.MONTHLY
                        else -> BackupInterval.entries[v?.toIntOrNull() ?: 0]
                    }
                },
            automaticBackupDirectory = sharedPreferences.getString(AUTOMATIC_BACKUP_DIRECTORY, default.automaticBackupDirectory.toString())?.toUri(),
            locationBasedSnooze = sharedPreferences.getBoolean(LOCATION_SNOOZE_ENABLED, default.locationBasedSnooze),
            homeLocation = getHomeLocation()
        )
    }

    companion object {
        const val WEEKEND_TIME = "weekend_time"
        const val WEEKEND_MODE = "weekend_mode"
        const val WEEKEND_DAYS = "weekend_days"
        const val EXACT_REMINDERS = "exact_reminders"
        const val REPEAT_REMINDERS = "repeat_reminders"
        const val NUMBER_OF_REPETITIONS = "repeat_reminders_repetitions"
        const val REPEAT_DELAY = "repeat_reminders_delay"
        const val SNOOZE_DURATION = "snooze_duration"
        const val OVERRIDE_DND = "override_dnd"
        const val HIDE_MED_NAME = "hide_med_name"
        const val SECURE_WINDOW = "window_flag_secure"
        const val USE_RELATIVE_DATE_TIME = "use_relative_date_time"
        const val SYSTEM_LOCALE = "system_locale"
        const val COMBINE_NOTIFICATIONS = "combine_notifications"
        const val SHOW_TAKEN_TIME_IN_OVERVIEW = "show_taken_time_in_overview"
        const val STICKY_ON_LOCKSCREEN = "sticky_on_lockscreen"
        const val BIG_NOTIFICATIONS = "big_notifications"
        const val DISMISS_NOTIFICATION_ACTION = "dismiss_notification_action"
        const val ALARM_RINGTONE = "alarm_ringtone"
        const val NO_ALARM_SOUND_WHEN_SILENT = "no_alarm_sound_when_silent"
        const val NO_VIBRATION_WHEN_SILENT = "no_vibration_when_silent"
        const val THEME = "theme"
        const val APP_AUTHENTICATION = "app_authentication"
        const val DISABLE_WIDGET = "disable_widget"
        const val AUTOMATIC_BACKUP_INTERVAL = "automatic_backup_interval"
        const val AUTOMATIC_BACKUP_DIRECTORY = "automatic_backup_directory"
        const val LOCATION_SNOOZE_ENABLED = "location_snooze_enabled"
        const val HOME_LOCATION = "home_location"
    }
}

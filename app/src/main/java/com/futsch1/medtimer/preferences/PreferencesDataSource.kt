package com.futsch1.medtimer.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceDataStore
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.di.DefaultPreferences
import com.futsch1.medtimer.model.BackupInterval
import com.futsch1.medtimer.model.DismissNotificationAction
import com.futsch1.medtimer.model.ThemeSetting
import com.futsch1.medtimer.model.UserPreferences
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
    @param:ApplicationScope private val scope: kotlinx.coroutines.CoroutineScope
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
        sharedPreferences.edit { putInt(PreferencesNames.WEEKEND_TIME, value.toSecondOfDay() / 60) }
    }

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
                sharedPreferences.getInt(PreferencesNames.WEEKEND_TIME, default.weekendTime.toSecondOfDay() / 60) / 60,
                sharedPreferences.getInt(PreferencesNames.WEEKEND_TIME, default.weekendTime.toSecondOfDay() / 60) % 60
            ),
            weekendMode = sharedPreferences.getBoolean(PreferencesNames.WEEKEND_MODE, default.weekendMode),
            weekendDays = sharedPreferences.getStringSet(PreferencesNames.WEEKEND_DAYS, default.weekendDays) ?: emptySet(),
            exactReminders = sharedPreferences.getBoolean(PreferencesNames.EXACT_REMINDERS, default.exactReminders),
            repeatReminders = sharedPreferences.getBoolean(PreferencesNames.REPEAT_REMINDERS, default.repeatReminders),
            numberOfRepetitions = sharedPreferences.getString(
                PreferencesNames.NUMBER_OF_REPETITIONS,
                default.numberOfRepetitions.toString()
            )
                ?.toInt()
                ?: 3,
            repeatDelay = (sharedPreferences.getString(PreferencesNames.REPEAT_DELAY, default.repeatDelay.inWholeMinutes.toString())
                ?.toInt()
                ?: 10).toDuration(DurationUnit.MINUTES),
            snoozeDuration = (sharedPreferences.getString(
                PreferencesNames.SNOOZE_DURATION,
                default.snoozeDuration.inWholeMinutes.toString()
            )
                ?.toInt()
                ?: 15).toDuration(DurationUnit.MINUTES),
            overrideDnd = sharedPreferences.getBoolean(PreferencesNames.OVERRIDE_DND, default.overrideDnd),
            stickyOnLockscreen = sharedPreferences.getBoolean(PreferencesNames.STICKY_ON_LOCKSCREEN, default.stickyOnLockscreen),
            dismissNotificationAction = when (sharedPreferences.getString(PreferencesNames.DISMISS_NOTIFICATION_ACTION, "0")) {
                "0" -> DismissNotificationAction.SKIP
                "1" -> DismissNotificationAction.SNOOZE
                else -> DismissNotificationAction.TAKE
            },
            bigNotifications = sharedPreferences.getBoolean(PreferencesNames.BIG_NOTIFICATIONS, default.bigNotifications),
            combineNotifications = sharedPreferences.getBoolean(PreferencesNames.COMBINE_NOTIFICATIONS, default.combineNotifications),
            useRelativeDateTime = sharedPreferences.getBoolean(PreferencesNames.USE_RELATIVE_DATE_TIME, default.useRelativeDateTime),
            showTakenTimeInOverview = sharedPreferences.getBoolean(
                PreferencesNames.SHOW_TAKEN_TIME_IN_OVERVIEW,
                default.showTakenTimeInOverview
            ),
            systemLocale = sharedPreferences.getBoolean(PreferencesNames.SYSTEM_LOCALE, default.systemLocale),
            theme = when (sharedPreferences.getString(PreferencesNames.THEME, "0")) {
                "0" -> ThemeSetting.DEFAULT
                else -> ThemeSetting.ALTERNATIVE
            },
            hideMedicineName = sharedPreferences.getBoolean(PreferencesNames.HIDE_MED_NAME, default.hideMedicineName),
            appAuthentication = sharedPreferences.getBoolean(PreferencesNames.APP_AUTHENTICATION, default.appAuthentication),
            useSecureWindow = sharedPreferences.getBoolean(PreferencesNames.SECURE_WINDOW, default.useSecureWindow),
            alarmRingtone = sharedPreferences.getString(PreferencesNames.ALARM_RINGTONE, null)?.toUri() ?: default.alarmRingtone,
            noAlarmSoundWhenSilent = sharedPreferences.getBoolean(
                PreferencesNames.NO_ALARM_SOUND_WHEN_SILENT,
                default.noAlarmSoundWhenSilent
            ),
            noVibrationWhenSilent = sharedPreferences.getBoolean(PreferencesNames.NO_VIBRATION_WHEN_SILENT, default.noVibrationWhenSilent),
            automaticBackupInterval = when (sharedPreferences.getString(PreferencesNames.AUTOMATIC_BACKUP_INTERVAL, "0")) {
                "0" -> BackupInterval.NEVER
                "1" -> BackupInterval.DAILY
                "2" -> BackupInterval.WEEKLY
                else -> BackupInterval.MONTHLY
            }
        )
    }
}

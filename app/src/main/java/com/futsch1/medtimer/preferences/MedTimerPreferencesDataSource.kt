package com.futsch1.medtimer.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceDataStore
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.di.DefaultPrefs
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MedTimerPreferencesDataSource @Inject constructor(
    @param:DefaultPrefs private val sharedPreferences: SharedPreferences,
    @param:ApplicationScope private val scope: kotlinx.coroutines.CoroutineScope
) : PreferenceDataStore() {
    val data: StateFlow<MedTimerSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(getSettings())
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        trySend(getSettings())

        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.stateIn(scope, started = SharingStarted.Eagerly, initialValue = getSettings())

    fun setWeekendTime(value: Int) {
        sharedPreferences.edit { putInt(PreferencesNames.WEEKEND_TIME, value) }
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

    private fun getSettings(): MedTimerSettings {
        return MedTimerSettings(
            weekendTime = LocalTime.of(
                sharedPreferences.getInt(PreferencesNames.WEEKEND_TIME, DEFAULT_MEDTIMER_SETTINGS.weekendTime.toSecondOfDay() / 60) % 60,
                sharedPreferences.getInt(PreferencesNames.WEEKEND_TIME, DEFAULT_MEDTIMER_SETTINGS.weekendTime.toSecondOfDay() / 60) / 60
            ),
            weekendMode = sharedPreferences.getBoolean(PreferencesNames.WEEKEND_MODE, DEFAULT_MEDTIMER_SETTINGS.weekendMode),
            weekendDays = sharedPreferences.getStringSet(PreferencesNames.WEEKEND_DAYS, DEFAULT_MEDTIMER_SETTINGS.weekendDays) ?: emptySet(),
            exactReminders = sharedPreferences.getBoolean(PreferencesNames.EXACT_REMINDERS, DEFAULT_MEDTIMER_SETTINGS.exactReminders),
            repeatReminders = sharedPreferences.getBoolean(PreferencesNames.REPEAT_REMINDERS, DEFAULT_MEDTIMER_SETTINGS.repeatReminders),
            numberOfRepetitions = sharedPreferences.getString(PreferencesNames.NUMBER_OF_REPETITIONS, DEFAULT_MEDTIMER_SETTINGS.numberOfRepetitions.toString())
                ?.toInt()
                ?: 3,
            repeatDelay = (sharedPreferences.getString(PreferencesNames.REPEAT_DELAY, DEFAULT_MEDTIMER_SETTINGS.repeatDelay.inWholeMinutes.toString())?.toInt()
                ?: 10).toDuration(DurationUnit.MINUTES),
            snoozeDuration = (sharedPreferences.getString(PreferencesNames.SNOOZE_DURATION, DEFAULT_MEDTIMER_SETTINGS.snoozeDuration.inWholeMinutes.toString())
                ?.toInt()
                ?: 15).toDuration(DurationUnit.MINUTES),
            overrideDnd = sharedPreferences.getBoolean(PreferencesNames.OVERRIDE_DND, DEFAULT_MEDTIMER_SETTINGS.overrideDnd),
            stickyOnLockscreen = sharedPreferences.getBoolean(PreferencesNames.STICKY_ON_LOCKSCREEN, DEFAULT_MEDTIMER_SETTINGS.overrideDnd),
            bigNotifications = sharedPreferences.getBoolean(PreferencesNames.BIG_NOTIFICATIONS, DEFAULT_MEDTIMER_SETTINGS.bigNotifications),
            dismissNotificationAction = when (sharedPreferences.getString(PreferencesNames.DISMISS_NOTIFICATION_ACTION, "0")) {
                "0" -> DismissNotificationAction.SKIP
                "1" -> DismissNotificationAction.SNOOZE
                else -> DismissNotificationAction.TAKE
            },
            combineNotifications = sharedPreferences.getBoolean(PreferencesNames.COMBINE_NOTIFICATIONS, DEFAULT_MEDTIMER_SETTINGS.combineNotifications),
            hideMedicineName = sharedPreferences.getBoolean(PreferencesNames.HIDE_MED_NAME, DEFAULT_MEDTIMER_SETTINGS.hideMedicineName)
        )
    }
}

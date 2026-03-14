package com.futsch1.medtimer.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceDataStore
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.di.DefaultPrefs
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class MedTimerPreferencesDataSource @Inject constructor(
    @param:DefaultPrefs private val sharedPreferences: SharedPreferences,
    @param:ApplicationScope private val scope: kotlinx.coroutines.CoroutineScope
) : PreferenceDataStore() {
    val data: Flow<MedTimerSettings> = callbackFlow {
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

    fun setWeekendMode(value: Boolean) {
        sharedPreferences.edit { putBoolean(PreferencesNames.WEEKEND_MODE, value) }
    }

    fun setWeekendDays(value: Set<String>) {
        sharedPreferences.edit { putStringSet(PreferencesNames.WEEKEND_DAYS, value) }
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

    private fun getSettings(): MedTimerSettings {
        return MedTimerSettings(
            weekendTime = sharedPreferences.getInt(PreferencesNames.WEEKEND_TIME, 540),
            weekendMode = sharedPreferences.getBoolean(PreferencesNames.WEEKEND_MODE, false),
            weekendDays = sharedPreferences.getStringSet(PreferencesNames.WEEKEND_DAYS, emptySet()) ?: emptySet()
        )
    }
}

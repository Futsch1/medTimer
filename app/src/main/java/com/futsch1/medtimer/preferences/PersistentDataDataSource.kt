package com.futsch1.medtimer.preferences

import android.content.SharedPreferences
import android.graphics.Color
import androidx.core.content.edit
import androidx.core.net.toUri
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.di.DefaultPreferences
import com.futsch1.medtimer.di.MedTimerPreferencess
import com.futsch1.medtimer.model.PersistentData
import com.futsch1.medtimer.model.StatisticFragment
import com.futsch1.medtimer.preferences.PreferencesNames.ACTIVE_STATISTICS_FRAGMENT
import com.futsch1.medtimer.preferences.PreferencesNames.ANALYSIS_DAYS
import com.futsch1.medtimer.preferences.PreferencesNames.AUTOMATIC_BACKUP_DIRECTORY
import com.futsch1.medtimer.preferences.PreferencesNames.BATTERY_WARNING_DISMISSED
import com.futsch1.medtimer.preferences.PreferencesNames.ICON_COLOR
import com.futsch1.medtimer.preferences.PreferencesNames.LAST_AUTOMATIC_BACKUP
import com.futsch1.medtimer.preferences.PreferencesNames.LAST_CUSTOM_DOSE
import com.futsch1.medtimer.preferences.PreferencesNames.LAST_CUSTOM_DOSE_AMOUNT
import com.futsch1.medtimer.preferences.PreferencesNames.NOTIFICATION_ID
import com.futsch1.medtimer.preferences.PreferencesNames.SHOW_NOTIFICATION
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

class PersistentDataDataSource @Inject constructor(
    @param:DefaultPreferences private val defaultSharedPreferences: SharedPreferences,
    @param:MedTimerPreferencess private val medTimerSharedPreferences: SharedPreferences,
    @param:ApplicationScope private val scope: kotlinx.coroutines.CoroutineScope
) {

    val data: StateFlow<PersistentData> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(getPersistentData())
        }

        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        medTimerSharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            medTimerSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.stateIn(scope, started = SharingStarted.Eagerly, initialValue = getPersistentData())

    fun setLastCustomDose(value: String) {
        medTimerSharedPreferences.edit { putString(LAST_CUSTOM_DOSE, value) }
    }

    fun setLastCustomDoseAmount(value: String) {
        medTimerSharedPreferences.edit { putString(LAST_CUSTOM_DOSE_AMOUNT, value) }
    }

    fun setActiveStatisticsFragment(fragment: StatisticFragment) {
        defaultSharedPreferences.edit { putInt(ACTIVE_STATISTICS_FRAGMENT, fragment.ordinal) }
    }

    private fun getPersistentData(): PersistentData {
        val default = PersistentData.default()
        return PersistentData(
            showNotifications = defaultSharedPreferences.getBoolean(SHOW_NOTIFICATION, default.showNotifications),
            iconColor = Color.valueOf(defaultSharedPreferences.getInt(ICON_COLOR, default.iconColor.toArgb())),
            activeStatisticsFragment = when (defaultSharedPreferences.getInt(ACTIVE_STATISTICS_FRAGMENT, default.activeStatisticsFragment.ordinal)) {
                StatisticFragment.CHARTS.ordinal -> StatisticFragment.CHARTS
                StatisticFragment.TABLE.ordinal -> StatisticFragment.TABLE
                else -> StatisticFragment.CALENDAR
            },
            analysisDays = defaultSharedPreferences.getInt(ANALYSIS_DAYS, default.analysisDays),
            batteryWarningDismissed = defaultSharedPreferences.getBoolean(BATTERY_WARNING_DISMISSED, default.batteryWarningDismissed),
            lastAutomaticBackup = LocalDate.parse(defaultSharedPreferences.getString(LAST_AUTOMATIC_BACKUP, null) ?: default.lastAutomaticBackup.toString()),
            automaticBackupDirectory = defaultSharedPreferences.getString(AUTOMATIC_BACKUP_DIRECTORY, default.automaticBackupDirectory.toString())?.toUri(),
            notificationId = medTimerSharedPreferences.getInt(NOTIFICATION_ID, default.notificationId),
            lastCustomDose = medTimerSharedPreferences.getString(LAST_CUSTOM_DOSE, null) ?: default.lastCustomDose,
            lastCustomDoseAmount = medTimerSharedPreferences.getString(LAST_CUSTOM_DOSE_AMOUNT, null) ?: default.lastCustomDoseAmount
        )
    }
}
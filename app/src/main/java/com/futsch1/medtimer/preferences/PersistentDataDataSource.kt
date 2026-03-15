package com.futsch1.medtimer.preferences

import android.content.SharedPreferences
import android.graphics.Color
import androidx.core.net.toUri
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.di.DefaultPrefs
import com.futsch1.medtimer.di.MedTimerPrefs
import com.futsch1.medtimer.model.MedTimerPersistentData
import com.futsch1.medtimer.model.StatisticFragment
import com.futsch1.medtimer.preferences.PreferencesNames.ACTIVE_STATISTICS_FRAGMENT
import com.futsch1.medtimer.preferences.PreferencesNames.ANALYSIS_DAYS
import com.futsch1.medtimer.preferences.PreferencesNames.AUTOMATIC_BACKUP_DIRECTORY
import com.futsch1.medtimer.preferences.PreferencesNames.BATTERY_WARNING_DISMISSED
import com.futsch1.medtimer.preferences.PreferencesNames.ICON_COLOR
import com.futsch1.medtimer.preferences.PreferencesNames.LAST_AUTOMATIC_BACKUP
import com.futsch1.medtimer.preferences.PreferencesNames.SHOW_NOTIFICATION
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

class PersistentDataDataSource @Inject constructor(
    @param:DefaultPrefs private val defaultSharedPreferences: SharedPreferences,
    @param:MedTimerPrefs private val medTimerSharedPreferences: SharedPreferences,
    @param:ApplicationScope private val scope: kotlinx.coroutines.CoroutineScope
) {

    val data: StateFlow<MedTimerPersistentData> = callbackFlow {
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

    private fun getPersistentData(): MedTimerPersistentData {
        val default = MedTimerPersistentData.default()
        return MedTimerPersistentData(
            showNotifications = defaultSharedPreferences.getBoolean(SHOW_NOTIFICATION, default.showNotifications),
            iconColor = Color.valueOf(defaultSharedPreferences.getInt(ICON_COLOR, default.iconColor.toArgb())),
            activeStatisticsFragment = when (defaultSharedPreferences.getInt(ACTIVE_STATISTICS_FRAGMENT, default.activeStatisticsFragment.ordinal)) {
                StatisticFragment.CHARTS.ordinal -> StatisticFragment.CHARTS
                StatisticFragment.TABLE.ordinal -> StatisticFragment.TABLE
                else -> StatisticFragment.CALENDAR
            },
            analysisDays = defaultSharedPreferences.getInt(ANALYSIS_DAYS, default.analysisDays),
            batteryWarningDismissed = defaultSharedPreferences.getBoolean(BATTERY_WARNING_DISMISSED, default.batteryWarningDismissed),
            lastAutomaticBackup = Instant.ofEpochMilli(defaultSharedPreferences.getLong(LAST_AUTOMATIC_BACKUP, default.lastAutomaticBackup.toEpochMilli())),
            automaticBackupDirectory = defaultSharedPreferences.getString(AUTOMATIC_BACKUP_DIRECTORY, default.automaticBackupDirectory.toString())?.toUri()
        )
    }
}
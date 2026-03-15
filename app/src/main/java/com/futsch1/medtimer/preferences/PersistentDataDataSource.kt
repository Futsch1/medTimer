package com.futsch1.medtimer.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.di.DefaultPreferences
import com.futsch1.medtimer.di.MedTimerPreferencess
import com.futsch1.medtimer.model.OverviewFilter
import com.futsch1.medtimer.model.PersistentData
import com.futsch1.medtimer.model.StatisticFragment
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

    fun setLastCustomDose(lastCustomDose: String) {
        medTimerSharedPreferences.edit { putString(LAST_CUSTOM_DOSE, lastCustomDose) }
    }

    fun setLastCustomDoseAmount(lastCustomDoseAmount: String) {
        medTimerSharedPreferences.edit { putString(LAST_CUSTOM_DOSE_AMOUNT, lastCustomDoseAmount) }
    }

    fun setActiveStatisticsFragment(fragment: StatisticFragment) {
        defaultSharedPreferences.edit { putInt(ACTIVE_STATISTICS_FRAGMENT, fragment.ordinal) }
    }

    fun setAnalysisDays(days: Int) {
        defaultSharedPreferences.edit { putInt(ANALYSIS_DAYS, days) }
    }

    fun setIntroShown(introShown: Boolean) {
        defaultSharedPreferences.edit { putBoolean(INTRO_SHOWN, introShown) }
    }

    fun setBatteryWarningShown(batteryWarningShown: Boolean) {
        defaultSharedPreferences.edit { putBoolean(BATTERY_WARNING_SHOWN, batteryWarningShown) }
    }

    fun setLastAutomaticBackup(localDate: LocalDate) {
        defaultSharedPreferences.edit { putString(LAST_AUTOMATIC_BACKUP, localDate.toString()) }
    }

    fun increaseNotificationId() {
        medTimerSharedPreferences.edit { putInt(NOTIFICATION_ID, data.value.notificationId + 1) }
    }

    fun setFilterTags(filterTags: Set<String>) {
        medTimerSharedPreferences.edit { putStringSet(FILTER_TAGS, filterTags) }
    }

    fun setIconColor(iconColor: Int) {
        defaultSharedPreferences.edit { putInt(ICON_COLOR, iconColor) }
    }

    fun setCheckedFilters(checkedFilters: Set<OverviewFilter>) {
        medTimerSharedPreferences.edit { putStringSet(CHECKED_FILTERS, checkedFilters.map { it.toString() }.toSet()) }
    }

    fun setShowNotifications(showNotifications: Boolean) {
        medTimerSharedPreferences.edit { putBoolean(SHOW_NOTIFICATION, showNotifications) }
    }

    private fun getPersistentData(): PersistentData {
        val default = PersistentData.default()
        return PersistentData(
            showNotifications = defaultSharedPreferences.getBoolean(SHOW_NOTIFICATION, default.showNotifications),
            iconColor = defaultSharedPreferences.getInt(ICON_COLOR, default.iconColor),
            activeStatisticsFragment = when (defaultSharedPreferences.getInt(ACTIVE_STATISTICS_FRAGMENT, default.activeStatisticsFragment.ordinal)) {
                StatisticFragment.CHARTS.ordinal -> StatisticFragment.CHARTS
                StatisticFragment.TABLE.ordinal -> StatisticFragment.TABLE
                else -> StatisticFragment.CALENDAR
            },
            analysisDays = defaultSharedPreferences.getInt(ANALYSIS_DAYS, default.analysisDays),
            batteryWarningShown = defaultSharedPreferences.getBoolean(BATTERY_WARNING_SHOWN, default.batteryWarningShown),
            introShown = defaultSharedPreferences.getBoolean(INTRO_SHOWN, default.introShown),
            lastAutomaticBackup = LocalDate.parse(defaultSharedPreferences.getString(LAST_AUTOMATIC_BACKUP, null) ?: default.lastAutomaticBackup.toString()),
            notificationId = medTimerSharedPreferences.getInt(NOTIFICATION_ID, default.notificationId),
            lastCustomDose = medTimerSharedPreferences.getString(LAST_CUSTOM_DOSE, null) ?: default.lastCustomDose,
            lastCustomDoseAmount = medTimerSharedPreferences.getString(LAST_CUSTOM_DOSE_AMOUNT, null) ?: default.lastCustomDoseAmount,
            filterTags = medTimerSharedPreferences.getStringSet(FILTER_TAGS, emptySet()) ?: emptySet(),
            checkedFilters = medTimerSharedPreferences.getStringSet(CHECKED_FILTERS, emptySet())?.map { OverviewFilter.valueOf(it) }?.toSet() ?: emptySet()
        )
    }

    companion object {
        const val SHOW_NOTIFICATION = "show_notification"
        const val ICON_COLOR = "icon_color"
        const val ACTIVE_STATISTICS_FRAGMENT = "active_statistics_fragment"
        const val ANALYSIS_DAYS = "analysis_days"
        const val BATTERY_WARNING_SHOWN = "battery_warning_dismissed"
        const val INTRO_SHOWN = "intro_shown"
        const val LAST_AUTOMATIC_BACKUP = "last_automatic_backup"
        const val NOTIFICATION_ID = "notificationId"
        const val LAST_CUSTOM_DOSE = "lastCustomDose"
        const val LAST_CUSTOM_DOSE_AMOUNT = "lastCustomDoseAmount"
        const val FILTER_TAGS = "filterTags"
        const val CHECKED_FILTERS = "checkedFilters"

    }
}
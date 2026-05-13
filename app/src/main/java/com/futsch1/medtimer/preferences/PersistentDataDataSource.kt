package com.futsch1.medtimer.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.di.DefaultPreferences
import com.futsch1.medtimer.di.MedTimerPreferencess
import com.futsch1.medtimer.model.OverviewFilter
import com.futsch1.medtimer.model.PersistentData
import com.futsch1.medtimer.model.StatisticFragment
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
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
    @param:ApplicationScope private val scope: kotlinx.coroutines.CoroutineScope,
    private val gson: Gson
) {
    private data class SerializablePendingSnooze(
        val reminderIds: List<Int>,
        val reminderEventIds: List<Int>,
        val notificationId: Int,
        val remindInstantEpochSecond: Long
    )

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

    @Synchronized
    fun getAndIncreaseNotificationId(): Int {
        val current = medTimerSharedPreferences.getInt(NOTIFICATION_ID, PersistentData.default().notificationId)
        medTimerSharedPreferences.edit { putInt(NOTIFICATION_ID, current + 1) }
        return current
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

    fun addPendingLocationSnooze(data: ReminderNotificationData) {
        val current = getPendingSnoozeList().toMutableList()
        current.add(data.toSerializable())
        medTimerSharedPreferences.edit { putString(PENDING_SNOOZES, gson.toJson(current)) }
    }

    fun getPendingLocationSnoozes(): List<ReminderNotificationData> =
        getPendingSnoozeList().map { it.toReminderNotificationData() }

    fun clearAllPendingLocationSnoozes() {
        medTimerSharedPreferences.edit { remove(PENDING_SNOOZES) }
    }

    fun removePendingLocationSnoozesForReminderEventIds(reminderEventIds: List<Int>) {
        val current = getPendingSnoozeList()
        val updated = current.mapNotNull { snooze ->
            val keptIndices = snooze.reminderEventIds.indices.filter { i -> snooze.reminderEventIds[i] !in reminderEventIds }
            when {
                keptIndices.isEmpty() -> null
                keptIndices.size == snooze.reminderEventIds.size -> snooze
                else -> snooze.copy(
                    reminderIds = keptIndices.map { snooze.reminderIds[it] },
                    reminderEventIds = keptIndices.map { snooze.reminderEventIds[it] }
                )
            }
        }
        if (updated != current) {
            if (updated.isEmpty()) {
                medTimerSharedPreferences.edit { remove(PENDING_SNOOZES) }
            } else {
                medTimerSharedPreferences.edit { putString(PENDING_SNOOZES, gson.toJson(updated)) }
            }
        }
    }

    private fun getPendingSnoozeList(): List<SerializablePendingSnooze> {
        val json = medTimerSharedPreferences.getString(PENDING_SNOOZES, null) ?: return emptyList()
        val type = object : TypeToken<List<SerializablePendingSnooze>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun ReminderNotificationData.toSerializable() = SerializablePendingSnooze(
        reminderIds = reminderIds,
        reminderEventIds = reminderEventIds,
        notificationId = notificationId,
        remindInstantEpochSecond = remindInstant.epochSecond
    )

    private fun SerializablePendingSnooze.toReminderNotificationData() = ReminderNotificationData(
        remindInstant = Instant.ofEpochSecond(remindInstantEpochSecond),
        reminderIds = reminderIds,
        reminderEventIds = reminderEventIds,
        notificationId = notificationId
    )

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
        const val PENDING_SNOOZES = "pending_location_snoozes"
    }
}
package com.futsch1.medtimer.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import com.futsch1.medtimer.di.MedTimerPreferencess
import com.futsch1.medtimer.model.HomeLocation
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject

class HomeLocationStore @Inject constructor(
    @param:MedTimerPreferencess private val prefs: SharedPreferences,
    private val gson: Gson
) {
    private data class SerializablePendingSnooze(
        val reminderIds: List<Int>,
        val reminderEventIds: List<Int>,
        val notificationId: Int,
        val remindInstantEpochSecond: Long
    )

    fun saveHomeLocation(location: HomeLocation) {
        prefs.edit { putString(KEY_HOME_LOCATION, gson.toJson(location)) }
    }

    fun getHomeLocation(): HomeLocation? {
        val json = prefs.getString(KEY_HOME_LOCATION, null) ?: return null
        return gson.fromJson(json, HomeLocation::class.java)
    }

    fun clearHomeLocation() {
        prefs.edit { remove(KEY_HOME_LOCATION) }
    }

    fun addPendingLocationSnooze(data: ReminderNotificationData) {
        val current = getPendingSnoozeList().toMutableList()
        current.add(data.toSerializable())
        prefs.edit { putString(KEY_PENDING_SNOOZES, gson.toJson(current)) }
    }

    fun getPendingLocationSnoozes(): List<ReminderNotificationData> =
        getPendingSnoozeList().map { it.toReminderNotificationData() }

    fun clearAllPendingLocationSnoozes() {
        prefs.edit { remove(KEY_PENDING_SNOOZES) }
    }

    private fun getPendingSnoozeList(): List<SerializablePendingSnooze> {
        val json = prefs.getString(KEY_PENDING_SNOOZES, null) ?: return emptyList()
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

    companion object {
        private const val KEY_HOME_LOCATION = "home_location"
        private const val KEY_PENDING_SNOOZES = "pending_location_snoozes"
    }
}
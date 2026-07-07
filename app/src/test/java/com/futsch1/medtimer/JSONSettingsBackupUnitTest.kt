package com.futsch1.medtimer

import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.BackupInterval
import com.futsch1.medtimer.core.domain.model.DismissNotificationAction
import com.futsch1.medtimer.core.domain.model.HomeLocation
import com.futsch1.medtimer.core.domain.model.ThemeSetting
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.database.backup.JSONSettingsBackup
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class JSONSettingsBackupUnitTest {

    private fun buildPreferences(): UserPreferences = UserPreferences(
        weekendStartTime = LocalTime.of(7, 30),
        weekendEndTime = LocalTime.of(10, 0),
        weekendMode = true,
        weekendDays = setOf("1", "7"),
        exactReminders = true,
        repeatReminders = true,
        numberOfRepetitions = 2,
        repeatDelay = 5.toDuration(DurationUnit.MINUTES),
        snoozeDuration = 20.toDuration(DurationUnit.MINUTES),
        overrideDnd = true,
        stickyOnLockscreen = true,
        dismissNotificationAction = DismissNotificationAction.SNOOZE,
        cannotSkipReminders = true,
        bigNotifications = true,
        combineNotifications = true,
        useRelativeDateTime = true,
        showTakenTimeInOverview = false,
        systemLocale = true,
        theme = ThemeSetting.ALTERNATIVE,
        hideMedicineName = true,
        appAuthentication = true,
        useSecureWindow = true,
        disableWidget = true,
        alarmRingtone = null,
        noAlarmSoundWhenSilent = true,
        noVibrationWhenSilent = true,
        automaticBackupInterval = BackupInterval.WEEKLY,
        automaticBackupDirectory = null,
        locationBasedSnooze = true,
        homeLocation = HomeLocation(48.137, 11.576, 200f),
        prescriptionPickupDays = 5
    )

    @Test
    fun testRoundTrip() {
        val mockPrefs = mock<PreferencesDataSource>()
        val prefs = buildPreferences()
        whenever(mockPrefs.preferences).thenReturn(MutableStateFlow(prefs))

        val backup = JSONSettingsBackup(mockPrefs)
        val json = Gson().toJson(backup.createBackup())

        val mockPrefs2 = mock<PreferencesDataSource>()
        val backup2 = JSONSettingsBackup(mockPrefs2)
        assertTrue(backup2.applyBackup(json))

        verify(mockPrefs2).putBoolean(PreferencesDataSource.WEEKEND_MODE, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.EXACT_REMINDERS, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.REPEAT_REMINDERS, true)
        verify(mockPrefs2).putString(PreferencesDataSource.NUMBER_OF_REPETITIONS, "2")
        verify(mockPrefs2).putString(PreferencesDataSource.REPEAT_DELAY, "5")
        verify(mockPrefs2).putString(PreferencesDataSource.SNOOZE_DURATION, "20")
        verify(mockPrefs2).putBoolean(PreferencesDataSource.OVERRIDE_DND, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.STICKY_ON_LOCKSCREEN, true)
        verify(mockPrefs2).putString(
            PreferencesDataSource.DISMISS_NOTIFICATION_ACTION,
            DismissNotificationAction.SNOOZE.ordinal.toString()
        )
        verify(mockPrefs2).putBoolean(PreferencesDataSource.CANNOT_SKIP_REMINDERS, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.BIG_NOTIFICATIONS, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.COMBINE_NOTIFICATIONS, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.USE_RELATIVE_DATE_TIME, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.SHOW_TAKEN_TIME_IN_OVERVIEW, false)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.SYSTEM_LOCALE, true)
        verify(mockPrefs2).putString(
            PreferencesDataSource.THEME,
            ThemeSetting.ALTERNATIVE.ordinal.toString()
        )
        verify(mockPrefs2).putBoolean(PreferencesDataSource.HIDE_MED_NAME, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.APP_AUTHENTICATION, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.SECURE_WINDOW, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.DISABLE_WIDGET, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.NO_ALARM_SOUND_WHEN_SILENT, true)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.NO_VIBRATION_WHEN_SILENT, true)
        verify(mockPrefs2).setAutomaticBackupInterval(BackupInterval.WEEKLY)
        verify(mockPrefs2).putBoolean(PreferencesDataSource.LOCATION_SNOOZE_ENABLED, true)
        verify(mockPrefs2).saveHomeLocation(HomeLocation(48.137, 11.576, 200f))
        verify(mockPrefs2).setWeekendStartTime(LocalTime.of(7, 30))
        verify(mockPrefs2).setWeekendEndTime(LocalTime.of(10, 0))
        verify(mockPrefs2).putStringSet(PreferencesDataSource.WEEKEND_DAYS, setOf("1", "7"))
    }

    @Test
    fun testInvalidJsonReturnsFalse() {
        val mockPrefs = mock<PreferencesDataSource>()
        val backup = JSONSettingsBackup(mockPrefs)
        assertFalse(backup.applyBackup("not valid json {{{"))
    }

    @Test
    fun testEmptyJsonReturnsFalse() {
        val mockPrefs = mock<PreferencesDataSource>()
        val backup = JSONSettingsBackup(mockPrefs)
        assertFalse(backup.applyBackup("{}"))
    }

    @Test
    fun testNoHomeLocationClearsIt() {
        val mockPrefs = mock<PreferencesDataSource>()
        val prefs = buildPreferences().copy(homeLocation = null)
        whenever(mockPrefs.preferences).thenReturn(MutableStateFlow(prefs))

        val backup = JSONSettingsBackup(mockPrefs)
        val json = Gson().toJson(backup.createBackup())

        val mockPrefs2 = mock<PreferencesDataSource>()
        val backup2 = JSONSettingsBackup(mockPrefs2)
        assertTrue(backup2.applyBackup(json))

        verify(mockPrefs2).clearHomeLocation()
    }
}

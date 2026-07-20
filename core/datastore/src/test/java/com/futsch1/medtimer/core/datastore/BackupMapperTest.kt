package com.futsch1.medtimer.core.datastore

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.core.domain.backup.SettingsBackup
import com.futsch1.medtimer.core.domain.model.BackupInterval
import com.futsch1.medtimer.core.domain.model.DismissNotificationAction
import com.futsch1.medtimer.core.domain.model.HomeLocation
import com.futsch1.medtimer.core.domain.model.ThemeSetting
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupMapperTest {

    @Test
    fun `toSettingsBackup maps all fields from non-default preferences`() {
        val prefs = UserPreferences(
            weekendStartTime = LocalTime.of(8, 30),
            weekendEndTime = LocalTime.of(22, 0),
            weekendMode = true,
            weekendDays = setOf("SATURDAY", "SUNDAY"),
            exactReminders = true,
            repeatReminders = true,
            numberOfRepetitions = 5,
            repeatDelay = 20.minutes,
            snoozeDuration = 30.minutes,
            overrideDnd = true,
            stickyOnLockscreen = true,
            dismissNotificationAction = DismissNotificationAction.TAKE,
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
            alarmRingtone = Uri.parse("content://settings/ringtone"),
            noAlarmSoundWhenSilent = true,
            noVibrationWhenSilent = true,
            automaticBackupInterval = BackupInterval.WEEKLY,
            automaticBackupDirectory = Uri.parse("content://backup/location"),
            locationBasedSnooze = true,
            homeLocation = HomeLocation(48.137, 11.575, 200f)
        )

        val backup = prefs.toSettingsBackup()

        assertEquals(510, backup.weekendStartTimeMinutes)    // 8*60+30
        assertEquals(1320, backup.weekendEndTimeMinutes)     // 22*60
        assertEquals(true, backup.weekendMode)
        assertEquals(setOf("SATURDAY", "SUNDAY"), backup.weekendDays)
        assertEquals(true, backup.exactReminders)
        assertEquals(true, backup.repeatReminders)
        assertEquals(5, backup.numberOfRepetitions)
        assertEquals(20L, backup.repeatDelayMinutes)
        assertEquals(30L, backup.snoozeDurationMinutes)
        assertEquals(true, backup.overrideDnd)
        assertEquals(true, backup.stickyOnLockscreen)
        assertEquals("TAKE", backup.dismissNotificationAction)
        assertEquals(true, backup.cannotSkipReminders)
        assertEquals(true, backup.bigNotifications)
        assertEquals(true, backup.combineNotifications)
        assertEquals(true, backup.useRelativeDateTime)
        assertEquals(false, backup.showTakenTimeInOverview)
        assertEquals(true, backup.systemLocale)
        assertEquals("ALTERNATIVE", backup.theme)
        assertEquals(true, backup.hideMedicineName)
        assertEquals(true, backup.appAuthentication)
        assertEquals(true, backup.useSecureWindow)
        assertEquals(true, backup.disableWidget)
        assertEquals("content://settings/ringtone", backup.alarmRingtone)
        assertEquals(true, backup.noAlarmSoundWhenSilent)
        assertEquals(true, backup.noVibrationWhenSilent)
        assertEquals("WEEKLY", backup.automaticBackupInterval)
        assertEquals("content://backup/location", backup.automaticBackupDirectory)
        assertEquals(true, backup.locationBasedSnooze)
        assertEquals(48.137, backup.homeLatitude!!, 0.0001)
        assertEquals(11.575, backup.homeLongitude!!, 0.0001)
        assertEquals(200f, backup.homeRadiusMeters!!, 0.01f)
    }

    @Test
    fun `toSettingsBackup maps default preferences`() {
        val prefs = UserPreferences.default()
        val backup = prefs.toSettingsBackup()

        assertEquals(0, backup.weekendStartTimeMinutes)
        assertEquals(540, backup.weekendEndTimeMinutes)  // 9*60
        assertEquals(false, backup.weekendMode)
        assertEquals(emptySet<String>(), backup.weekendDays)
        assertEquals(false, backup.exactReminders)
        assertEquals(false, backup.repeatReminders)
        assertEquals(3, backup.numberOfRepetitions)
        assertEquals(10L, backup.repeatDelayMinutes)
        assertEquals(15L, backup.snoozeDurationMinutes)
        assertEquals(false, backup.overrideDnd)
        assertEquals(false, backup.stickyOnLockscreen)
        assertEquals("SKIP", backup.dismissNotificationAction)
        assertEquals(false, backup.cannotSkipReminders)
        assertEquals(false, backup.bigNotifications)
        assertEquals(false, backup.combineNotifications)
        assertEquals(false, backup.useRelativeDateTime)
        assertEquals(true, backup.showTakenTimeInOverview)
        assertEquals(false, backup.systemLocale)
        assertEquals("DEFAULT", backup.theme)
        assertEquals(false, backup.hideMedicineName)
        assertEquals(false, backup.appAuthentication)
        assertEquals(false, backup.useSecureWindow)
        assertEquals(false, backup.disableWidget)
        assertEquals(false, backup.noAlarmSoundWhenSilent)
        assertEquals(false, backup.noVibrationWhenSilent)
        assertEquals("NEVER", backup.automaticBackupInterval)
        assertEquals(null, backup.automaticBackupDirectory)
        assertEquals(false, backup.locationBasedSnooze)
        assertNull(backup.homeLatitude)
        assertNull(backup.homeLongitude)
        assertNull(backup.homeRadiusMeters)
    }

    @Test
    fun `toSettingsBackup with null home location maps to null coordinates`() {
        val prefs = UserPreferences.default().copy(homeLocation = null)
        val backup = prefs.toSettingsBackup()
        assertNull(backup.homeLatitude)
        assertNull(backup.homeLongitude)
        assertNull(backup.homeRadiusMeters)
    }

    @Test
    fun `toSettingsBackup with null alarm ringtone maps to null string`() {
        val prefs = UserPreferences.default().copy(alarmRingtone = null)
        val backup = prefs.toSettingsBackup()
        assertNull(backup.alarmRingtone)
    }

    @Test
    fun `toSettingsBackup with null backup directory maps to null string`() {
        val prefs = UserPreferences.default().copy(automaticBackupDirectory = null)
        val backup = prefs.toSettingsBackup()
        assertNull(backup.automaticBackupDirectory)
    }

    @Test
    fun `toSettingsBackup with all enum values maps name strings correctly`() {
        // DismissNotificationAction
        for (action in DismissNotificationAction.entries) {
            val prefs = UserPreferences.default().copy(dismissNotificationAction = action)
            assertEquals(action.name, prefs.toSettingsBackup().dismissNotificationAction)
        }

        // ThemeSetting
        for (theme in ThemeSetting.entries) {
            val prefs = UserPreferences.default().copy(theme = theme)
            assertEquals(theme.name, prefs.toSettingsBackup().theme)
        }

        // BackupInterval
        for (interval in BackupInterval.entries) {
            val prefs = UserPreferences.default().copy(automaticBackupInterval = interval)
            assertEquals(interval.name, prefs.toSettingsBackup().automaticBackupInterval)
        }
    }

    @Test
    fun `toSettingsBackup time conversion is correct`() {
        // Edge case: midnight
        val midnight = UserPreferences.default().copy(
            weekendStartTime = LocalTime.of(0, 0),
            weekendEndTime = LocalTime.of(0, 0)
        )
        assertEquals(0, midnight.toSettingsBackup().weekendStartTimeMinutes)
        assertEquals(0, midnight.toSettingsBackup().weekendEndTimeMinutes)

        // Edge case: end of day
        val endOfDay = UserPreferences.default().copy(
            weekendStartTime = LocalTime.of(23, 59),
            weekendEndTime = LocalTime.of(23, 59)
        )
        assertEquals(1439, endOfDay.toSettingsBackup().weekendStartTimeMinutes)
        assertEquals(1439, endOfDay.toSettingsBackup().weekendEndTimeMinutes)
    }

    @Test
    fun `toSettingsBackup with empty weekend days maps to empty set`() {
        val prefs = UserPreferences.default().copy(weekendDays = emptySet())
        assertEquals(emptySet<String>(), prefs.toSettingsBackup().weekendDays)
    }

    @Test
    fun `toSettingsBackup preserves homeLocation with explicit radius`() {
        val prefs = UserPreferences.default().copy(
            homeLocation = HomeLocation(1.0, 2.0, 150f)
        )
        val backup = prefs.toSettingsBackup()
        assertEquals(1.0, backup.homeLatitude!!, 0.0001)
        assertEquals(2.0, backup.homeLongitude!!, 0.0001)
        assertEquals(150f, backup.homeRadiusMeters!!, 0.01f)
    }

    // ── applyTo (BackupMapper → PreferencesDataSource integration) ───

    @Test
    fun `applyTo writes all fields correctly`() {
        val freshPrefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_apply_to", Context.MODE_PRIVATE)
        freshPrefs.edit().clear().commit()
        val ds = PreferencesDataSource(
            freshPrefs,
            CoroutineScope(Dispatchers.Unconfined),
            GsonBuilder().create()
        )

        val backup = SettingsBackup(
            weekendStartTimeMinutes = 510,
            weekendEndTimeMinutes = 1320,
            weekendMode = true,
            weekendDays = setOf("SATURDAY", "SUNDAY"),
            exactReminders = true,
            repeatReminders = true,
            numberOfRepetitions = 5,
            repeatDelayMinutes = 20,
            snoozeDurationMinutes = 30,
            overrideDnd = true,
            stickyOnLockscreen = true,
            dismissNotificationAction = "TAKE",
            cannotSkipReminders = true,
            bigNotifications = true,
            combineNotifications = true,
            useRelativeDateTime = true,
            showTakenTimeInOverview = false,
            systemLocale = true,
            theme = "ALTERNATIVE",
            hideMedicineName = true,
            appAuthentication = true,
            useSecureWindow = true,
            disableWidget = true,
            alarmRingtone = "content://settings/ringtone",
            noAlarmSoundWhenSilent = true,
            noVibrationWhenSilent = true,
            automaticBackupInterval = "WEEKLY",
            automaticBackupDirectory = "content://backup/test",
            locationBasedSnooze = true,
            homeLatitude = 48.137,
            homeLongitude = 11.575,
            homeRadiusMeters = 200f
        )
        backup.applyTo(ds)

        val p = ds.preferences.value
        assertEquals(LocalTime.of(8, 30), p.weekendStartTime)
        assertEquals(LocalTime.of(22, 0), p.weekendEndTime)
        assertEquals(true, p.weekendMode)
        assertEquals(setOf("SATURDAY", "SUNDAY"), p.weekendDays)
        assertEquals(true, p.exactReminders)
        assertEquals(true, p.repeatReminders)
        assertEquals(5, p.numberOfRepetitions)
        assertEquals(20.minutes, p.repeatDelay)
        assertEquals(30.minutes, p.snoozeDuration)
        assertEquals(true, p.overrideDnd)
        assertEquals(true, p.stickyOnLockscreen)
        assertEquals(DismissNotificationAction.TAKE, p.dismissNotificationAction)
        assertEquals(true, p.cannotSkipReminders)
        assertEquals(true, p.bigNotifications)
        assertEquals(true, p.combineNotifications)
        assertEquals(true, p.useRelativeDateTime)
        assertEquals(false, p.showTakenTimeInOverview)
        assertEquals(true, p.systemLocale)
        assertEquals(ThemeSetting.ALTERNATIVE, p.theme)
        assertEquals(true, p.hideMedicineName)
        assertEquals(true, p.appAuthentication)
        assertEquals(true, p.useSecureWindow)
        assertEquals(true, p.disableWidget)
        assertNotNull(p.alarmRingtone)
        assertEquals(true, p.noAlarmSoundWhenSilent)
        assertEquals(true, p.noVibrationWhenSilent)
        assertEquals(BackupInterval.WEEKLY, p.automaticBackupInterval)
        assertNotNull(p.automaticBackupDirectory)
        assertEquals(true, p.locationBasedSnooze)
        assertNotNull(p.homeLocation)
        assertEquals(48.137, p.homeLocation!!.latitude, 0.0001)
        assertEquals(11.575, p.homeLocation!!.longitude, 0.0001)
        assertEquals(200f, p.homeLocation!!.radiusMeters, 0.01f)
    }

    @Test
    fun `applyTo with null home location clears location`() {
        val freshPrefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_apply_null", Context.MODE_PRIVATE)
        freshPrefs.edit().clear().commit()
        val ds = PreferencesDataSource(
            freshPrefs,
            CoroutineScope(Dispatchers.Unconfined),
            GsonBuilder().create()
        )

        ds.saveHomeLocation(HomeLocation(1.0, 2.0, 100f))

        val backup = SettingsBackup(
            weekendStartTimeMinutes = 0, weekendEndTimeMinutes = 540,
            weekendMode = false, weekendDays = emptySet(),
            exactReminders = false, repeatReminders = false,
            numberOfRepetitions = 3, repeatDelayMinutes = 10, snoozeDurationMinutes = 15,
            overrideDnd = false, stickyOnLockscreen = false,
            dismissNotificationAction = "SKIP", cannotSkipReminders = false,
            bigNotifications = false, combineNotifications = false,
            useRelativeDateTime = false, showTakenTimeInOverview = true,
            systemLocale = false, theme = "DEFAULT",
            hideMedicineName = false, appAuthentication = false,
            useSecureWindow = false, disableWidget = false,
            alarmRingtone = null, noAlarmSoundWhenSilent = false,
            noVibrationWhenSilent = false,
            automaticBackupInterval = "NEVER", automaticBackupDirectory = null,
            locationBasedSnooze = false,
            homeLatitude = null, homeLongitude = null, homeRadiusMeters = null
        )
        backup.applyTo(ds)

        assertNull(ds.preferences.value.homeLocation)
    }

    @Test
    fun `applyTo roundtrip consistency`() {
        val freshPrefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_roundtrip", Context.MODE_PRIVATE)
        freshPrefs.edit().clear().commit()
        val ds = PreferencesDataSource(
            freshPrefs,
            CoroutineScope(Dispatchers.Unconfined),
            GsonBuilder().create()
        )

        val original = UserPreferences.default().copy(
            weekendStartTime = LocalTime.of(7, 15),
            weekendEndTime = LocalTime.of(21, 45),
            weekendMode = true,
            weekendDays = setOf("MONDAY", "WEDNESDAY", "FRIDAY"),
            exactReminders = true,
            repeatReminders = true,
            numberOfRepetitions = 4,
            repeatDelay = 25.minutes,
            snoozeDuration = 10.minutes,
            overrideDnd = true,
            stickyOnLockscreen = false,
            dismissNotificationAction = DismissNotificationAction.SNOOZE,
            cannotSkipReminders = true,
            bigNotifications = false,
            combineNotifications = true,
            useRelativeDateTime = false,
            showTakenTimeInOverview = false,
            systemLocale = true,
            theme = ThemeSetting.ALTERNATIVE,
            hideMedicineName = false,
            appAuthentication = true,
            useSecureWindow = true,
            disableWidget = false,
            alarmRingtone = Uri.parse("content://settings/custom_ringtone"),
            noAlarmSoundWhenSilent = true,
            noVibrationWhenSilent = false,
            automaticBackupInterval = BackupInterval.MONTHLY,
            automaticBackupDirectory = Uri.parse("content://backup/test"),
            locationBasedSnooze = true,
            homeLocation = HomeLocation(10.0, 20.0, 50f)
        )

        val backup = original.toSettingsBackup()
        backup.applyTo(ds)
        val result = ds.preferences.value

        assertEquals(original.weekendStartTime, result.weekendStartTime)
        assertEquals(original.weekendEndTime, result.weekendEndTime)
        assertEquals(original.weekendMode, result.weekendMode)
        assertEquals(original.weekendDays, result.weekendDays)
        assertEquals(original.exactReminders, result.exactReminders)
        assertEquals(original.repeatReminders, result.repeatReminders)
        assertEquals(original.numberOfRepetitions, result.numberOfRepetitions)
        assertEquals(original.repeatDelay, result.repeatDelay)
        assertEquals(original.snoozeDuration, result.snoozeDuration)
        assertEquals(original.overrideDnd, result.overrideDnd)
        assertEquals(original.stickyOnLockscreen, result.stickyOnLockscreen)
        assertEquals(original.dismissNotificationAction, result.dismissNotificationAction)
        assertEquals(original.cannotSkipReminders, result.cannotSkipReminders)
        assertEquals(original.bigNotifications, result.bigNotifications)
        assertEquals(original.combineNotifications, result.combineNotifications)
        assertEquals(original.useRelativeDateTime, result.useRelativeDateTime)
        assertEquals(original.showTakenTimeInOverview, result.showTakenTimeInOverview)
        assertEquals(original.systemLocale, result.systemLocale)
        assertEquals(original.theme, result.theme)
        assertEquals(original.hideMedicineName, result.hideMedicineName)
        assertEquals(original.appAuthentication, result.appAuthentication)
        assertEquals(original.useSecureWindow, result.useSecureWindow)
        assertEquals(original.disableWidget, result.disableWidget)
        assertEquals(original.alarmRingtone, result.alarmRingtone)
        assertEquals(original.noAlarmSoundWhenSilent, result.noAlarmSoundWhenSilent)
        assertEquals(original.noVibrationWhenSilent, result.noVibrationWhenSilent)
        assertEquals(original.automaticBackupInterval, result.automaticBackupInterval)
        assertEquals(original.automaticBackupDirectory, result.automaticBackupDirectory)
        assertEquals(original.locationBasedSnooze, result.locationBasedSnooze)
        assertNotNull(result.homeLocation)
        assertEquals(10.0, result.homeLocation!!.latitude, 0.0001)
        assertEquals(20.0, result.homeLocation!!.longitude, 0.0001)
        assertEquals(50f, result.homeLocation!!.radiusMeters, 0.01f)
    }
}

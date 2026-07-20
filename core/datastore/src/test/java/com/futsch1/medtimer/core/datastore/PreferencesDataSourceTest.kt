package com.futsch1.medtimer.core.datastore

import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.core.domain.model.BackupInterval
import com.futsch1.medtimer.core.domain.model.DismissNotificationAction
import com.futsch1.medtimer.core.domain.model.HomeLocation
import com.futsch1.medtimer.core.domain.model.ThemeSetting
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PreferencesDataSourceTest {

    private lateinit var dataSource: PreferencesDataSource
    private lateinit var sharedPreferences: android.content.SharedPreferences

    @Before
    fun setUp() {
        sharedPreferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_prefs_ds", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        dataSource = PreferencesDataSource(
            sharedPreferences,
            CoroutineScope(Dispatchers.Unconfined),
            GsonBuilder().create()
        )
    }

    // ── A: Default values ──────────────────────────────────────────────

    @Test
    fun `default weekendStartTime is midnight`() {
        assertEquals(LocalTime.of(0, 0), dataSource.preferences.value.weekendStartTime)
    }

    @Test
    fun `default weekendEndTime is 9 AM`() {
        assertEquals(LocalTime.of(9, 0), dataSource.preferences.value.weekendEndTime)
    }

    @Test
    fun `default weekendMode is false`() {
        assertEquals(false, dataSource.preferences.value.weekendMode)
    }

    @Test
    fun `default weekendDays is empty set`() {
        assertTrue(dataSource.preferences.value.weekendDays.isEmpty())
    }

    @Test
    fun `default exactReminders is false`() {
        assertEquals(false, dataSource.preferences.value.exactReminders)
    }

    @Test
    fun `default repeatReminders is false`() {
        assertEquals(false, dataSource.preferences.value.repeatReminders)
    }

    @Test
    fun `default numberOfRepetitions is 3`() {
        assertEquals(3, dataSource.preferences.value.numberOfRepetitions)
    }

    @Test
    fun `default repeatDelay is 10 minutes`() {
        assertEquals(10.minutes, dataSource.preferences.value.repeatDelay)
    }

    @Test
    fun `default snoozeDuration is 15 minutes`() {
        assertEquals(15.minutes, dataSource.preferences.value.snoozeDuration)
    }

    @Test
    fun `default overrideDnd is false`() {
        assertEquals(false, dataSource.preferences.value.overrideDnd)
    }

    @Test
    fun `default stickyOnLockscreen is false`() {
        assertEquals(false, dataSource.preferences.value.stickyOnLockscreen)
    }

    @Test
    fun `default dismissNotificationAction is SKIP`() {
        assertEquals(DismissNotificationAction.SKIP, dataSource.preferences.value.dismissNotificationAction)
    }

    @Test
    fun `default cannotSkipReminders is false`() {
        assertEquals(false, dataSource.preferences.value.cannotSkipReminders)
    }

    @Test
    fun `default bigNotifications is false`() {
        assertEquals(false, dataSource.preferences.value.bigNotifications)
    }

    @Test
    fun `default combineNotifications is false`() {
        assertEquals(false, dataSource.preferences.value.combineNotifications)
    }

    @Test
    fun `default useRelativeDateTime is false`() {
        assertEquals(false, dataSource.preferences.value.useRelativeDateTime)
    }

    @Test
    fun `default showTakenTimeInOverview is true`() {
        assertEquals(true, dataSource.preferences.value.showTakenTimeInOverview)
    }

    @Test
    fun `default systemLocale is false`() {
        assertEquals(false, dataSource.preferences.value.systemLocale)
    }

    @Test
    fun `default theme is DEFAULT`() {
        assertEquals(ThemeSetting.DEFAULT, dataSource.preferences.value.theme)
    }

    @Test
    fun `default hideMedicineName is false`() {
        assertEquals(false, dataSource.preferences.value.hideMedicineName)
    }

    @Test
    fun `default appAuthentication is false`() {
        assertEquals(false, dataSource.preferences.value.appAuthentication)
    }

    @Test
    fun `default useSecureWindow is false`() {
        assertEquals(false, dataSource.preferences.value.useSecureWindow)
    }

    @Test
    fun `default disableWidget is false`() {
        assertEquals(false, dataSource.preferences.value.disableWidget)
    }

    @Test
    fun `default alarmRingtone is system default alarm URI`() {
        assertEquals(Settings.System.DEFAULT_ALARM_ALERT_URI, dataSource.preferences.value.alarmRingtone)
    }

    @Test
    fun `default noAlarmSoundWhenSilent is false`() {
        assertEquals(false, dataSource.preferences.value.noAlarmSoundWhenSilent)
    }

    @Test
    fun `default noVibrationWhenSilent is false`() {
        assertEquals(false, dataSource.preferences.value.noVibrationWhenSilent)
    }

    @Test
    fun `default automaticBackupInterval is NEVER`() {
        assertEquals(BackupInterval.NEVER, dataSource.preferences.value.automaticBackupInterval)
    }

    @Test
    fun `default automaticBackupDirectory is null`() {
        assertNull(dataSource.preferences.value.automaticBackupDirectory)
    }

    @Test
    fun `default locationBasedSnooze is false`() {
        assertEquals(false, dataSource.preferences.value.locationBasedSnooze)
    }

    @Test
    fun `default homeLocation is null`() {
        assertNull(dataSource.preferences.value.homeLocation)
    }

    // ── B: Write-read roundtrips ───────────────────────────────────────

    @Test
    fun `setWeekendStartTime roundtrip`() {
        dataSource.setWeekendStartTime(LocalTime.of(14, 30))
        assertEquals(LocalTime.of(14, 30), dataSource.preferences.value.weekendStartTime)
    }

    @Test
    fun `setWeekendEndTime to midnight`() {
        dataSource.setWeekendEndTime(LocalTime.of(0, 0))
        assertEquals(LocalTime.of(0, 0), dataSource.preferences.value.weekendEndTime)
    }

    @Test
    fun `setWeekendEndTime roundtrip`() {
        dataSource.setWeekendEndTime(LocalTime.of(22, 0))
        assertEquals(LocalTime.of(22, 0), dataSource.preferences.value.weekendEndTime)
    }

    @Test
    fun `setAutomaticBackupInterval roundtrip DAILY`() {
        dataSource.setAutomaticBackupInterval(BackupInterval.DAILY)
        assertEquals(BackupInterval.DAILY, dataSource.preferences.value.automaticBackupInterval)
    }

    @Test
    fun `setAutomaticBackupInterval roundtrip WEEKLY`() {
        dataSource.setAutomaticBackupInterval(BackupInterval.WEEKLY)
        assertEquals(BackupInterval.WEEKLY, dataSource.preferences.value.automaticBackupInterval)
    }

    @Test
    fun `setAutomaticBackupInterval roundtrip MONTHLY`() {
        dataSource.setAutomaticBackupInterval(BackupInterval.MONTHLY)
        assertEquals(BackupInterval.MONTHLY, dataSource.preferences.value.automaticBackupInterval)
    }

    @Test
    fun `setAutomaticBackupInterval roundtrip NEVER`() {
        dataSource.setAutomaticBackupInterval(BackupInterval.DAILY)
        dataSource.setAutomaticBackupInterval(BackupInterval.NEVER)
        assertEquals(BackupInterval.NEVER, dataSource.preferences.value.automaticBackupInterval)
    }

    @Test
    fun `setAutomaticBackupDirectory roundtrip`() {
        val uri = Uri.parse("content://backup/test")
        dataSource.setAutomaticBackupDirectory(uri)
        assertEquals(uri, dataSource.preferences.value.automaticBackupDirectory)
    }

    @Test
    fun `setAutomaticBackupDirectory to null via empty string`() {
        // Set a value first, then overwrite with empty — verifies the setter
        dataSource.setAutomaticBackupDirectory(Uri.parse("content://test"))
        sharedPreferences.edit().putString(PreferencesDataSource.AUTOMATIC_BACKUP_DIRECTORY, "").commit()
        // After clearing, the default is null
        assertNull(dataSource.preferences.value.automaticBackupDirectory)
    }

    @Test
    fun `saveHomeLocation roundtrip`() {
        val location = HomeLocation(48.137, 11.575, 200f)
        dataSource.saveHomeLocation(location)
        val loaded = dataSource.preferences.value.homeLocation
        assertNotNull(loaded)
        assertEquals(48.137, loaded!!.latitude, 0.0001)
        assertEquals(11.575, loaded.longitude, 0.0001)
        assertEquals(200f, loaded.radiusMeters, 0.01f)
    }

    @Test
    fun `saveHomeLocation with min radius`() {
        dataSource.saveHomeLocation(HomeLocation(0.0, 0.0, 0f))
        val loaded = dataSource.preferences.value.homeLocation
        assertEquals(0f, loaded!!.radiusMeters, 0.01f)
    }

    @Test
    fun `saveHomeLocation with max radius`() {
        dataSource.saveHomeLocation(HomeLocation(90.0, -180.0, Float.MAX_VALUE))
        val loaded = dataSource.preferences.value.homeLocation
        assertEquals(Float.MAX_VALUE, loaded!!.radiusMeters, 0.01f)
    }

    @Test
    fun `clearHomeLocation removes it`() {
        dataSource.saveHomeLocation(HomeLocation(1.0, 2.0, 100f))
        dataSource.clearHomeLocation()
        assertNull(dataSource.preferences.value.homeLocation)
    }

    // ── C: PreferenceDataStore overrides ───────────────────────────────

    @Test
    fun `putBoolean and getBoolean roundtrip`() {
        dataSource.putBoolean("test_bool", true)
        assertEquals(true, dataSource.getBoolean("test_bool", false))
        dataSource.putBoolean("test_bool", false)
        assertEquals(false, dataSource.getBoolean("test_bool", true))
    }

    @Test
    fun `getBoolean returns default when key missing`() {
        assertEquals(true, dataSource.getBoolean("nonexistent", true))
    }

    @Test
    fun `putStringSet and getStringSet roundtrip`() {
        val set = setOf("a", "b", "c")
        dataSource.putStringSet("test_set", set)
        assertEquals(set, dataSource.getStringSet("test_set", emptySet()))
    }

    @Test
    fun `putStringSet with null returns default`() {
        dataSource.putStringSet("test_null", null)
        val result = dataSource.getStringSet("test_null", setOf("default"))
        // SharedPreferences returns null for non-existent keys after clear, but putStringSet with null...
        // Actually SharedPreferences.edit { putStringSet(key, null) } removes the key
        assertEquals(setOf("default"), result)
    }

    @Test
    fun `putInt and getInt roundtrip`() {
        dataSource.putInt("test_int", 42)
        assertEquals(42, dataSource.getInt("test_int", 0))
        dataSource.putInt("test_int", Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, dataSource.getInt("test_int", 0))
        dataSource.putInt("test_int", Int.MIN_VALUE)
        assertEquals(Int.MIN_VALUE, dataSource.getInt("test_int", 0))
    }

    @Test
    fun `getInt returns default when key missing`() {
        assertEquals(-1, dataSource.getInt("nonexistent", -1))
    }

    @Test
    fun `putString and getString roundtrip`() {
        dataSource.putString("test_str", "hello")
        assertEquals("hello", dataSource.getString("test_str", null))
    }

    @Test
    fun `putString with null removes key so getString returns default`() {
        // SharedPreferences.edit { putString(key, null) } removes the key
        dataSource.putString("test_str", "hello")
        dataSource.putString("test_str", null)
        assertEquals("fallback", dataSource.getString("test_str", "fallback"))
    }

    @Test
    fun `getString returns default when key missing`() {
        assertEquals("fallback", dataSource.getString("nonexistent", "fallback"))
    }

    @Test
    fun `getString with empty string`() {
        dataSource.putString("test_empty", "")
        assertEquals("", dataSource.getString("test_empty", null))
    }

    // ── D: Enum parsing ────────────────────────────────────────────────

    @Test
    fun `dismissNotificationAction parses all variants`() {
        data class Case(val input: String?, val expected: DismissNotificationAction)
        listOf(
            Case("0", DismissNotificationAction.SKIP),
            Case("1", DismissNotificationAction.SNOOZE),
            Case("2", DismissNotificationAction.TAKE),
            Case("invalid", DismissNotificationAction.TAKE),
        ).forEach { (input, expected) ->
            sharedPreferences.edit().putString(PreferencesDataSource.DISMISS_NOTIFICATION_ACTION, input).commit()
            assertEquals(expected, dataSource.preferences.value.dismissNotificationAction)
        }
    }

    @Test
    fun `theme parses all variants`() {
        data class Case(val setup: () -> Unit, val expected: ThemeSetting)
        listOf(
            Case({ sharedPreferences.edit().putString(PreferencesDataSource.THEME, "0").commit() }, ThemeSetting.DEFAULT),
            Case({ sharedPreferences.edit().putString(PreferencesDataSource.THEME, "1").commit() }, ThemeSetting.ALTERNATIVE),
            Case({ sharedPreferences.edit().putString(PreferencesDataSource.THEME, "invalid").commit() }, ThemeSetting.ALTERNATIVE),
            Case({ sharedPreferences.edit().remove(PreferencesDataSource.THEME).commit() }, ThemeSetting.DEFAULT),
        ).forEach { (setup, expected) ->
            setup()
            assertEquals(expected, dataSource.preferences.value.theme)
        }
    }

    @Test
    fun `automaticBackupInterval parses all variants`() {
        data class Case(val input: String, val expected: BackupInterval)
        listOf(
            Case("0", BackupInterval.NEVER),
            Case("1", BackupInterval.DAILY),
            Case("2", BackupInterval.WEEKLY),
            Case("3", BackupInterval.MONTHLY),
            Case("never", BackupInterval.NEVER),
            Case("daily", BackupInterval.DAILY),
            Case("weekly", BackupInterval.WEEKLY),
            Case("monthly", BackupInterval.MONTHLY),
            Case("invalid", BackupInterval.NEVER),
        ).forEach { (input, expected) ->
            sharedPreferences.edit().putString(PreferencesDataSource.AUTOMATIC_BACKUP_INTERVAL, input).commit()
            assertEquals(expected, dataSource.preferences.value.automaticBackupInterval)
        }
    }

    // ── E: Notificatie-gevoelige transformaties ─────────────────────────

    @Test
    fun `numberOfRepetitions parses from string`() {
        sharedPreferences.edit().putString(PreferencesDataSource.NUMBER_OF_REPETITIONS, "5").commit()
        assertEquals(5, dataSource.preferences.value.numberOfRepetitions)
    }

    @Test
    fun `numberOfRepetitions defaults on invalid string`() {
        sharedPreferences.edit().putString(PreferencesDataSource.NUMBER_OF_REPETITIONS, "abc").commit()
        assertEquals(3, dataSource.preferences.value.numberOfRepetitions)
    }

    @Test
    fun `numberOfRepetitions defaults on missing key`() {
        sharedPreferences.edit().remove(PreferencesDataSource.NUMBER_OF_REPETITIONS).commit()
        assertEquals(3, dataSource.preferences.value.numberOfRepetitions)
    }

    @Test
    fun `repeatDelay parses from string`() {
        sharedPreferences.edit().putString(PreferencesDataSource.REPEAT_DELAY, "20").commit()
        assertEquals(20.minutes, dataSource.preferences.value.repeatDelay)
    }

    @Test
    fun `repeatDelay defaults on invalid string`() {
        sharedPreferences.edit().putString(PreferencesDataSource.REPEAT_DELAY, "abc").commit()
        assertEquals(10.minutes, dataSource.preferences.value.repeatDelay)
    }

    @Test
    fun `snoozeDuration parses from string`() {
        sharedPreferences.edit().putString(PreferencesDataSource.SNOOZE_DURATION, "30").commit()
        assertEquals(30.minutes, dataSource.preferences.value.snoozeDuration)
    }

    @Test
    fun `snoozeDuration defaults on invalid string`() {
        sharedPreferences.edit().putString(PreferencesDataSource.SNOOZE_DURATION, "abc").commit()
        assertEquals(15.minutes, dataSource.preferences.value.snoozeDuration)
    }

    // ── F: Flow behaviour ──────────────────────────────────────────────

    @Test
    fun `flow emits initial value`() = runTest {
        val value = dataSource.preferences.first()
        assertNotNull(value)
        assertEquals(LocalTime.of(0, 0), value.weekendStartTime)
    }

    @Test
    fun `flow emits updated value after putBoolean`() = runTest {
        dataSource.putBoolean(PreferencesDataSource.WEEKEND_MODE, true)
        val value = dataSource.preferences.first()
        assertEquals(true, value.weekendMode)
    }

    @Test
    fun `flow emits updated value after setWeekendStartTime`() = runTest {
        dataSource.setWeekendStartTime(LocalTime.of(12, 0))
        val value = dataSource.preferences.first()
        assertEquals(LocalTime.of(12, 0), value.weekendStartTime)
    }

    @Test
    fun `flow emits updated value after setAutomaticBackupInterval`() = runTest {
        dataSource.setAutomaticBackupInterval(BackupInterval.DAILY)
        val value = dataSource.preferences.first()
        assertEquals(BackupInterval.DAILY, value.automaticBackupInterval)
    }

    @Test
    fun `flow emits updated value after saveHomeLocation`() = runTest {
        dataSource.saveHomeLocation(HomeLocation(10.0, 20.0, 50f))
        val value = dataSource.preferences.first()
        assertNotNull(value.homeLocation)
    }

}

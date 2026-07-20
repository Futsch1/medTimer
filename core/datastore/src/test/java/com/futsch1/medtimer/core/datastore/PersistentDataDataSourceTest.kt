package com.futsch1.medtimer.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.core.domain.model.PendingSnooze
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PersistentDataDataSourceTest {

    private lateinit var dataSource: PersistentDataDataSource
    private lateinit var defaultPrefs: android.content.SharedPreferences
    private lateinit var medTimerPrefs: android.content.SharedPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        defaultPrefs = context.getSharedPreferences("test_pd_default", Context.MODE_PRIVATE)
        medTimerPrefs = context.getSharedPreferences("test_pd_medtimer", Context.MODE_PRIVATE)
        defaultPrefs.edit().clear().commit()
        medTimerPrefs.edit().clear().commit()
        dataSource = PersistentDataDataSource(
            defaultPrefs,
            medTimerPrefs,
            CoroutineScope(Dispatchers.Unconfined),
            GsonBuilder().create()
        )
    }

    // ── A: Default values ──────────────────────────────────────────────

    @Test
    fun `default showNotifications is true`() {
        assertEquals(true, dataSource.data.value.showNotifications)
    }

    @Test
    fun `default iconColor is 0`() {
        assertEquals(0, dataSource.data.value.iconColor)
    }

    @Test
    fun `default activeStatisticsFragment is CHARTS`() {
        assertEquals(StatisticFragment.CHARTS, dataSource.data.value.activeStatisticsFragment)
    }

    @Test
    fun `default analysisDays is 7`() {
        assertEquals(7, dataSource.data.value.analysisDays)
    }

    @Test
    fun `default batteryWarningShown is false`() {
        assertEquals(false, dataSource.data.value.batteryWarningShown)
    }

    @Test
    fun `default exactRemindersWarningShown is false`() {
        assertEquals(false, dataSource.data.value.exactRemindersWarningShown)
    }

    @Test
    fun `default introShown is false`() {
        assertEquals(false, dataSource.data.value.introShown)
    }

    @Test
    fun `default lastAutomaticBackup is EPOCH`() {
        assertEquals(LocalDate.EPOCH, dataSource.data.value.lastAutomaticBackup)
    }

    @Test
    fun `default notificationId is 0`() {
        assertEquals(0, dataSource.data.value.notificationId)
    }

    @Test
    fun `default lastCustomDose is empty string`() {
        assertEquals("", dataSource.data.value.lastCustomDose)
    }

    @Test
    fun `default lastCustomDoseAmount is empty string`() {
        assertEquals("", dataSource.data.value.lastCustomDoseAmount)
    }

    @Test
    fun `default filterTags is empty`() {
        assertTrue(dataSource.data.value.filterTags.isEmpty())
    }

    @Test
    fun `default checkedFilters is empty`() {
        assertTrue(dataSource.data.value.checkedFilters.isEmpty())
    }

    // ── B: Write-read setters ──────────────────────────────────────────

    @Test
    fun `setLastCustomDose roundtrip`() {
        dataSource.setLastCustomDose("Aspirin 500mg")
        assertEquals("Aspirin 500mg", dataSource.data.value.lastCustomDose)
    }

    @Test
    fun `setLastCustomDose with empty string`() {
        dataSource.setLastCustomDose("")
        assertEquals("", dataSource.data.value.lastCustomDose)
    }

    @Test
    fun `setLastCustomDoseAmount roundtrip`() {
        dataSource.setLastCustomDoseAmount("1.5")
        assertEquals("1.5", dataSource.data.value.lastCustomDoseAmount)
    }

    @Test
    fun `setActiveStatisticsFragment roundtrip CHARTS`() {
        dataSource.setActiveStatisticsFragment(StatisticFragment.CHARTS)
        assertEquals(StatisticFragment.CHARTS, dataSource.data.value.activeStatisticsFragment)
    }

    @Test
    fun `setActiveStatisticsFragment roundtrip TABLE`() {
        dataSource.setActiveStatisticsFragment(StatisticFragment.TABLE)
        assertEquals(StatisticFragment.TABLE, dataSource.data.value.activeStatisticsFragment)
    }

    @Test
    fun `setActiveStatisticsFragment roundtrip CALENDAR`() {
        dataSource.setActiveStatisticsFragment(StatisticFragment.CALENDAR)
        assertEquals(StatisticFragment.CALENDAR, dataSource.data.value.activeStatisticsFragment)
    }

    @Test
    fun `setAnalysisDays roundtrip`() {
        dataSource.setAnalysisDays(30)
        assertEquals(30, dataSource.data.value.analysisDays)
    }

    @Test
    fun `setAnalysisDays to 1`() {
        dataSource.setAnalysisDays(1)
        assertEquals(1, dataSource.data.value.analysisDays)
    }

    @Test
    fun `setAnalysisDays to 365`() {
        dataSource.setAnalysisDays(365)
        assertEquals(365, dataSource.data.value.analysisDays)
    }

    @Test
    fun `setIntroShown roundtrip true`() {
        dataSource.setIntroShown(true)
        assertEquals(true, dataSource.data.value.introShown)
    }

    @Test
    fun `setIntroShown toggle back to false`() {
        dataSource.setIntroShown(true)
        dataSource.setIntroShown(false)
        assertEquals(false, dataSource.data.value.introShown)
    }

    @Test
    fun `setBatteryWarningShown roundtrip`() {
        dataSource.setBatteryWarningShown(true)
        assertEquals(true, dataSource.data.value.batteryWarningShown)
    }

    @Test
    fun `setExactRemindersWarningShown roundtrip`() {
        dataSource.setExactRemindersWarningShown(true)
        assertEquals(true, dataSource.data.value.exactRemindersWarningShown)
    }

    @Test
    fun `setLastAutomaticBackup roundtrip`() {
        val date = LocalDate.of(2026, 7, 20)
        dataSource.setLastAutomaticBackup(date)
        assertEquals(date, dataSource.data.value.lastAutomaticBackup)
    }

    @Test
    fun `setLastAutomaticBackup to epoch`() {
        dataSource.setLastAutomaticBackup(LocalDate.EPOCH)
        assertEquals(LocalDate.EPOCH, dataSource.data.value.lastAutomaticBackup)
    }

    @Test
    fun `setFilterTags roundtrip`() {
        val tags = setOf("tag1", "tag2", "tag3")
        dataSource.setFilterTags(tags)
        assertEquals(tags, dataSource.data.value.filterTags)
    }

    @Test
    fun `setFilterTags with empty set`() {
        dataSource.setFilterTags(setOf("a"))
        dataSource.setFilterTags(emptySet())
        assertTrue(dataSource.data.value.filterTags.isEmpty())
    }

    @Test
    fun `setCheckedFilters roundtrip with single filter`() {
        dataSource.setCheckedFilters(setOf(OverviewFilter.TAKEN))
        assertEquals(setOf(OverviewFilter.TAKEN), dataSource.data.value.checkedFilters)
    }

    @Test
    fun `setCheckedFilters roundtrip with multiple filters`() {
        val filters = setOf(OverviewFilter.TAKEN, OverviewFilter.SKIPPED, OverviewFilter.SCHEDULED, OverviewFilter.RAISED)
        dataSource.setCheckedFilters(filters)
        assertEquals(filters, dataSource.data.value.checkedFilters)
    }

    @Test
    fun `setCheckedFilters with empty set`() {
        dataSource.setCheckedFilters(setOf(OverviewFilter.TAKEN))
        dataSource.setCheckedFilters(emptySet())
        assertTrue(dataSource.data.value.checkedFilters.isEmpty())
    }

    @Test
    // BUG: setShowNotifications writes to medTimerSharedPreferences but
    // getPersistentData() reads from defaultSharedPreferences →
    // roundtrip always returns the default (true).
    fun `setShowNotifications roundtrip`() {
        dataSource.setShowNotifications(false)
        assertEquals(false, dataSource.data.value.showNotifications)
        dataSource.setShowNotifications(true)
        assertEquals(true, dataSource.data.value.showNotifications)
    }

    @Test
    fun `setIconColor roundtrip`() {
        dataSource.setIconColor(0xFF5722)
        assertEquals(0xFF5722, dataSource.data.value.iconColor)
    }

    @Test
    fun `setIconColor to 0`() {
        dataSource.setIconColor(0)
        assertEquals(0, dataSource.data.value.iconColor)
    }

    // ── C: Notification ID ─────────────────────────────────────────────

    @Test
    fun `getAndIncreaseNotificationId starts at 0`() {
        assertEquals(0, dataSource.getAndIncreaseNotificationId())
    }

    @Test
    fun `getAndIncreaseNotificationId increments`() {
        assertEquals(0, dataSource.getAndIncreaseNotificationId())
        assertEquals(1, dataSource.getAndIncreaseNotificationId())
        assertEquals(2, dataSource.getAndIncreaseNotificationId())
    }

    @Test
    fun `getAndIncreaseNotificationId handles many increments`() {
        val ids = (0 until 1000).map { dataSource.getAndIncreaseNotificationId() }
        assertEquals(1000, ids.size)
        assertEquals(0, ids.first())
        assertEquals(999, ids.last())
        // Verify no duplicates
        assertEquals(ids.distinct().size, ids.size)
    }

    @Test
    fun `getAndIncreaseNotificationId persists between instances`() {
        dataSource.getAndIncreaseNotificationId() // 0
        dataSource.getAndIncreaseNotificationId() // 1
        dataSource.getAndIncreaseNotificationId() // 2

        // Create new instance reading same prefs
        val ds2 = PersistentDataDataSource(
            defaultPrefs,
            medTimerPrefs,
            CoroutineScope(Dispatchers.Unconfined),
            GsonBuilder().create()
        )
        assertEquals(3, ds2.getAndIncreaseNotificationId())
    }

    @Test
    fun `getAndIncreaseNotificationId handles overflow`() {
        medTimerPrefs.edit().putInt(PersistentDataDataSource.NOTIFICATION_ID, Int.MAX_VALUE).commit()
        // Should wrap around to Int.MIN_VALUE
        assertEquals(Int.MAX_VALUE, dataSource.getAndIncreaseNotificationId())
        assertEquals(Int.MIN_VALUE, dataSource.getAndIncreaseNotificationId())
    }

    // ── D: StatisticFragment enum mapping ──────────────────────────────

    @Test
    fun `activeStatisticsFragment parses CHARTS from 0`() {
        defaultPrefs.edit().putInt(PersistentDataDataSource.ACTIVE_STATISTICS_FRAGMENT, 0).commit()
        assertEquals(StatisticFragment.CHARTS, dataSource.data.value.activeStatisticsFragment)
    }

    @Test
    fun `activeStatisticsFragment parses TABLE from 1`() {
        defaultPrefs.edit().putInt(PersistentDataDataSource.ACTIVE_STATISTICS_FRAGMENT, 1).commit()
        assertEquals(StatisticFragment.TABLE, dataSource.data.value.activeStatisticsFragment)
    }

    @Test
    fun `activeStatisticsFragment parses CALENDAR from 2`() {
        defaultPrefs.edit().putInt(PersistentDataDataSource.ACTIVE_STATISTICS_FRAGMENT, 2).commit()
        assertEquals(StatisticFragment.CALENDAR, dataSource.data.value.activeStatisticsFragment)
    }

    @Test
    fun `activeStatisticsFragment defaults to CALENDAR on invalid`() {
        defaultPrefs.edit().putInt(PersistentDataDataSource.ACTIVE_STATISTICS_FRAGMENT, 99).commit()
        assertEquals(StatisticFragment.CALENDAR, dataSource.data.value.activeStatisticsFragment)
    }

    // ── E: Pending snooze CRUD ─────────────────────────────────────────

    @Test
    fun `getPendingLocationSnoozes returns empty initially`() {
        assertTrue(dataSource.getPendingLocationSnoozes().isEmpty())
    }

    @Test
    fun `addAndGetPendingSnooze roundtrip`() {
        val snooze = PendingSnooze(
            remindInstant = Instant.ofEpochSecond(1234567890L),
            reminderIds = listOf(1, 2),
            reminderEventIds = mutableListOf(10, 20),
            notificationId = 42
        )
        dataSource.addPendingLocationSnooze(snooze)

        val loaded = dataSource.getPendingLocationSnoozes()
        assertEquals(1, loaded.size)
        val result = loaded[0]
        assertEquals(1234567890L, result.remindInstant.epochSecond)
        assertEquals(listOf(1, 2), result.reminderIds)
        assertEquals(listOf(10, 20), result.reminderEventIds)
        assertEquals(42, result.notificationId)
    }

    @Test
    fun `multiple pending snoozes accumulate`() {
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(100), listOf(1), mutableListOf(10), 1))
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(200), listOf(2), mutableListOf(20), 2))
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(300), listOf(3), mutableListOf(30), 3))

        assertEquals(3, dataSource.getPendingLocationSnoozes().size)
    }

    @Test
    fun `clearAllPendingSnoozes empties list`() {
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(100), listOf(1), mutableListOf(10), 1))
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(200), listOf(2), mutableListOf(20), 2))

        dataSource.clearAllPendingLocationSnoozes()

        assertTrue(dataSource.getPendingLocationSnoozes().isEmpty())
    }

    @Test
    fun `remove by eventId drops entire entry when all events match`() {
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(100), listOf(1, 2), mutableListOf(10, 20), 1))
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(200), listOf(3), mutableListOf(30), 2))

        dataSource.removePendingLocationSnoozesForReminderEventIds(listOf(10, 20))

        val remaining = dataSource.getPendingLocationSnoozes()
        assertEquals(1, remaining.size)
        assertEquals(listOf(30), remaining[0].reminderEventIds)
    }

    @Test
    fun `remove by eventId keeps unmatched eventIds within same entry`() {
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(100), listOf(1, 2, 3), mutableListOf(10, 20, 30), 1))

        dataSource.removePendingLocationSnoozesForReminderEventIds(listOf(20))

        val remaining = dataSource.getPendingLocationSnoozes()
        assertEquals(1, remaining.size)
        assertEquals(listOf(1, 3), remaining[0].reminderIds)
        assertEquals(listOf(10, 30), remaining[0].reminderEventIds)
    }

    @Test
    fun `remove by eventId with no match leaves list unchanged`() {
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(100), listOf(1), mutableListOf(10), 1))

        dataSource.removePendingLocationSnoozesForReminderEventIds(listOf(99))

        assertEquals(1, dataSource.getPendingLocationSnoozes().size)
    }

    @Test
    fun `remove by eventId all entries removed results in empty`() {
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(100), listOf(1), mutableListOf(10), 1))
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(200), listOf(2), mutableListOf(20), 2))

        dataSource.removePendingLocationSnoozesForReminderEventIds(listOf(10, 20))

        assertTrue(dataSource.getPendingLocationSnoozes().isEmpty())
    }

    @Test
    fun `remove by eventId with empty list is no-op`() {
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(100), listOf(1), mutableListOf(10), 1))

        dataSource.removePendingLocationSnoozesForReminderEventIds(emptyList())

        assertEquals(1, dataSource.getPendingLocationSnoozes().size)
    }

    @Test
    fun `pending snooze with empty reminderIds`() {
        dataSource.addPendingLocationSnooze(PendingSnooze(Instant.ofEpochSecond(100), emptyList(), mutableListOf(10), 1))

        val loaded = dataSource.getPendingLocationSnoozes()
        assertEquals(1, loaded.size)
        assertTrue(loaded[0].reminderIds.isEmpty())
        assertEquals(listOf(10), loaded[0].reminderEventIds)
    }

    @Test
    // BUG: Gson.fromJson throws JsonSyntaxException instead of returning
    // null on corrupt data → the expected emptyList is never reached.
    fun `corrupt JSON in pending snoozes returns empty list`() {
        medTimerPrefs.edit().putString(PersistentDataDataSource.PENDING_SNOOZES, "not valid json at all").commit()

        val result = dataSource.getPendingLocationSnoozes()
        assertTrue(result.isEmpty())
    }

    // ── F: Data Flow ──────────────────────────────────────────────────

    @Test
    fun `data flow emits initial value`() = runTest {
        val value = dataSource.data.first()
        assertNotNull(value)
        assertEquals(7, value.analysisDays)
    }

    @Test
    fun `data flow emits on default preference change`() = runTest {
        dataSource.setIntroShown(true)
        val value = dataSource.data.first()
        assertEquals(true, value.introShown)
    }

    @Test
    fun `data flow emits on medTimer preference change`() = runTest {
        dataSource.setLastCustomDose("Test")
        val value = dataSource.data.first()
        assertEquals("Test", value.lastCustomDose)
    }

    @Test
    fun `data flow emits after setAnalysisDays`() = runTest {
        dataSource.setAnalysisDays(14)
        val value = dataSource.data.first()
        assertEquals(14, value.analysisDays)
    }

    @Test
    fun `data flow emits after setActiveStatisticsFragment`() = runTest {
        dataSource.setActiveStatisticsFragment(StatisticFragment.TABLE)
        val value = dataSource.data.first()
        assertEquals(StatisticFragment.TABLE, value.activeStatisticsFragment)
    }
}

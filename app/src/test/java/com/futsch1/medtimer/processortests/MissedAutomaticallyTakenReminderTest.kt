package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import com.futsch1.medtimer.core.common.di.TimeAccessModule
import com.futsch1.medtimer.core.datastore.di.DatastoreModule
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.dao.MedicineDao
import com.futsch1.medtimer.database.dao.ReminderDao
import com.futsch1.medtimer.database.dao.ReminderEventDao
import com.futsch1.medtimer.database.dao.TagDao
import com.futsch1.medtimer.database.di.DatabaseModule
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.database.toModel.toModel
import com.futsch1.medtimer.feature.reminders.ScheduleNextReminderNotificationProcessor
import com.futsch1.medtimer.schedulertests.TestHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import javax.inject.Inject
import kotlin.test.assertEquals

/**
 * Reproduction test for https://github.com/Futsch1/medTimer/issues/1418
 * ("Battery dead = missing medications").
 *
 * ## The reported bug
 *
 * When the smartphone battery dies (or the device is powered off) at the moment a reminder is
 * due, that reminder is not processed: the alarm fires while the device is off and the
 * [ReminderNotificationProcessor] never runs, so no [ReminderEvent] is created in the database.
 *
 * After the device boots again, [com.futsch1.medtimer.Autostart] runs
 * [com.futsch1.medtimer.AutostartService.restoreNotifications] (which only re-shows *existing*
 * raised events — nothing exists for a missed reminder) and then asks the
 * [ScheduleNextReminderNotificationProcessor] to schedule the next notification.
 *
 * The scheduler intentionally returns *today's already-past* reminder time for a reminder that
 * was not raised yet (see `StandardScheduling.canScheduleEveryDay`: `possibleDays[0] =
 * reminderBeforeCreation() && !raisedToday`). [AlarmProcessor.setNextReminderAlarm] detects the
 * past instant and processes the reminder immediately instead of scheduling an alarm.
 *
 * For a reminder with `automaticallyTaken = true` the immediate processing must mark the missed
 * medication as TAKEN (so the "missing medication" is added to the history, and the stock is
 * decreased) — that is the expected behaviour from the issue. This test pins that behaviour.
 *
 * ## Scenario layout
 *
 * A reminder is set for 10:00 (600 minutes) on the fixed test day (epoch day 0, UTC). The
 * "device was off" case is simulated by advancing the clock to 11:00 with *no* reminder event
 * in the repository — exactly the state after a missed alarm and a reboot.
 *
 * 1. [missedAutomaticallyTakenReminderIsAddedAsTaken] — the reported bug: the missed
 *    automatically-taken reminder must end up as a TAKEN event.
 * 2. [missedNonAutomaticallyTakenReminderIsRaised] — control case: a normal reminder must end
 *    up as a RAISED event (the notification is then shown to the user).
 * 3. [deviceOnAtReminderTime] — contrast case: with the device on *before* the reminder time,
 *    no event is created yet; only an alarm is scheduled.
 *
 * The tests run the same entry point the app uses after boot
 * ([ScheduleNextReminderNotificationProcessor.scheduleNextReminder]) and inspect the in-memory
 * fake repositories, so they fail/succeed purely on the presence and status of the created
 * reminder events.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@UninstallModules(
    DatabaseModule::class,
    DatastoreModule::class,
    TimeAccessModule::class
)
class MissedAutomaticallyTakenReminderTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    private val reminderContext = TestReminderContext()

    @BindValue
    val boundAlarmManager: AlarmManager = reminderContext.alarmManagerMock

    @BindValue
    val boundNotificationManager: NotificationManager = reminderContext.notificationManagerFake.mock

    @BindValue
    val boundMedicineRepository: com.futsch1.medtimer.core.domain.repository.MedicineRepository =
        reminderContext.repositoryFakes.medicineRepositoryMock

    @BindValue
    val boundReminderRepository: com.futsch1.medtimer.core.domain.repository.ReminderRepository =
        reminderContext.repositoryFakes.reminderRepositoryMock

    @BindValue
    val boundReminderEventRepository: com.futsch1.medtimer.core.domain.repository.ReminderEventRepository =
        reminderContext.repositoryFakes.reminderEventRepositoryMock

    @BindValue
    val boundPreferencesDataSource: com.futsch1.medtimer.core.datastore.PreferencesDataSource =
        reminderContext.preferencesDataSourceMock

    @BindValue
    val boundPersistentDataDataSource: com.futsch1.medtimer.core.datastore.PersistentDataDataSource =
        reminderContext.persistentDataDataSourceMock

    @BindValue
    val boundTimeAccess: com.futsch1.medtimer.core.common.time.TimeAccess = object : com.futsch1.medtimer.core.common.time.TimeAccess {
        override fun systemZone(): java.time.ZoneId = java.time.ZoneId.of("UTC")
        override fun localDate(): java.time.LocalDate = reminderContext.localDate
        override fun now(): java.time.Instant = reminderContext.instant
    }

    @BindValue
    val boundMedicineRoomDatabase: com.futsch1.medtimer.database.MedicineRoomDatabase = org.mockito.Mockito.mock()

    @BindValue
    val boundMedicineDao: MedicineDao = org.mockito.Mockito.mock()

    @BindValue
    val boundReminderDao: ReminderDao = org.mockito.Mockito.mock()

    @BindValue
    val boundReminderEventDao: ReminderEventDao = org.mockito.Mockito.mock()

    @BindValue
    val boundTagDao: TagDao = org.mockito.Mockito.mock()

    @BindValue
    val boundTagRepository: com.futsch1.medtimer.core.domain.repository.TagRepository = org.mockito.Mockito.mock()

    @BindValue
    val boundDatabaseManager: com.futsch1.medtimer.database.DatabaseManager = org.mockito.Mockito.mock()

    @BindValue
    val boundBackupRepository: com.futsch1.medtimer.core.domain.repository.BackupRepository = org.mockito.Mockito.mock()

    @BindValue
    @com.futsch1.medtimer.core.datastore.di.DefaultPreferences
    val boundDefaultSharedPreferences: android.content.SharedPreferences = org.mockito.Mockito.mock()

    @BindValue
    @com.futsch1.medtimer.core.datastore.di.MedTimerPreferences
    val boundMedTimerSharedPreferences: android.content.SharedPreferences = org.mockito.Mockito.mock()

    @Inject
    lateinit var scheduleNextReminderNotificationProcessor: ScheduleNextReminderNotificationProcessor

    /** Reminder time in minutes since midnight: 600 = 10:00 on the fixed test day (epoch day 0, UTC). */
    private val reminderTimeInMinutes = 600

    /**
     * The reported bug: a reminder due at 10:00 with `automaticallyTaken` that was missed
     * because the device was off must be **added as TAKEN** when the app reschedules after boot.
     */
    @Test
    fun missedAutomaticallyTakenReminderIsAddedAsTaken() {
        // Given: a medicine with an automatically-taken reminder due at 10:00
        reminderContext.repositoryFakes.medicines.add(MedicineEntity("Vitamin X 500 mg").also { it.medicineId = 1 })
        reminderContext.repositoryFakes.reminders.add(
            TestHelper.buildReminder(1, 1, "1", reminderTimeInMinutes, 1)
                .copy(automaticallyTaken = true)
                .toEntity()
        )

        // And: the phone was off at 10:00 — no reminder event was created, and it is now 11:00
        reminderContext.instant = Instant.ofEpochSecond(11 * 60 * 60)

        // When: the app reschedules after boot
        runBlocking {
            scheduleNextReminderNotificationProcessor.scheduleNextReminder()
        }

        // Then: the missed medication was added as taken (this is the expected behaviour from the issue)
        val events = reminderContext.repositoryFakes.reminderEvents.map { it.toModel() }
        assertEquals(1, events.size, "Expected exactly one reminder event for the missed reminder")
        assertEquals(
            10 * 60 * 60L,
            events[0].remindedTimestamp.epochSecond,
            "The event must carry the original (missed) reminder time of 10:00"
        )
        assertEquals(
            ReminderEvent.ReminderStatus.TAKEN,
            events[0].status,
            "The automatically-taken reminder must be marked as taken"
        )
    }

    /**
     * Control case: a *normal* (not automatically-taken) missed reminder must still be created
     * (as RAISED — the notification is then shown to the user). This documents the difference
     * between the two reminder kinds and guards the scheduling catch-up itself.
     */
    @Test
    fun missedNonAutomaticallyTakenReminderIsRaised() {
        // Given: a medicine with a normal reminder due at 10:00
        reminderContext.repositoryFakes.medicines.add(MedicineEntity("Vitamin X 500 mg").also { it.medicineId = 1 })
        reminderContext.repositoryFakes.reminders.add(
            TestHelper.buildReminder(1, 1, "1", reminderTimeInMinutes, 1)
                .copy(automaticallyTaken = false)
                .toEntity()
        )

        // And: the phone was off at 10:00 — no reminder event was created, and it is now 11:00
        reminderContext.instant = Instant.ofEpochSecond(11 * 60 * 60)

        // When: the app reschedules after boot
        runBlocking {
            scheduleNextReminderNotificationProcessor.scheduleNextReminder()
        }

        // Then: the missed reminder event exists and is raised (awaiting user action)
        val events = reminderContext.repositoryFakes.reminderEvents.map { it.toModel() }
        assertEquals(1, events.size, "Expected exactly one reminder event for the missed reminder")
        assertEquals(ReminderEvent.ReminderStatus.RAISED, events[0].status)
    }

    /**
     * Contrast case: with the device on *before* the reminder time, scheduling must not create
     * any event yet — it only arms the alarm. This guards against the test scenario being
     * trivially green because events are created unconditionally.
     */
    @Test
    fun deviceOnAtReminderTime() {
        // Given: a medicine with an automatically-taken reminder due at 10:00
        reminderContext.repositoryFakes.medicines.add(MedicineEntity("Vitamin X 500 mg").also { it.medicineId = 1 })
        reminderContext.repositoryFakes.reminders.add(
            TestHelper.buildReminder(1, 1, "1", reminderTimeInMinutes, 1)
                .copy(automaticallyTaken = true)
                .toEntity()
        )

        // And: the device is on and it is 09:00 — before the reminder time
        reminderContext.instant = Instant.ofEpochSecond(9 * 60 * 60)

        // When: the app schedules
        runBlocking {
            scheduleNextReminderNotificationProcessor.scheduleNextReminder()
        }

        // Then: no event exists yet and an alarm was armed for 10:00
        assertEquals(
            0,
            reminderContext.repositoryFakes.reminderEvents.size,
            "No event may be created while the reminder is still in the future"
        )
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.eq(reminderTimeInMinutes * 60 * 1000L),
            org.mockito.ArgumentMatchers.any()
        )
        verify(reminderContext.alarmManagerMock, never()).cancel(org.mockito.ArgumentMatchers.any<PendingIntent>())
    }
}

package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.NotificationManager
import android.media.AudioManager
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.di.SystemServicesModule
import com.futsch1.medtimer.reminders.RefillProcessor
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import javax.inject.Inject
import kotlin.test.assertEquals

@HiltAndroidTest
@UninstallModules(SystemServicesModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RefillProcessorTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    val reminderContext = TestReminderContext()

    @BindValue
    val boundReminderContext: ReminderContext = reminderContext.mock

    @BindValue
    val boundAlarmManager: AlarmManager = reminderContext.alarmManagerMock

    @BindValue
    val boundNotificationManager: NotificationManager = reminderContext.notificationManagerFake.mock

    @BindValue
    val boundAudioManager: AudioManager = org.mockito.Mockito.mock()

    @Inject
    lateinit var refillProcessor: RefillProcessor

    @Test
    fun directRefill() {
        reminderContext.instant = Instant.ofEpochSecond(10)

        reminderContext.medicineRepositoryFake.medicines.add(TestHelper.buildFullMedicine(1, "Test").medicine)
        reminderContext.medicineRepositoryFake.medicines[0].refillSizes.add(10.0)
        reminderContext.medicineRepositoryFake.medicines[0].amount = 100.0

        runBlocking {
            refillProcessor.processRefill(1)
        }

        assertEquals(110.0, reminderContext.medicineRepositoryFake.medicines[0].amount)
        assertEquals(10, reminderContext.medicineRepositoryFake.reminderEvents[0].processedTimestamp)
        assertEquals(10, reminderContext.medicineRepositoryFake.reminderEvents[0].remindedTimestamp)
        assertEquals("100 ➡ 110", reminderContext.medicineRepositoryFake.reminderEvents[0].amount)
        assertEquals(Reminder.ReminderType.REFILL, reminderContext.medicineRepositoryFake.reminderEvents[0].reminderType)
    }

    @Test
    fun refillViaEvent() {
        reminderContext.medicineRepositoryFake.medicines.add(TestHelper.buildFullMedicine(1, "Test").medicine)
        reminderContext.medicineRepositoryFake.medicines[0].refillSizes.add(10.0)
        reminderContext.medicineRepositoryFake.medicines[0].amount = 100.0
        reminderContext.medicineRepositoryFake.reminders.add(TestHelper.buildReminder(1, 1, "1", 0, 1))
        reminderContext.medicineRepositoryFake.reminderEvents.add(TestHelper.buildReminderEvent(1, 0, 1))

        runBlocking {
            refillProcessor.processRefill(ProcessedNotificationData(listOf(1)))
        }

        assertEquals(110.0, reminderContext.medicineRepositoryFake.medicines[0].amount)
        assertEquals(ReminderEvent.ReminderStatus.ACKNOWLEDGED, reminderContext.medicineRepositoryFake.reminderEvents[0].status)
    }
}
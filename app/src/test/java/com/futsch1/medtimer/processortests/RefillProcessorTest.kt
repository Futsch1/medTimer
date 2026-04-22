package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.SharedPreferences
import com.futsch1.medtimer.database.DatabaseManager
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineRoomDatabase
import com.futsch1.medtimer.database.ReminderEntityType
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.database.TagRepository
import com.futsch1.medtimer.database.dao.MedicineDao
import com.futsch1.medtimer.database.dao.ReminderDao
import com.futsch1.medtimer.database.dao.ReminderEventDao
import com.futsch1.medtimer.database.dao.TagDao
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.di.DatabaseModule
import com.futsch1.medtimer.di.DatastoreModule
import com.futsch1.medtimer.di.TimeAccessModule
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.RefillProcessor
import com.futsch1.medtimer.reminders.TimeAccess
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
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.test.assertEquals

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@UninstallModules(
    DatabaseModule::class,
    DatastoreModule::class,
    TimeAccessModule::class
)
class RefillProcessorTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    val reminderContext = TestReminderContext()

    @BindValue
    val boundAlarmManager: AlarmManager = reminderContext.alarmManagerMock

    @BindValue
    val boundNotificationManager: NotificationManager = reminderContext.notificationManagerFake.mock

    @BindValue
    val boundMedicineRepository: MedicineRepository = reminderContext.repositoryFakes.medicineRepositoryMock

    @BindValue
    val boundReminderRepository: ReminderRepository = reminderContext.repositoryFakes.reminderRepositoryMock

    @BindValue
    val boundReminderEventRepository: ReminderEventRepository = reminderContext.repositoryFakes.reminderEventRepositoryMock

    @BindValue
    val boundPreferencesDataSource: PreferencesDataSource = reminderContext.preferencesDataSourceMock

    @BindValue
    val boundPersistentDataDataSource: PersistentDataDataSource = reminderContext.persistentDataDataSourceMock

    @BindValue
    val boundTimeAccess: TimeAccess = object : TimeAccess {
        override fun systemZone(): ZoneId = ZoneId.of("UTC")
        override fun localDate(): LocalDate = reminderContext.localDate
        override fun now(): Instant = reminderContext.instant
    }

    @BindValue
    val boundMedicineRoomDatabase: MedicineRoomDatabase = mock()

    @BindValue
    val boundMedicineDao: MedicineDao = mock()

    @BindValue
    val boundReminderDao: ReminderDao = mock()

    @BindValue
    val boundReminderEventDao: ReminderEventDao = mock()

    @BindValue
    val boundTagDao: TagDao = mock()

    @BindValue
    val boundTagRepository: TagRepository = mock()

    @BindValue
    val boundDatabaseManager: DatabaseManager = mock()

    @BindValue
    @com.futsch1.medtimer.di.DefaultPreferences
    val boundDefaultSharedPreferences: SharedPreferences = mock()

    @BindValue
    @com.futsch1.medtimer.di.MedTimerPreferencess
    val boundMedTimerSharedPreferences: SharedPreferences = mock()

    @Inject
    lateinit var refillProcessor: RefillProcessor

    @Test
    fun directRefill() {
        reminderContext.instant = Instant.ofEpochSecond(10)

        reminderContext.repositoryFakes.medicines.add(MedicineEntity("Test").also { it.medicineId = 1 })
        reminderContext.repositoryFakes.medicines[0].refillSizes = mutableListOf(10.0)
        reminderContext.repositoryFakes.medicines[0].amount = 100.0

        runBlocking {
            refillProcessor.processRefill(1)
        }

        assertEquals(110.0, reminderContext.repositoryFakes.medicines[0].amount)
        assertEquals(10, reminderContext.repositoryFakes.reminderEvents[0].processedTimestamp)
        assertEquals(10, reminderContext.repositoryFakes.reminderEvents[0].remindedTimestamp)
        assertEquals("100 ➡ 110", reminderContext.repositoryFakes.reminderEvents[0].amount)
        assertEquals(ReminderEntityType.REFILL, reminderContext.repositoryFakes.reminderEvents[0].reminderType)
    }

    @Test
    fun refillViaEvent() {
        reminderContext.repositoryFakes.medicines.add(MedicineEntity("Test").also { it.medicineId = 1 })
        reminderContext.repositoryFakes.medicines[0].refillSizes = mutableListOf(10.0)
        reminderContext.repositoryFakes.medicines[0].amount = 100.0
        reminderContext.repositoryFakes.reminders.add(TestHelper.buildReminder(1, 1, "1", 0, 1).toEntity())
        reminderContext.repositoryFakes.reminderEvents.add(TestHelper.buildReminderEvent(1, 0, 1).toEntity())

        runBlocking {
            refillProcessor.processRefill(ProcessedNotificationData(listOf(1)))
        }

        assertEquals(110.0, reminderContext.repositoryFakes.medicines[0].amount)
        assertEquals(ReminderEventEntity.ReminderEntityStatus.ACKNOWLEDGED, reminderContext.repositoryFakes.reminderEvents[0].status)
    }
}
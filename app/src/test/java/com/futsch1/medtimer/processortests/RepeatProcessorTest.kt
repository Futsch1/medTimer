package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.NotificationManager
import com.futsch1.medtimer.database.DatabaseManager
import com.futsch1.medtimer.database.MedicineDao
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineRoomDatabase
import com.futsch1.medtimer.database.ReminderDao
import com.futsch1.medtimer.database.ReminderEventDao
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.database.TagDao
import com.futsch1.medtimer.database.TagRepository
import com.futsch1.medtimer.di.DatabaseModule
import com.futsch1.medtimer.di.DatastoreModule
import com.futsch1.medtimer.di.TimeAccessModule
import com.futsch1.medtimer.reminders.RepeatProcessor
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@UninstallModules(
    DatabaseModule::class,
    DatastoreModule::class,
    TimeAccessModule::class
)
class RepeatProcessorTest {
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
    val boundPreferencesDataSource: com.futsch1.medtimer.preferences.PreferencesDataSource = reminderContext.preferencesDataSourceMock

    @BindValue
    val boundPersistentDataDataSource: com.futsch1.medtimer.preferences.PersistentDataDataSource = reminderContext.persistentDataDataSourceMock

    @BindValue
    val boundTimeAccess: com.futsch1.medtimer.reminders.TimeAccess = object : com.futsch1.medtimer.reminders.TimeAccess {
        override fun systemZone(): java.time.ZoneId = java.time.ZoneId.of("UTC")
        override fun localDate(): java.time.LocalDate = reminderContext.localDate
        override fun now(): java.time.Instant = reminderContext.instant
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
    val boundDefaultSharedPreferences: android.content.SharedPreferences = mock()

    @BindValue
    @com.futsch1.medtimer.di.MedTimerPreferencess
    val boundMedTimerSharedPreferences: android.content.SharedPreferences = mock()

    @Inject
    lateinit var repeatProcessor: RepeatProcessor

    @Test
    fun repeat() {
        val reminderNotificationData = fillWithTwoReminders(reminderContext)

        runBlocking {
            repeatProcessor.processRepeat(reminderNotificationData, 10.seconds)
        }

        assertEquals(reminderContext.repositoryFakes.reminderEvents[0].remainingRepeats, -1)
        assertEquals(reminderContext.repositoryFakes.reminderEvents[1].remainingRepeats, -1)
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(10_000L), any())
    }
}
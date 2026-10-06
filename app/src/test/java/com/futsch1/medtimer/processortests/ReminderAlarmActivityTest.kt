package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.SharedPreferences
import android.os.Looper
import android.widget.TextView
import com.futsch1.medtimer.core.common.di.TimeAccessModule
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.datastore.di.DatastoreModule
import com.futsch1.medtimer.core.datastore.di.DefaultPreferences
import com.futsch1.medtimer.core.datastore.di.MedTimerPreferences
import com.futsch1.medtimer.core.domain.repository.BackupRepository
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.database.DatabaseManager
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.MedicineRoomDatabase
import com.futsch1.medtimer.database.dao.MedicineDao
import com.futsch1.medtimer.database.dao.ReminderDao
import com.futsch1.medtimer.database.dao.ReminderEventDao
import com.futsch1.medtimer.database.dao.TagDao
import com.futsch1.medtimer.database.di.DatabaseModule
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.feature.reminders.R as RemindersR
import com.futsch1.medtimer.feature.reminders.alarm.ReminderAlarmActivity
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationFactory
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationPart
import com.futsch1.medtimer.schedulertests.TestHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the alarm screen against showing the reminders of the alarm before. The alarm activity is
 * single instance, so the alarm that follows while it is still on screen reaches it through
 * onNewIntent instead of onCreate.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = HiltTestApplication::class)
@UninstallModules(
    DatabaseModule::class,
    DatastoreModule::class,
    TimeAccessModule::class
)
class ReminderAlarmActivityTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val testReminderContext = TestReminderContext()

    @BindValue
    val boundAlarmManager: AlarmManager = testReminderContext.alarmManagerMock

    @BindValue
    val boundNotificationManager: NotificationManager = testReminderContext.notificationManagerFake.mock

    @BindValue
    val boundTimeAccess: TimeAccess = object : TimeAccess {
        override fun systemZone(): ZoneId = ZoneId.of("UTC")
        override fun localDate(): LocalDate = testReminderContext.localDate
        override fun now(): Instant = testReminderContext.instant
    }

    @BindValue
    val boundMedicineRepository: MedicineRepository = testReminderContext.repositoryFakes.medicineRepositoryMock

    @BindValue
    val boundReminderRepository: ReminderRepository = testReminderContext.repositoryFakes.reminderRepositoryMock

    @BindValue
    val boundReminderEventRepository: ReminderEventRepository = testReminderContext.repositoryFakes.reminderEventRepositoryMock

    @BindValue
    val boundPreferencesDataSource: PreferencesDataSource = testReminderContext.preferencesDataSourceMock

    @BindValue
    val boundPersistentDataDataSource: PersistentDataDataSource = testReminderContext.persistentDataDataSourceMock

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
    val boundBackupRepository: BackupRepository = mock()

    @BindValue
    @DefaultPreferences
    val boundDefaultSharedPreferences: SharedPreferences = mock()

    @BindValue
    @MedTimerPreferences
    val boundMedTimerSharedPreferences: SharedPreferences = mock()

    @BindValue
    val boundReminderNotificationFactory: ReminderNotificationFactory =
        object : ReminderNotificationFactory(mock(), mock(), mock(), mock(), mock()) {
            override suspend fun create(reminderNotificationData: ReminderNotificationData): ReminderNotification? {
                if (!reminderNotificationData.valid) return null

                val parts = mutableListOf<ReminderNotificationPart>()
                for (i in reminderNotificationData.reminderIds.indices) {
                    val reminder = testReminderContext.repositoryFakes.reminderRepositoryMock.fetch(reminderNotificationData.reminderIds[i])
                        ?: return null
                    val medicine = testReminderContext.repositoryFakes.medicineRepositoryMock.fetch(reminder.medicineRelId)
                        ?: return null
                    val event = testReminderContext.repositoryFakes.reminderEventRepositoryMock.fetch(reminderNotificationData.reminderEventIds[i])
                        ?: return null
                    parts.add(ReminderNotificationPart(reminder, event, medicine))
                }
                return ReminderNotification(parts, reminderNotificationData)
            }
        }

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun `should show the reminders of the arriving alarm when it reaches the running alarm screen`() {
        seedReminders()

        val context = RuntimeEnvironment.getApplication()
        val controller = Robolectric.buildActivity(
            ReminderAlarmActivity::class.java,
            ReminderAlarmActivity.getIntent(context, alarmData(reminderId = 1, reminderEventId = 1))
        ).setup()

        assertTrue(awaitAlarmTitle(controller.get(), FIRST_MEDICINE))

        controller.newIntent(ReminderAlarmActivity.getIntent(context, alarmData(reminderId = 2, reminderEventId = 2)))

        assertTrue(awaitAlarmTitle(controller.get(), SECOND_MEDICINE))
        // The screen of the alarm that arrived stays up
        assertFalse(controller.get().isFinishing)
    }

    private fun seedReminders() {
        val repositoryFakes = testReminderContext.repositoryFakes
        repositoryFakes.medicines.add(MedicineEntity(FIRST_MEDICINE).also { it.medicineId = 1 })
        repositoryFakes.medicines.add(MedicineEntity(SECOND_MEDICINE).also { it.medicineId = 2 })
        repositoryFakes.reminders.add(TestHelper.buildReminder(1, 1, "1", 600, 1).toEntity())
        repositoryFakes.reminders.add(TestHelper.buildReminder(2, 2, "1", 600, 1).toEntity())
        repositoryFakes.reminderEvents.add(TestHelper.buildReminderEvent(1, 0, 1).toEntity())
        repositoryFakes.reminderEvents.add(TestHelper.buildReminderEvent(2, 0, 2).toEntity())
    }

    private fun alarmData(reminderId: Int, reminderEventId: Int) = ReminderNotificationData.fromArrays(
        listOf(reminderId),
        listOf(reminderEventId),
        Instant.ofEpochSecond(0)
    )

    /** The alarm screen builds its texts off the main thread, so wait for the shown medicine. */
    private fun awaitAlarmTitle(activity: ReminderAlarmActivity, medicineName: String): Boolean {
        repeat(TITLE_TRIES) {
            shadowOf(Looper.getMainLooper()).idle()
            val title = activity.findViewById<TextView>(RemindersR.id.notificationTitle).text?.toString()
            if (title != null && title.contains(medicineName)) return true
            Thread.sleep(TITLE_POLL_MILLIS)
        }
        return false
    }

    private companion object {
        const val FIRST_MEDICINE = "Medicine A"
        const val SECOND_MEDICINE = "Medicine B"
        const val TITLE_POLL_MILLIS = 20L
        const val TITLE_TRIES = 100
    }
}

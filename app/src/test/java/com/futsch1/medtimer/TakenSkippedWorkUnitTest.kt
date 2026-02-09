package com.futsch1.medtimer

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.service.notification.StatusBarNotification
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import com.futsch1.medtimer.reminders.NotificationSkippedWorker
import com.futsch1.medtimer.reminders.NotificationTakenWorker
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.time.Instant

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [34])
// Required for Robolectric extension
class TakenSkippedWorkUnitTest {
    @Captor
    lateinit var listCaptor: ArgumentCaptor<List<ReminderEvent>>

    @Mock
    private var mockApplication: Application? = null

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        mockApplication = Mockito.mock(Application::class.java)

        val mockNotificationManager = Mockito.mock(NotificationManager::class.java)
        Mockito.`when`(mockApplication!!.getSystemService(NotificationManager::class.java))
            .thenReturn(mockNotificationManager)
        Mockito.`when`(mockNotificationManager.getActiveNotifications()).thenReturn(arrayOf<StatusBarNotification?>())

        val mockAlarmManager = Mockito.mock(AlarmManager::class.java)
        Mockito.`when`(mockApplication!!.getSystemService(AlarmManager::class.java)).thenReturn(mockAlarmManager)
    }

    @Test
    fun testDoWorkTaken() {
        val workerParams = Mockito.mock(WorkerParameters::class.java)
        val processedNotificationData = ProcessedNotificationData(listOf(REMINDER_EVENT_ID))
        val builder = Data.Builder()
        processedNotificationData.toBuilder(builder)
        Mockito.`when`(workerParams.inputData).thenReturn(builder.build())
        val takenWork = NotificationTakenWorker(mockApplication!!, workerParams)

        testWork(takenWork, ReminderStatus.TAKEN)
    }

    private fun testWork(worker: Worker, status: ReminderStatus?) {
        val reminderEvent = ReminderEvent()
        val notificationId = 14
        reminderEvent.notificationId = notificationId
        val reminderId = 11
        reminderEvent.reminderId = reminderId
        reminderEvent.reminderEventId = REMINDER_EVENT_ID
        reminderEvent.status = ReminderStatus.RAISED
        reminderEvent.processedTimestamp = Instant.now().epochSecond
        reminderEvent.amount = "4"
        val reminder = Reminder(5)

        Mockito.mockConstruction(
            MedicineRepository::class.java
        ) { mock: MedicineRepository?, _: MockedConstruction.Context? ->
            Mockito.`when`<ReminderEvent?>(mock!!.getReminderEvent(REMINDER_EVENT_ID)).thenReturn(reminderEvent)
            Mockito.`when`<Reminder?>(mock.getReminder(reminderId)).thenReturn(reminder)
        }.use { mockedMedicineRepositories ->
            Mockito.mockStatic(WorkManagerAccess::class.java).use { mockedWorkManagerAccess ->
                val mockWorkManager = Mockito.mock(WorkManager::class.java)
                mockedWorkManagerAccess.`when`<Any?> { WorkManagerAccess.getWorkManager(mockApplication) }
                    .thenReturn(mockWorkManager)
                // Expected to pass
                val result = worker.doWork()
                Assertions.assertInstanceOf(ListenableWorker.Result.Success::class.java, result)

                // Check if reminder event was updated
                val mockedMedicineRepository = mockedMedicineRepositories.constructed()[0]

                Mockito.verify(mockedMedicineRepository, Mockito.times(1)).updateReminderEvents(listCaptor.capture() ?: emptyList())
                Assertions.assertEquals(notificationId, listCaptor.getValue()!![0].notificationId)
                Assertions.assertEquals(reminderId, listCaptor.getValue()!![0].reminderId)
                Assertions.assertEquals(REMINDER_EVENT_ID, listCaptor.getValue()!![0].reminderEventId)
                Assertions.assertEquals(status, listCaptor.getValue()!![0].status)

                if (status == ReminderStatus.TAKEN) {
                    val captor2 = ArgumentCaptor.forClass(WorkRequest::class.java)
                    val dummyWorkRequest = OneTimeWorkRequest.Builder(NotificationTakenWorker::class.java).build()
                    // Use Elvis operator to avoid NPE from Kotlin's null-safety check on non-nullable parameter
                    Mockito.verify(mockWorkManager, Mockito.times(1)).enqueue(captor2.capture() ?: dummyWorkRequest)
                    Assertions.assertInstanceOf(OneTimeWorkRequest::class.java, captor2.getValue())
                    // Compare doubles as the input data stores the amount as a Double
                    Assertions.assertEquals(4.0, captor2.getValue()!!.workSpec.input.getDouble(ActivityCodes.EXTRA_AMOUNT, 0.0))
                    Assertions.assertEquals(reminder.medicineRelId, captor2.getValue()!!.workSpec.input.getInt(ActivityCodes.EXTRA_MEDICINE_ID, -1))
                }
            }
        }
    }

    @Test
    fun testDoWorkSkipped() {
        val workerParams = Mockito.mock(WorkerParameters::class.java)
        val processedNotificationData = ProcessedNotificationData(listOf(REMINDER_EVENT_ID))
        val builder = Data.Builder()
        processedNotificationData.toBuilder(builder)
        Mockito.`when`(workerParams.inputData).thenReturn(builder.build())
        val skippedWork = NotificationSkippedWorker(mockApplication!!, workerParams)

        testWork(skippedWork, ReminderStatus.SKIPPED)
    }

    companion object {
        private const val REMINDER_EVENT_ID = 12
    }
}

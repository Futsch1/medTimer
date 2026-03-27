package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.NotificationManager
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.RepeatProcessor
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepeatProcessorTest {
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

    @Inject
    lateinit var repeatProcessor: RepeatProcessor

    @Test
    fun repeat() {
        val reminderNotificationData = fillWithTwoReminders(reminderContext)

        runBlocking {
            repeatProcessor.processRepeat(reminderNotificationData, 10.seconds)
        }

        assertEquals(reminderContext.medicineRepositoryFake.reminderEvents[0].remainingRepeats, -1)
        assertEquals(reminderContext.medicineRepositoryFake.reminderEvents[1].remainingRepeats, -1)
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(10_000L), any())
    }
}
package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.reminders.AlarmProcessor
import com.futsch1.medtimer.reminders.TimeAccess
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [36])
class AlarmProcessorTest {
    @Test
    fun exactAlarms() {
        val reminderContext = TestReminderContext()
        reminderContext.userPreferences = UserPreferences.default().copy(exactReminders = true)
        val processedNotificationData = fillWithTwoReminders(reminderContext)
        processedNotificationData.remindInstant = processedNotificationData.remindInstant.plusSeconds(10)

        val timeAccess = object : TimeAccess {
            override fun systemZone(): ZoneId = ZoneId.of("UTC")
            override fun localDate(): LocalDate = LocalDate.ofEpochDay(0)
            override fun now(): Instant = Instant.ofEpochSecond(0)
        }

        runBlocking {
            AlarmProcessor(
                reminderContext.contextMock,
                reminderContext.alarmManagerMock,
                timeAccess,
                reminderContext.preferencesDataSourceMock
            ).setAlarmForReminderNotification(
                processedNotificationData
            )
        }

        verify(reminderContext.alarmManagerMock, times(1)).setExactAndAllowWhileIdle(
            anyInt(),
            anyLong(),
            any()
        )
    }
}

package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.feature.reminders.AlarmProcessor
import com.futsch1.medtimer.core.common.time.TimeAccess
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    private val timeAccess = object : TimeAccess {
        override fun systemZone(): ZoneId = ZoneId.of("UTC")
        override fun localDate(): LocalDate = LocalDate.ofEpochDay(0)
        override fun now(): Instant = Instant.ofEpochSecond(0)
    }

    @Test
    fun exactAlarms() {
        val reminderContext = TestReminderContext()
        reminderContext.userPreferences = UserPreferences.default().copy(exactReminders = true)
        val processedNotificationData = fillWithTwoReminders(reminderContext)
        processedNotificationData.remindInstant = processedNotificationData.remindInstant.plusSeconds(10)

        runBlocking {
            AlarmProcessor(
                reminderContext.contextMock,
                reminderContext.alarmManagerMock,
                timeAccess,
                reminderContext.preferencesDataSourceMock
            ).setSecondaryAlarm(
                processedNotificationData
            )
        }

        verify(reminderContext.alarmManagerMock, times(1)).setExactAndAllowWhileIdle(
            anyInt(),
            anyLong(),
            any()
        )
    }

    @Test
    fun recordsRequestedSecondaryAlarmBeforeTestDelay() {
        val reminderContext = TestReminderContext()
        val processedNotificationData = fillWithTwoReminders(reminderContext)
        val requested = Instant.ofEpochSecond(600)
        processedNotificationData.remindInstant = requested

        AlarmProcessor.delay = 1_000
        AlarmProcessor.repeats = 0
        try {
            AlarmProcessor(
                reminderContext.contextMock,
                reminderContext.alarmManagerMock,
                timeAccess,
                reminderContext.preferencesDataSourceMock
            ).setSecondaryAlarm(processedNotificationData)
        } finally {
            AlarmProcessor.delay = -1
            AlarmProcessor.repeats = -1
        }

        assertEquals(requested, AlarmProcessor.lastSecondaryAlarmForTests)
        assertEquals(Instant.ofEpochSecond(1), processedNotificationData.remindInstant)
    }
}

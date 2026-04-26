package com.futsch1.medtimer.location

import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.reminders.AlarmProcessor
import com.futsch1.medtimer.reminders.LocationSnoozeProcessor
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationSnoozeProcessorTest {
    private lateinit var alarmProcessor: AlarmProcessor
    private lateinit var persistentDataDataSource: PersistentDataDataSource
    private lateinit var geofenceRegistrar: GeofenceRegistrar
    private lateinit var processor: LocationSnoozeProcessor

    @Before
    fun setUp() {
        alarmProcessor = mock()
        persistentDataDataSource = mock()
        geofenceRegistrar = mock()
        processor = LocationSnoozeProcessor(alarmProcessor, persistentDataDataSource, geofenceRegistrar)
    }

    @Test
    fun emptyPendingList() {
        whenever(persistentDataDataSource.getPendingLocationSnoozes()).thenReturn(emptyList())

        processor.processLocationSnooze()

        verify(alarmProcessor, never()).setAlarmForReminderNotification(any())
        verify(persistentDataDataSource).clearAllPendingLocationSnoozes()
        verify(geofenceRegistrar).unregisterHomeGeofence()
    }

    @Test
    fun singlePendingSnooze() {
        val futureInstant = Instant.now().plusSeconds(3600)
        val data = ReminderNotificationData(futureInstant, listOf(1), mutableListOf(10), 1)
        whenever(persistentDataDataSource.getPendingLocationSnoozes()).thenReturn(listOf(data))

        processor.processLocationSnooze()

        verify(alarmProcessor, times(1)).setAlarmForReminderNotification(any())
        verify(persistentDataDataSource).clearAllPendingLocationSnoozes()
        verify(geofenceRegistrar).unregisterHomeGeofence()
    }

    @Test
    fun remindInstant() {
        val futureInstant = Instant.now().plusSeconds(3600)
        val data = ReminderNotificationData(futureInstant, listOf(1), mutableListOf(10), 1)
        whenever(persistentDataDataSource.getPendingLocationSnoozes()).thenReturn(listOf(data))

        val beforeCall = Instant.now()
        processor.processLocationSnooze()
        val afterCall = Instant.now()

        // remindInstant must have been set to now() — not in the future
        val firedInstant = data.remindInstant
        assertTrue(!firedInstant.isAfter(afterCall), "remindInstant should be <= now after call")
        assertTrue(!firedInstant.isBefore(beforeCall), "remindInstant should be >= before call")
    }

    @Test
    fun multipleSnoozes() {
        val snoozes = listOf(
            ReminderNotificationData(Instant.now().plusSeconds(100), listOf(1), mutableListOf(10), 1),
            ReminderNotificationData(Instant.now().plusSeconds(200), listOf(2), mutableListOf(20), 2),
            ReminderNotificationData(Instant.now().plusSeconds(300), listOf(3), mutableListOf(30), 3)
        )
        whenever(persistentDataDataSource.getPendingLocationSnoozes()).thenReturn(snoozes)

        processor.processLocationSnooze()

        verify(alarmProcessor, times(3)).setAlarmForReminderNotification(any())
        verify(geofenceRegistrar).unregisterHomeGeofence()
    }
}

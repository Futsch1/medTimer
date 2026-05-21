package com.futsch1.medtimer.location

import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.PendingSnooze
import com.futsch1.medtimer.reminders.AlarmProcessor
import com.futsch1.medtimer.reminders.LocationSnoozeProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

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

        runBlocking { processor.processLocationSnooze() }

        runBlocking { verify(alarmProcessor, never()).setAlarmForReminderNotification(any(), isNull()) }
        verify(persistentDataDataSource).clearAllPendingLocationSnoozes()
        verify(geofenceRegistrar).unregisterHomeGeofence()
    }

    @Test
    fun singlePendingSnooze() {
        val futureInstant = Instant.now().plusSeconds(3600)
        val data = PendingSnooze(futureInstant, listOf(1), mutableListOf(10), 1)
        whenever(persistentDataDataSource.getPendingLocationSnoozes()).thenReturn(listOf(data))

        runBlocking { processor.processLocationSnooze() }

        runBlocking { verify(alarmProcessor, times(1)).setAlarmForReminderNotification(any(), isNull()) }
        verify(persistentDataDataSource).clearAllPendingLocationSnoozes()
        verify(geofenceRegistrar).unregisterHomeGeofence()
    }

    @Test
    fun multipleSnoozes() {
        val snoozes = listOf(
            PendingSnooze(Instant.now().plusSeconds(100), listOf(1), mutableListOf(10), 1),
            PendingSnooze(Instant.now().plusSeconds(200), listOf(2), mutableListOf(20), 2),
            PendingSnooze(Instant.now().plusSeconds(300), listOf(3), mutableListOf(30), 3)
        )
        whenever(persistentDataDataSource.getPendingLocationSnoozes()).thenReturn(snoozes)

        runBlocking { processor.processLocationSnooze() }

        runBlocking { verify(alarmProcessor, times(3)).setAlarmForReminderNotification(any(), isNull()) }
        verify(geofenceRegistrar).unregisterHomeGeofence()
    }
}

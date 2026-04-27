package com.futsch1.medtimer.location

import com.futsch1.medtimer.reminders.LocationSnoozeProcessor
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeofenceBroadcastReceiverTest {
    private lateinit var locationSnoozeProcessor: LocationSnoozeProcessor
    private lateinit var receiver: GeofenceBroadcastReceiver

    @Before
    fun setUp() {
        locationSnoozeProcessor = mock()
        receiver = GeofenceBroadcastReceiver()
        receiver.locationSnoozeProcessor = locationSnoozeProcessor
    }

    @Test
    fun enterTransitionTriggersProcessing() {
        val event = mock<GeofencingEvent>()
        whenever(event.hasError()).thenReturn(false)
        whenever(event.geofenceTransition).thenReturn(Geofence.GEOFENCE_TRANSITION_ENTER)

        receiver.handleGeofencingEvent(event)

        verify(locationSnoozeProcessor).processLocationSnooze()
    }

    @Test
    fun exitTransitionDoesNotTriggerProcessing() {
        val event = mock<GeofencingEvent>()
        whenever(event.hasError()).thenReturn(false)
        whenever(event.geofenceTransition).thenReturn(Geofence.GEOFENCE_TRANSITION_EXIT)

        receiver.handleGeofencingEvent(event)

        verify(locationSnoozeProcessor, never()).processLocationSnooze()
    }

    @Test
    fun errorEventDoesNotTriggerProcessing() {
        val event = mock<GeofencingEvent>()
        whenever(event.hasError()).thenReturn(true)
        whenever(event.errorCode).thenReturn(1)

        receiver.handleGeofencingEvent(event)

        verify(locationSnoozeProcessor, never()).processLocationSnooze()
    }
}

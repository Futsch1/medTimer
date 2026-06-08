package com.futsch1.medtimer.location

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.core.common.ProcessorCode
import com.futsch1.medtimer.core.location.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeofenceBroadcastReceiverTest {
    private lateinit var application: Application
    private lateinit var receiver: GeofenceBroadcastReceiver

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        receiver = GeofenceBroadcastReceiver()
    }

    @Test
    fun enterTransitionForwardsBroadcastToRemindersReceiver() {
        val event = mock<GeofencingEvent>()
        whenever(event.hasError()).thenReturn(false)
        whenever(event.geofenceTransition).thenReturn(Geofence.GEOFENCE_TRANSITION_ENTER)

        receiver.handleGeofencingEvent(application, event)

        val forwarded = shadowOf(application).broadcastIntents.lastOrNull()
        assertEquals(ProcessorCode.GeofenceEntered.action, forwarded?.action)
        assertEquals(
            "com.futsch1.medtimer.feature.reminders.impl.ReminderProcessorBroadcastReceiver",
            forwarded?.component?.className
        )
    }

    @Test
    fun exitTransitionDoesNotForward() {
        val event = mock<GeofencingEvent>()
        whenever(event.hasError()).thenReturn(false)
        whenever(event.geofenceTransition).thenReturn(Geofence.GEOFENCE_TRANSITION_EXIT)

        receiver.handleGeofencingEvent(application, event)

        assertNull(shadowOf(application).broadcastIntents.lastOrNull())
    }

    @Test
    fun errorEventDoesNotForward() {
        val event = mock<GeofencingEvent>()
        whenever(event.hasError()).thenReturn(true)
        whenever(event.errorCode).thenReturn(1)

        receiver.handleGeofencingEvent(application, event)

        assertNull(shadowOf(application).broadcastIntents.lastOrNull())
    }
}

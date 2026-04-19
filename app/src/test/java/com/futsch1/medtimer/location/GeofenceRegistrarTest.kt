package com.futsch1.medtimer.location

import android.Manifest
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.model.HomeLocation
import com.futsch1.medtimer.preferences.HomeLocationDataSource
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.tasks.Task
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeofenceRegistrarTest {
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var homeLocationDataSource: HomeLocationDataSource
    private lateinit var googleApiAvailability: GoogleApiAvailability
    private lateinit var registrar: GeofenceRegistrar

    @Before
    fun setUp() {
        geofencingClient = mock()
        homeLocationDataSource = mock()
        googleApiAvailability = mock()

        val mockTask = mock<Task<Void>>()
        whenever(geofencingClient.addGeofences(any(), any())).thenReturn(mockTask)
        whenever(geofencingClient.removeGeofences(any<List<String>>())).thenReturn(mockTask)
        whenever(mockTask.addOnSuccessListener(any())).thenReturn(mockTask)
        whenever(mockTask.addOnFailureListener(any())).thenReturn(mockTask)

        registrar = GeofenceRegistrar(
            ApplicationProvider.getApplicationContext(),
            geofencingClient,
            homeLocationDataSource,
            googleApiAvailability
        )
    }

    @Test
    fun `isLocationServiceAvailable returns true when Play Services available`() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SUCCESS)
        assertTrue(registrar.isLocationServiceAvailable())
    }

    @Test
    fun `isLocationServiceAvailable returns false when Play Services unavailable`() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SERVICE_MISSING)
        assertFalse(registrar.isLocationServiceAvailable())
    }

    @Test
    fun `registerHomeGeofence returns false when Play Services unavailable`() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SERVICE_MISSING)

        assertFalse(registrar.registerHomeGeofence())
        verify(geofencingClient, never()).addGeofences(any(), any())
    }

    @Test
    fun `registerHomeGeofence returns false when no home location saved`() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SUCCESS)
        whenever(homeLocationDataSource.getHomeLocation()).thenReturn(null)
        grantLocationPermissions()

        assertFalse(registrar.registerHomeGeofence())
        verify(geofencingClient, never()).addGeofences(any(), any())
    }

    @Test
    fun `registerHomeGeofence returns false when fine location permission not granted`() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SUCCESS)
        whenever(homeLocationDataSource.getHomeLocation()).thenReturn(HomeLocation(48.0, 11.0))

        assertFalse(registrar.registerHomeGeofence())
        verify(geofencingClient, never()).addGeofences(any(), any())
    }

    @Test
    fun `registerHomeGeofence calls addGeofences when all conditions met`() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SUCCESS)
        whenever(homeLocationDataSource.getHomeLocation()).thenReturn(HomeLocation(48.137, 11.575, 150f))
        grantLocationPermissions()

        val result = registrar.registerHomeGeofence()

        assertTrue(result)
        verify(geofencingClient).addGeofences(any(), any())
    }

    @Test
    fun `registerHomeGeofence returns false on SecurityException`() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SUCCESS)
        whenever(homeLocationDataSource.getHomeLocation()).thenReturn(HomeLocation(48.0, 11.0))
        grantLocationPermissions()
        whenever(geofencingClient.addGeofences(any(), any())).thenThrow(SecurityException("denied"))

        assertFalse(registrar.registerHomeGeofence())
    }

    @Test
    fun `unregisterHomeGeofence calls removeGeofences with the correct ID`() {
        registrar.unregisterHomeGeofence()
        verify(geofencingClient).removeGeofences(listOf(GeofenceRegistrar.GEOFENCE_ID))
    }

    private fun grantLocationPermissions() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        shadowOf(app).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }
}

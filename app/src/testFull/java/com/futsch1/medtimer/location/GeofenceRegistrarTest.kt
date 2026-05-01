package com.futsch1.medtimer.location

import android.Manifest
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.model.HomeLocation
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var preferencesDataSource: PreferencesDataSource
    private lateinit var googleApiAvailability: GoogleApiAvailability
    private lateinit var registrar: GeofenceRegistrar

    @Before
    fun setUp() {
        geofencingClient = mock()
        preferencesDataSource = mock()
        googleApiAvailability = mock()

        val mockTask = mock<Task<Void>>()
        whenever(geofencingClient.addGeofences(any(), any())).thenReturn(mockTask)
        whenever(geofencingClient.removeGeofences(any<List<String>>())).thenReturn(mockTask)
        whenever(mockTask.addOnSuccessListener(any())).thenReturn(mockTask)
        whenever(mockTask.addOnFailureListener(any())).thenReturn(mockTask)
        whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default()))

        registrar = GmsGeofenceRegistrar(
            ApplicationProvider.getApplicationContext(),
            geofencingClient,
            preferencesDataSource,
            googleApiAvailability
        )
    }

    @Test
    fun locationServiceAvailableWhenPlayServicesPresent() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SUCCESS)
        assertTrue(registrar.isLocationServiceAvailable())
    }

    @Test
    fun locationServiceUnavailableWhenPlayServicesMissing() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SERVICE_MISSING)
        assertFalse(registrar.isLocationServiceAvailable())
    }

    @Test
    fun registerReturnsFalseWhenPlayServicesMissing() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SERVICE_MISSING)

        assertFalse(registrar.registerHomeGeofence())
        verify(geofencingClient, never()).addGeofences(any(), any())
    }

    @Test
    fun registerReturnsFalseWhenNoHomeLocation() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SUCCESS)
        whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default().copy(homeLocation = null)))
        grantLocationPermissions()

        assertFalse(registrar.registerHomeGeofence())
        verify(geofencingClient, never()).addGeofences(any(), any())
    }

    @Test
    fun registerReturnsFalseWhenPermissionDenied() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SUCCESS)
        whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default().copy(homeLocation = HomeLocation(48.0, 11.0))))

        assertFalse(registrar.registerHomeGeofence())
        verify(geofencingClient, never()).addGeofences(any(), any())
    }

    @Test
    fun registerCallsAddGeofencesWhenConditionsMet() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SUCCESS)
        whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default().copy(homeLocation = HomeLocation(48.137, 11.575, 150f))))
        grantLocationPermissions()

        val result = registrar.registerHomeGeofence()

        assertTrue(result)
        verify(geofencingClient).addGeofences(any(), any())
    }

    @Test
    fun registerReturnsFalseOnSecurityException() {
        whenever(googleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(ConnectionResult.SUCCESS)
        whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default().copy(homeLocation = HomeLocation(48.0, 11.0))))
        grantLocationPermissions()
        whenever(geofencingClient.addGeofences(any(), any())).thenThrow(SecurityException("denied"))

        assertFalse(registrar.registerHomeGeofence())
    }

    @Test
    fun unregisterCallsRemoveGeofences() {
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

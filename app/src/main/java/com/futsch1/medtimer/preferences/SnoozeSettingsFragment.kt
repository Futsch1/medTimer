package com.futsch1.medtimer.preferences

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.safeStartActivity
import com.futsch1.medtimer.location.GeofenceRegistrar
import com.futsch1.medtimer.model.HomeLocation
import com.futsch1.medtimer.preferences.PreferencesDataSource.Companion.LOCATION_SNOOZE_ENABLED
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SnoozeSettingsFragment : PreferencesFragment() {
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    @Inject
    lateinit var geofenceRegistrar: GeofenceRegistrar

    @Inject
    lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var requestFineLocationLauncher: ActivityResultLauncher<String>
    private lateinit var requestBackgroundLocationLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestFineLocationLauncher = registerForActivityResult(RequestPermission()) { granted ->
            when {
                granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !geofenceRegistrar.hasRequiredPermissions() -> showBackgroundLocationRationale()
                granted -> onLocationPermissionsGranted()
                else -> resetLocationSnooze()
            }
        }
        requestBackgroundLocationLauncher = registerForActivityResult(RequestPermission()) { granted ->
            if (granted) {
                onLocationPermissionsGranted()
            } else {
                resetLocationSnooze()
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = preferencesDataSource

        setPreferencesFromResource(R.xml.snooze_settings, rootKey)

        setupLocationSnooze()
        setupHomeLocation()
        setupShowOnMap()
    }

    private fun setupLocationSnooze() {
        val preference = preferenceScreen.findPreference<SwitchPreferenceCompat>(LOCATION_SNOOZE_ENABLED) ?: return
        updateLocationPrefsVisibility(preference.isChecked)
        preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                if (!geofenceRegistrar.isLocationServiceAvailable()) {
                    showInfoDialog(R.string.location_snooze_no_play_services)
                    return@OnPreferenceChangeListener false
                }
                requestFineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                geofenceRegistrar.unregisterHomeGeofence()
                updateLocationPrefsVisibility(false)
            }
            true
        }
    }

    private fun setupHomeLocation() {
        val pref = preferenceScreen.findPreference<Preference>("home_location") ?: return
        pref.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
            fetchCurrentLocationAndConfirm()
            true
        }
    }

    private fun setupShowOnMap() {
        val pref = preferenceScreen.findPreference<Preference>("show_home_location_on_map") ?: return
        pref.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
            val loc = preferencesDataSource.preferences.value.homeLocation ?: return@OnPreferenceClickListener true
            val uri = "geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}".toUri()
            safeStartActivity(context, Intent(Intent.ACTION_VIEW, uri))
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocationAndConfirm() {
        val cts = CancellationTokenSource()
        val progressDialog: AlertDialog = MaterialAlertDialogBuilder(requireActivity())
            .setMessage(R.string.determining_location)
            .setCancelable(false)
            .setNegativeButton(R.string.cancel) { _, _ -> cts.cancel() }
            .show()

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (isAdded) {
                    progressDialog.dismiss()
                }
                if (location != null) {
                    showSetHomeLocationDialog(location)
                } else {
                    showInfoDialog(R.string.home_location_error)
                }
            }
            .addOnFailureListener {
                if (isAdded) progressDialog.dismiss()
                showInfoDialog(R.string.home_location_error)
            }
    }

    private fun showSetHomeLocationDialog(location: Location) {
        val lat = String.format(Locale.US, "%.5f", location.latitude)
        val lon = String.format(Locale.US, "%.5f", location.longitude)
        MaterialAlertDialogBuilder(requireActivity())
            .setMessage(getString(R.string.set_home_location_confirm, lat, lon))
            .setPositiveButton(R.string.ok) { _, _ ->
                preferencesDataSource.saveHomeLocation(HomeLocation(location.latitude, location.longitude))
                updateLocationPrefsVisibility(true)
                if (preferencesDataSource.preferences.value.homeLocation != null) {
                    geofenceRegistrar.registerHomeGeofence()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateLocationPrefsVisibility(locationSnoozeEnabled: Boolean) {
        val homeLocPref = preferenceScreen.findPreference<Preference>("home_location") ?: return
        val mapPref = preferenceScreen.findPreference<Preference>("show_home_location_on_map") ?: return
        val hasHomeLocation = preferencesDataSource.preferences.value.homeLocation != null

        homeLocPref.isVisible = locationSnoozeEnabled
        homeLocPref.summary = if (hasHomeLocation) getString(R.string.home_location_set)
        else getString(R.string.home_location_not_set)

        mapPref.isVisible = locationSnoozeEnabled && hasHomeLocation
    }

    private fun showInfoDialog(@StringRes messageRes: Int) {
        if (!isAdded) return
        MaterialAlertDialogBuilder(requireActivity())
            .setMessage(messageRes)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showBackgroundLocationRationale() {
        MaterialAlertDialogBuilder(requireActivity())
            .setMessage(R.string.location_snooze_background_permission)
            .setPositiveButton(R.string.ok) { _, _ ->
                requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                resetLocationSnooze()
            }
            .show()
    }

    private fun onLocationPermissionsGranted() {
        updateLocationPrefsVisibility(true)
        if (preferencesDataSource.preferences.value.homeLocation != null && !geofenceRegistrar.registerHomeGeofence()) {
            resetLocationSnooze()
        }
    }

    override fun onResume() {
        super.onResume()
        val pref = findPreference<SwitchPreferenceCompat>(LOCATION_SNOOZE_ENABLED) ?: return
        if (pref.isChecked && !geofenceRegistrar.hasRequiredPermissions()) {
            resetLocationSnooze()
            geofenceRegistrar.unregisterHomeGeofence()
        }
        updateLocationPrefsVisibility(pref.isChecked)
    }

    private fun resetLocationSnooze() {
        preferenceManager.preferenceDataStore?.putBoolean(LOCATION_SNOOZE_ENABLED, false)
        findPreference<SwitchPreferenceCompat>(LOCATION_SNOOZE_ENABLED)?.isChecked = false
        updateLocationPrefsVisibility(false)
    }
}

package com.futsch1.medtimer.feature.ui.medicine.medicineSettings

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.futsch1.medtimer.core.common.helpers.safeStartActivity
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.feature.ui.R
import com.futsch1.medtimer.feature.ui.medicine.dialogs.ColorPickerDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MedicineSettingsFragment : MedicinePreferences(
    R.xml.medicine_settings,
    mapOf(
    ),
    mapOf(
        "color" to { activity, preference ->
            ColorPickerDialog(
                activity,
                activity,
                preference.preferenceDataStore?.getInt("color", 0) ?: 0
            ) { newColor ->
                preference.preferenceDataStore?.putInt("color", newColor)
                Toast.makeText(
                    activity,
                    com.futsch1.medtimer.core.ui.R.string.change_color_toast,
                    Toast.LENGTH_LONG
                ).show()
            }

        }
    ),
    listOf()
) {
    @Inject
    lateinit var notificationManager: NotificationManager

    override fun customSetup(modelData: Medicine) {
        findPreference<ListPreference>("notification_importance")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue == "2") {
                    showEnablePermissionsDialog()
                }
                true
            }
    }

    override fun onModelDataUpdated(modelData: Medicine) {
        super.onModelDataUpdated(modelData)

        // Style the icon of select color to match the color of the medicine
        findPreference<Preference>("color")?.let {
            if (modelData.useColor) {
                it.icon?.let { icon ->
                    DrawableCompat.setTint(icon, modelData.color)
                }
            } else {
                it.icon?.setTintList(null)
            }
        }

        findPreference<Preference>("notification_importance")?.summary =
            when (modelData.notificationImportance) {
                Medicine.NotificationImportance.DEFAULT -> getString(com.futsch1.medtimer.core.ui.R.string.default_)
                Medicine.NotificationImportance.HIGH -> if (modelData.showNotificationAsAlarm) getString(
                    com.futsch1.medtimer.core.ui.R.string.high_and_alarm
                ) else getString(com.futsch1.medtimer.core.ui.R.string.high)
            }
    }

    fun showEnablePermissionsDialog() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || notificationManager.canUseFullScreenIntent()) {
            return
        }

        val context = requireContext()
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage(com.futsch1.medtimer.core.ui.R.string.enable_notification_alarm_dialog)
            .setPositiveButton(com.futsch1.medtimer.core.ui.R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = "package:${context.packageName}".toUri()
                }
                safeStartActivity(context, intent)
            }

            .setNegativeButton(com.futsch1.medtimer.core.ui.R.string.cancel) { _, _ ->
                // Intentionally empty
            }.create()

        dialog.show()
    }

}
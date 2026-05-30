package com.futsch1.medtimer.feature.ui.medicine.medicineSettings

import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavDirections
import androidx.preference.Preference
import com.futsch1.medtimer.core.domain.model.Medicine

class MedicineSettingsFragment(
    preferencesResId: Int,
    links: Map<String, (Int) -> NavDirections>,
    customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>,
    simpleSummaryKeys: List<String>
) : MedicinePreferences(
    preferencesResId,
    links,
    customOnClick,
    simpleSummaryKeys
) {
    override fun customSetup(modelData: Medicine) {
        TODO("Not yet implemented")
    }

}
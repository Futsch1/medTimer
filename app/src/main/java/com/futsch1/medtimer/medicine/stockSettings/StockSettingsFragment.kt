package com.futsch1.medtimer.medicine.stockSettings

import android.os.Bundle
import android.text.InputType
import androidx.lifecycle.ViewModelProvider
import androidx.preference.EditTextPreference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.EntityDataStore
import com.futsch1.medtimer.helpers.EntityPreferencesFragment
import com.futsch1.medtimer.helpers.EntityViewModel

class StockSettingsFragment(
) : EntityPreferencesFragment<Medicine>(
    R.xml.stock_settings,
    mapOf(
    ),
    mapOf(

    ),
    listOf("stock_reminder")
) {
    override fun getEntityDataStore(requireArguments: Bundle): EntityDataStore<Medicine> {
        return MedicineDataStore(requireArguments.getInt("medicineId"), requireContext(), getEntityViewModel().medicineRepository)
    }

    override fun getEntityViewModel(): EntityViewModel<Medicine> {
        return ViewModelProvider(this)[MedicineViewModel::class.java]
    }

    override fun customSetup(entity: Medicine) {
        findPreference<EditTextPreference>("cycle_consecutive_days")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        findPreference<EditTextPreference>("cycle_pause_days")?.setOnBindEditTextListener { editText -> editText.inputType = InputType.TYPE_NUMBER_FLAG_SIGNED }
    }
}
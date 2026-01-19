package com.futsch1.medtimer.medicine.stockSettings

import android.os.Bundle
import android.text.InputType
import android.text.method.DigitsKeyListener
import androidx.lifecycle.ViewModelProvider
import androidx.preference.EditTextPreference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.EntityDataStore
import com.futsch1.medtimer.helpers.EntityPreferencesFragment
import com.futsch1.medtimer.helpers.EntityViewModel
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.medicine.addDoubleValidator
import java.text.DecimalFormatSymbols

class StockSettingsFragment(
) : EntityPreferencesFragment<Medicine>(
    R.xml.stock_settings,
    mapOf(
    ),
    mapOf(

    ),
    listOf("stock_unit")
) {
    override fun getEntityDataStore(requireArguments: Bundle): EntityDataStore<Medicine> {
        return MedicineDataStore(requireArguments.getInt("medicineId"), requireContext(), getEntityViewModel().medicineRepository)
    }

    override fun getEntityViewModel(): EntityViewModel<Medicine> {
        return ViewModelProvider(this)[MedicineViewModel::class.java]
    }

    override fun customSetup(entity: Medicine) {
        findPreference<EditTextPreference>("amount")!!.setOnBindEditTextListener { editText ->
            val separator = DecimalFormatSymbols.getInstance().decimalSeparator
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789$separator"))
            editText.addDoubleValidator()
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
    }

    override fun onEntityUpdated(entity: Medicine) {
        super.onEntityUpdated(entity)

        findPreference<EditTextPreference>("amount")!!.summary = MedicineHelper.formatAmount(entity.amount, entity.unit)
    }
}
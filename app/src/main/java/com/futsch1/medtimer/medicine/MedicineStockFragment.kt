package com.futsch1.medtimer.medicine

import android.annotation.SuppressLint
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.DatabaseEntityEditFragment
import com.futsch1.medtimer.helpers.MedicineEntityInterface
import com.futsch1.medtimer.medicine.editMedicine.stockReminderStringToValue
import com.futsch1.medtimer.medicine.editMedicine.stockReminderValueToString
import com.google.android.material.textfield.TextInputEditText

class MedicineStockFragment :
    DatabaseEntityEditFragment<Medicine>(
        MedicineEntityInterface(),
        R.layout.fragment_medicine_stock,
        MedicineStockFragment::class.java.name
    ) {

    override fun getEntityId(): Int {
        return MedicineStockFragmentArgs.fromBundle(requireArguments()).medicineId
    }

    override fun fillEntityData(entity: Medicine, fragmentView: View) {
        try {
            entity.medicationAmount =
                fragmentView.findViewById<TextInputEditText>(R.id.amountLeft).text.toString()
                    .toInt()
            entity.medicationStockReminder = stockReminderStringToValue(
                fragmentView.findViewById<AutoCompleteTextView>(R.id.medicineStockReminder).text.toString(),
                this.resources
            )
            entity.medicationAmountReminderThreshold =
                fragmentView.findViewById<TextInputEditText>(R.id.reminderThreshold).text.toString()
                    .toInt()
        } catch (e: NumberFormatException) {
            // Empty for now
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onEntityLoaded(entity: Medicine, fragmentView: View) {
        fragmentView.findViewById<TextInputEditText>(R.id.amountLeft)
            .setText(entity.medicationAmount.toString())
        fragmentView.findViewById<TextInputEditText>(R.id.reminderThreshold)
            .setText(entity.medicationAmountReminderThreshold.toString())

        val stockReminder: AutoCompleteTextView =
            fragmentView.findViewById(R.id.medicineStockReminder)
        val importanceTexts = this.resources.getStringArray(R.array.stock_reminder)
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, importanceTexts)
        stockReminder.setAdapter<ArrayAdapter<String>>(arrayAdapter)
        stockReminder.setText(
            stockReminderValueToString(entity.medicationStockReminder, this.resources), false
        )
    }
}
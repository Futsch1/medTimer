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
            entity.amount =
                fragmentView.findViewById<TextInputEditText>(R.id.amountLeft).text.toString()
                    .toDouble()
            entity.outOfStockReminder = stockReminderStringToValue(
                fragmentView.findViewById<AutoCompleteTextView>(R.id.medicineStockReminder).text.toString(),
                this.resources
            )
            entity.outOfStockReminderThreshold =
                fragmentView.findViewById<TextInputEditText>(R.id.reminderThreshold).text.toString()
                    .toDouble()
            entity.refillSizes = arrayListOf(
                fragmentView.findViewById<TextInputEditText>(R.id.refillSize).text.toString()
                    .toDouble()
            )
        } catch (e: NumberFormatException) {
            // Empty for now
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onEntityLoaded(entity: Medicine, fragmentView: View): Boolean {
        fragmentView.findViewById<TextInputEditText>(R.id.amountLeft)
            .setText(entity.amount.toString())
        fragmentView.findViewById<TextInputEditText>(R.id.reminderThreshold)
            .setText(entity.outOfStockReminderThreshold.toString())
        if (entity.refillSizes.isNotEmpty()) {
            fragmentView.findViewById<TextInputEditText>(R.id.refillSize)
                .setText(entity.refillSizes[0].toString())
        }

        val stockReminder: AutoCompleteTextView =
            fragmentView.findViewById(R.id.medicineStockReminder)
        val importanceTexts = this.resources.getStringArray(R.array.stock_reminder)
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, importanceTexts)
        stockReminder.setAdapter<ArrayAdapter<String>>(arrayAdapter)
        stockReminder.setText(
            stockReminderValueToString(entity.outOfStockReminder, this.resources), false
        )

        fragmentView.findViewById<View>(R.id.refillNow).setOnClickListener {
            onRefillClick(fragmentView)
        }

        return true
    }

    @SuppressLint("SetTextI18n")
    private fun onRefillClick(fragmentView: View) {
        try {
            var amount =
                fragmentView.findViewById<TextInputEditText>(R.id.amountLeft).text.toString()
                    .toInt()
            amount += fragmentView.findViewById<TextInputEditText>(R.id.refillSize).text.toString()
                .toInt()
            fragmentView.findViewById<TextInputEditText>(R.id.amountLeft)
                .setText(amount.toString())
        } catch (e: NumberFormatException) {
            // Intentionally empty
        }
    }
}
package com.futsch1.medtimer.medicine

import android.annotation.SuppressLint
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.DatabaseEntityEditFragment
import com.futsch1.medtimer.helpers.MedicineEntityInterface
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.medicine.editMedicine.stockReminderStringToValue
import com.futsch1.medtimer.medicine.editMedicine.stockReminderValueToString
import com.google.android.material.textfield.TextInputEditText
import java.text.ParseException

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
                MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.amountLeft).text.toString())

            entity.outOfStockReminder = stockReminderStringToValue(
                fragmentView.findViewById<AutoCompleteTextView>(R.id.medicineStockReminder).text.toString(),
                this.resources
            )
            entity.outOfStockReminderThreshold =
                MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.reminderThreshold).text.toString())
            entity.refillSizes = arrayListOf(
                MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.refillSize).text.toString())
            )
        } catch (e: ParseException) {
            // Empty for now
        }
    }

    override fun onEntityLoaded(entity: Medicine, fragmentView: View): Boolean {
        fragmentView.findViewById<TextInputEditText>(R.id.amountLeft)
            .setText(MedicineHelper.formatAmount(entity.amount))
        fragmentView.findViewById<TextInputEditText>(R.id.reminderThreshold)
            .setText(MedicineHelper.formatAmount(entity.outOfStockReminderThreshold))
        if (entity.refillSizes.isNotEmpty()) {
            fragmentView.findViewById<TextInputEditText>(R.id.refillSize)
                .setText(MedicineHelper.formatAmount(entity.refillSizes[0]))
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
                MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.amountLeft).text.toString())

            amount += MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.refillSize).text.toString())
            fragmentView.findViewById<TextInputEditText>(R.id.amountLeft)
                .setText(MedicineHelper.formatAmount(amount))
        } catch (e: ParseException) {
            // Intentionally empty
        }
    }
}
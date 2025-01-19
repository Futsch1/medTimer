package com.futsch1.medtimer.medicine

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.DatabaseEntityEditFragment
import com.futsch1.medtimer.helpers.MedicineEntityInterface
import com.futsch1.medtimer.helpers.MedicineHelper
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
        MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.amountLeft).text.toString())
            ?.let { entity.amount = it }

        entity.outOfStockReminder = stockReminderStringToValue(
            fragmentView.findViewById<AutoCompleteTextView>(R.id.medicineStockReminder).text.toString(),
            this.resources
        )
        MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.reminderThreshold).text.toString())
            ?.let { entity.outOfStockReminderThreshold = it }

        entity.refillSizes = arrayListOf(
            MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.refillSize).text.toString())
        )
    }

    override fun onEntityLoaded(entity: Medicine, fragmentView: View): Boolean {
        fragmentView.findViewById<TextInputEditText>(R.id.amountLeft)
            .setText(MedicineHelper.formatAmount(entity.amount))
        fragmentView.findViewById<TextInputEditText>(R.id.amountLeft).addDoubleValidator()

        fragmentView.findViewById<TextInputEditText>(R.id.reminderThreshold)
            .setText(MedicineHelper.formatAmount(entity.outOfStockReminderThreshold))
        fragmentView.findViewById<TextInputEditText>(R.id.reminderThreshold).addDoubleValidator()

        if (entity.refillSizes.isNotEmpty()) {
            fragmentView.findViewById<TextInputEditText>(R.id.refillSize)
                .setText(MedicineHelper.formatAmount(entity.refillSizes[0]))
        }
        fragmentView.findViewById<TextInputEditText>(R.id.refillSize).addDoubleValidator()

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
        var amount: Double? =
            MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.amountLeft).text.toString())
        val refillSize: Double? =
            MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.refillSize).text.toString())

        if (amount != null && refillSize != null) {
            amount += refillSize
            fragmentView.findViewById<TextInputEditText>(R.id.amountLeft)
                .setText(MedicineHelper.formatAmount(amount))
        }
    }
}

fun EditText.addDoubleValidator() {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Only afterTextChanged required
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // Only afterTextChanged required
        }

        override fun afterTextChanged(s: Editable?) {
            if (s == null || s.toString().isEmpty()) return

            val parsed: Double? = MedicineHelper.parseAmount(s.toString())
            if (parsed == null || parsed.isNaN() || parsed < 0.0) {
                // If the input is not a valid double, remove the last character
                s.delete(s.length - 1, s.length)
            }
        }
    })
}
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
        entity.amount =
            MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.amountLeft).text.toString())
                ?: entity.amount

        entity.unit =
            fragmentView.findViewById<TextInputEditText>(R.id.stockUnit).text.toString()

        entity.outOfStockReminder = stockReminderStringToValue(
            fragmentView.findViewById<AutoCompleteTextView>(R.id.medicineStockReminder).text.toString(),
            this.resources
        )
        entity.outOfStockReminderThreshold =
            MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.reminderThreshold).text.toString())
                ?: entity.outOfStockReminderThreshold

        val refillSize =
            MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.refillSize).text.toString())
        refillSize?.let { entity.refillSizes = arrayListOf(it) }
    }

    override fun onEntityLoaded(entity: Medicine, fragmentView: View): Boolean {
        amountToView(fragmentView, R.id.amountLeft, entity.amount)

        fragmentView.findViewById<TextInputEditText>(R.id.stockUnit).setText(entity.unit)

        amountToView(fragmentView, R.id.reminderThreshold, entity.outOfStockReminderThreshold)
        amountToView(fragmentView, R.id.refillSize, if (entity.refillSizes.isNotEmpty()) entity.refillSizes[0] else 0.0)

        val stockReminder: AutoCompleteTextView = fragmentView.findViewById(R.id.medicineStockReminder)
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

    private fun amountToView(fragmentView: View, i: Int, d: Double) {
        fragmentView.findViewById<TextInputEditText>(i).setText(MedicineHelper.formatAmount(d, ""))
        fragmentView.findViewById<TextInputEditText>(i).addDoubleValidator()
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
                .setText(MedicineHelper.formatAmount(amount, ""))
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
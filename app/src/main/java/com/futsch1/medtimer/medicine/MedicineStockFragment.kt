package com.futsch1.medtimer.medicine

import android.annotation.SuppressLint
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.DatabaseEntityEditFragment
import com.futsch1.medtimer.helpers.MedicineEntityInterface
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.createCalendarEventIntent
import com.futsch1.medtimer.medicine.editMedicine.stockReminderStringToValue
import com.futsch1.medtimer.medicine.editMedicine.stockReminderValueToString
import com.google.android.material.textfield.TextInputEditText
import java.text.DecimalFormatSymbols


class MedicineStockFragment :
    DatabaseEntityEditFragment<Medicine>(
        MedicineEntityInterface(),
        R.layout.fragment_medicine_stock,
        MedicineStockFragment::class.java.name
    ) {

    private lateinit var amountLeft: TextInputEditText
    private var medicineId: Int = -1
    private lateinit var runOutDateField: TextInputEditText

    override fun getEntityId(): Int {
        return MedicineStockFragmentArgs.fromBundle(requireArguments()).medicineId
    }

    override fun fillEntityData(entity: Medicine, fragmentView: View) {
        entity.amount = getCurrentAmount() ?: entity.amount

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
        amountLeft = fragmentView.findViewById(R.id.amountLeft)
        amountToView(fragmentView, R.id.amountLeft, entity.amount)

        fragmentView.findViewById<TextInputEditText>(R.id.stockUnit).setText(entity.unit)

        amountToView(fragmentView, R.id.reminderThreshold, entity.outOfStockReminderThreshold)
        amountToView(fragmentView, R.id.refillSize, if (entity.refillSizes.isNotEmpty()) entity.refillSizes[0] else 0.0)

        val stockReminder: AutoCompleteTextView = fragmentView.findViewById(R.id.medicineStockReminder)
        val importanceTexts = this.resources.getStringArray(R.array.stock_reminder)
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, importanceTexts)
        stockReminder.setAdapter(arrayAdapter)
        stockReminder.setText(
            stockReminderValueToString(entity.outOfStockReminder, this.resources), false
        )

        fragmentView.findViewById<View>(R.id.refillNow).setOnClickListener {
            onRefillClick(fragmentView)
        }

        medicineId = entity.medicineId
        runOutDateField = fragmentView.findViewById(R.id.runOut)

        calculateRunOutDate()

        amountLeft.doAfterTextChanged { calculateRunOutDate() }

        setupToCalendarButton(fragmentView, entity)

        return true
    }

    private fun setupToCalendarButton(fragmentView: View, medicine: Medicine) {
        fragmentView.findViewById<View>(R.id.runOutToCalendar).setOnClickListener {
            val date = TimeHelper.dateStringToDate(context, runOutDateField.text.toString())
            if (date != null) {
                val intent = createCalendarEventIntent(context?.getString(R.string.out_of_stock_notification_title) + " - " + medicine.name, date)
                startActivity(intent)
            }

        }
    }

    private fun calculateRunOutDate() {
        if (::runOutDateField.isInitialized) {
            Handler(thread.looper).post {
                idlingResource.setBusy()
                val runOutDate = estimateStockRunOutDate(medicineViewModel, medicineId, getCurrentAmount())
                val runOutString = if (runOutDate != null) TimeHelper.localDateToDateString(context, runOutDate) else "---"

                this.activity?.runOnUiThread {
                    runOutDateField.setText(runOutString)
                    idlingResource.setIdle()
                }
            }
        }
    }

    private fun amountToView(fragmentView: View, i: Int, d: Double) {
        fragmentView.findViewById<TextInputEditText>(i).setText(MedicineHelper.formatAmount(d, ""))
        val separator = DecimalFormatSymbols.getInstance().decimalSeparator
        fragmentView.findViewById<TextInputEditText>(i).setKeyListener(DigitsKeyListener.getInstance("0123456789$separator"))
        fragmentView.findViewById<TextInputEditText>(i).addDoubleValidator()
    }

    @SuppressLint("SetTextI18n")
    private fun onRefillClick(fragmentView: View) {
        var amount: Double? = getCurrentAmount()
        val refillSize: Double? =
            MedicineHelper.parseAmount(fragmentView.findViewById<TextInputEditText>(R.id.refillSize).text.toString())

        if (amount != null && refillSize != null) {
            amount += refillSize
            fragmentView.findViewById<TextInputEditText>(R.id.amountLeft)
                .setText(MedicineHelper.formatAmount(amount, ""))
        }
    }

    private fun getCurrentAmount(): Double? {
        return MedicineHelper.parseAmount(amountLeft.text.toString())
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
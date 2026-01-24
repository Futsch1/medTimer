package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.content.Context
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.medicine.editors.TimeEditor
import com.futsch1.medtimer.medicine.stockSettings.addDoubleValidator
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.DecimalFormatSymbols

class NewReminderStockExpirationDialog(
    val context: Context,
    val activity: FragmentActivity,
    val medicine: Medicine,
    val medicineViewModel: MedicineViewModel,
    val reminder: Reminder
) {
    private val dialog: Dialog = Dialog(context)

    init {
        dialog.setContentView(R.layout.dialog_new_reminder_stock_expiration)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupCreateReminder()
        setupVisibilities()
        dialog.findViewById<MaterialButton>(R.id.cancelCreateReminder).setOnClickListener {
            dialog.dismiss()
        }

        setupEditStockThreshold()
        setupStockReminderType()

        dialog.show()
    }

    private fun setupEditStockThreshold() {
        val textInputEditText = dialog.findViewById<TextInputEditText>(R.id.editStockThreshold)

        val separator = DecimalFormatSymbols.getInstance().decimalSeparator
        textInputEditText.setKeyListener(DigitsKeyListener.getInstance("0123456789$separator"))
        textInputEditText.addDoubleValidator()
        textInputEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    private fun setupStockReminderType() {
        dialog.findViewById<RadioGroup>(R.id.stockReminderType).setOnCheckedChangeListener { _, checkedId ->
            dialog.findViewById<TextInputLayout>(R.id.editReminderTimeLayout).visibility = if (checkedId == R.id.daily) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun setupVisibilities() {
        val outOfStockVisibility = if (reminder.reminderType == Reminder.ReminderType.OUT_OF_STOCK) {
            View.VISIBLE
        } else {
            View.GONE
        }
        val expirationDateVisibility = if (reminder.reminderType == Reminder.ReminderType.EXPIRATION_DATE) {
            View.VISIBLE
        } else {
            View.GONE
        }

        dialog.findViewById<TextInputLayout>(R.id.editStockThresholdLayout).visibility = outOfStockVisibility
        dialog.findViewById<RadioGroup>(R.id.stockReminderType).visibility = outOfStockVisibility

        dialog.findViewById<TextInputLayout>(R.id.editExpirationDaysBeforeLayout).visibility = expirationDateVisibility
        dialog.findViewById<TextInputLayout>(R.id.editReminderTimeLayout).visibility = expirationDateVisibility
    }

    private fun setupCreateReminder(
    ) {
        val timeEditor = TimeEditor(
            activity,
            dialog.findViewById(R.id.editReminderTime),
            reminder.timeInMinutes,
            { _ -> },
            null
        )

        setupOnClickCreateReminder(
            timeEditor
        )
    }

    private fun setupOnClickCreateReminder(
        timeEditor: TimeEditor
    ) {
        dialog.findViewById<MaterialButton>(R.id.createReminder).setOnClickListener {
            var canCreate = true
            reminder.timeInMinutes = timeEditor.getMinutes()

            if (reminder.reminderType == Reminder.ReminderType.OUT_OF_STOCK) {
                try {
                    reminder.stockThreshold = dialog.findViewById<TextInputEditText>(R.id.editStockThreshold).text.toString().toDouble()
                } catch (_: NumberFormatException) {
                    canCreate = false
                }
                reminder.stockReminderType = when (dialog.findViewById<RadioGroup>(R.id.stockReminderType).checkedRadioButtonId) {
                    R.id.once -> Reminder.StockReminderType.ONCE
                    R.id.always -> Reminder.StockReminderType.ALWAYS
                    R.id.daily -> Reminder.StockReminderType.DAILY
                    else -> Reminder.StockReminderType.ONCE
                }
            }
            if (reminder.reminderType == Reminder.ReminderType.EXPIRATION_DATE) {
                try {
                    reminder.periodStart = dialog.findViewById<TextInputEditText>(R.id.editExpirationDaysBefore).text.toString().toLong()
                } catch (_: NumberFormatException) {
                    canCreate = false
                }
            }

            if (!canCreate) {
                Toast.makeText(context, R.string.invalid_input, Toast.LENGTH_SHORT).show()
            } else {
                medicineViewModel.medicineRepository.insertReminder(reminder)
                dialog.dismiss()
            }
        }
    }
}
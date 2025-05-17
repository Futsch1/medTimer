package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.AmountTextWatcher
import com.futsch1.medtimer.medicine.editors.DateTimeEditor
import com.futsch1.medtimer.medicine.editors.IntervalEditor
import com.futsch1.medtimer.medicine.editors.TimeEditor
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import java.time.Instant
import java.time.LocalDate


class NewReminderDialog(
    val context: Context,
    val activity: FragmentActivity,
    val medicine: Medicine,
    val medicineViewModel: MedicineViewModel
) {
    private val dialog: Dialog = Dialog(context)

    init {
        dialog.setContentView(R.layout.dialog_new_reminder)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupCreateReminder()
        setupVisibilities()
        dialog.findViewById<MaterialButton>(R.id.cancelCreateReminder).setOnClickListener {
            dialog.dismiss()
        }

        startEditAmount()
        dialog.show()
    }

    private fun setReminderTypeHint(checkedId: Int) {
        val reminderTypeHint = dialog.findViewById<MaterialTextView>(R.id.reminderTypeHint)
        if (checkedId == R.id.timeBased) {
            reminderTypeHint.setText(R.string.time_reminder_type_hint)
        } else {
            reminderTypeHint.setText(R.string.interval_reminder_type_hint)
        }
    }

    private fun startEditAmount() {
        val textInputEditText = dialog.findViewById<TextInputEditText>(R.id.editAmount)
        textInputEditText.requestFocus()
        textInputEditText.postDelayed({
            val imm: InputMethodManager? =
                getSystemService(context, InputMethodManager::class.java)
            imm?.showSoftInput(textInputEditText, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
        if (medicine.isStockManagementActive) {
            textInputEditText.addTextChangedListener(
                AmountTextWatcher(
                    textInputEditText
                )
            )
        } else {
            dialog.findViewById<TextInputLayout>(R.id.editAmountLayout).isErrorEnabled = false
        }
    }

    private fun setupVisibilities() {
        dialog.findViewById<RadioGroup>(R.id.reminderType)
            .setOnCheckedChangeListener { _, checkedId ->
                setVisibilities(checkedId)
                setReminderTypeHint(checkedId)
            }
        setVisibilities(
            dialog.findViewById<RadioGroup>(R.id.reminderType).checkedRadioButtonId
        )
    }

    private fun setVisibilities(checkedId: Int) {
        val timeBasedVisibility =
            if (checkedId == R.id.timeBased) ViewGroup.VISIBLE else ViewGroup.GONE
        val intervalBasedVisibility =
            if (checkedId == R.id.intervalBased) ViewGroup.VISIBLE else ViewGroup.GONE
        dialog.findViewById<TextInputLayout>(R.id.editIntervalTimeLayout).visibility =
            intervalBasedVisibility
        dialog.findViewById<MaterialButtonToggleGroup>(R.id.intervalUnit).visibility =
            intervalBasedVisibility
        dialog.findViewById<TextInputLayout>(R.id.editIntervalStartDateTimeLayout).visibility =
            intervalBasedVisibility
        dialog.findViewById<RadioGroup>(R.id.intervalStartType).visibility =
            intervalBasedVisibility
        dialog.findViewById<TextInputLayout>(R.id.editReminderTimeLayout).visibility =
            timeBasedVisibility
    }

    private fun setupCreateReminder(
    ) {
        val reminder = Reminder(medicine.medicineId)

        val timeEditor = TimeEditor(
            activity,
            dialog.findViewById(R.id.editReminderTime),
            reminder.timeInMinutes,
            { _ -> },
            null
        )

        val intervalEditor = IntervalEditor(
            dialog.findViewById(R.id.editIntervalTime),
            dialog.findViewById(R.id.intervalUnit), 12 * 60
        )

        val intervalStartDateTimeEditor = DateTimeEditor(
            activity,
            dialog.findViewById(R.id.editIntervalStartDateTime),
            Instant.now().epochSecond
        )

        setupOnClickCreateReminder(
            reminder,
            timeEditor,
            intervalEditor,
            intervalStartDateTimeEditor
        )
    }

    private fun setupOnClickCreateReminder(
        reminder: Reminder,
        timeEditor: TimeEditor,
        intervalEditor: IntervalEditor,
        intervalStartDateTimeEditor: DateTimeEditor
    ) {
        dialog.findViewById<MaterialButton>(R.id.createReminder).setOnClickListener {
            setDefaults(reminder)
            reminder.amount =
                dialog.findViewById<TextInputEditText>(R.id.editAmount).text.toString().trim()

            val isTimeBased = dialog.findViewById<MaterialRadioButton>(R.id.timeBased).isChecked

            val minutes = if (isTimeBased) {
                timeEditor.getMinutes()
            } else {
                intervalEditor.getMinutes()
            }
            reminder.timeInMinutes = minutes
            if (!isTimeBased) {
                reminder.intervalStart = intervalStartDateTimeEditor.getDateTimeSecondsSinceEpoch()
                reminder.intervalStartsFromProcessed =
                    dialog.findViewById<MaterialRadioButton>(R.id.intervalStarsFromProcessed).isChecked
            }
            if (minutes >= 0 && (isTimeBased || reminder.intervalStart >= 0)) {
                medicineViewModel.medicineRepository.insertReminder(reminder)
                dialog.dismiss()
            } else {
                Toast.makeText(context, R.string.invalid_input, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setDefaults(reminder: Reminder) {
        reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000
        reminder.cycleStartDay = LocalDate.now().plusDays(1).toEpochDay()
        reminder.instructions = ""
    }
}
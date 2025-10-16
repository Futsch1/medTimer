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
import java.time.Instant


class NewReminderDialog(
    val context: Context,
    val activity: FragmentActivity,
    val medicine: Medicine,
    val medicineViewModel: MedicineViewModel,
    val reminder: Reminder
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
        val timeBasedVisibility =
            if (reminder.reminderType == Reminder.ReminderType.TIME_BASED) ViewGroup.VISIBLE else ViewGroup.GONE
        val intervalBasedVisibility =
            if (reminder.reminderType == Reminder.ReminderType.CONTINUOUS_INTERVAL || reminder.reminderType == Reminder.ReminderType.WINDOWED_INTERVAL) ViewGroup.VISIBLE else ViewGroup.GONE
        val continuousIntervalVisibility =
            if (reminder.reminderType == Reminder.ReminderType.CONTINUOUS_INTERVAL) ViewGroup.VISIBLE else ViewGroup.GONE
        val windowedIntervalVisibility = if (reminder.reminderType == Reminder.ReminderType.WINDOWED_INTERVAL) ViewGroup.VISIBLE else ViewGroup.GONE

        dialog.findViewById<TextInputLayout>(R.id.editIntervalTimeLayout).visibility =
            intervalBasedVisibility
        dialog.findViewById<MaterialButtonToggleGroup>(R.id.intervalUnit).visibility =
            intervalBasedVisibility
        dialog.findViewById<TextInputLayout>(R.id.editIntervalStartDateTimeLayout).visibility =
            continuousIntervalVisibility
        dialog.findViewById<TextInputLayout>(R.id.editIntervalDailyStartTimeLayout).visibility = windowedIntervalVisibility
        dialog.findViewById<TextInputLayout>(R.id.editIntervalDailyEndTimeLayout).visibility = windowedIntervalVisibility
        dialog.findViewById<RadioGroup>(R.id.intervalStartType).visibility =
            intervalBasedVisibility
        dialog.findViewById<TextInputLayout>(R.id.editReminderTimeLayout).visibility =
            timeBasedVisibility
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

        val intervalEditor = IntervalEditor(
            dialog.findViewById(R.id.editIntervalTime),
            dialog.findViewById(R.id.intervalUnit), 12 * 60
        )

        val intervalStartDateTimeEditor = DateTimeEditor(
            activity,
            dialog.findViewById(R.id.editIntervalStartDateTime),
            Instant.now().epochSecond
        )

        val dailyStartTimeEditor = TimeEditor(
            activity,
            dialog.findViewById(R.id.editIntervalDailyStartTime),
            reminder.intervalStartTimeOfDay,
            { _ -> },
            null
        )
        val dailyEndTimeEditor = TimeEditor(
            activity,
            dialog.findViewById(R.id.editIntervalDailyEndTime),
            reminder.intervalEndTimeOfDay,
            { _ -> },
            null
        )


        setupOnClickCreateReminder(
            timeEditor,
            intervalEditor,
            intervalStartDateTimeEditor,
            dailyStartTimeEditor,
            dailyEndTimeEditor
        )
    }

    private fun setupOnClickCreateReminder(
        timeEditor: TimeEditor,
        intervalEditor: IntervalEditor,
        intervalStartDateTimeEditor: DateTimeEditor,
        dailyStartTimeEditor: TimeEditor,
        dailyEndTimeEditor: TimeEditor
    ) {
        dialog.findViewById<MaterialButton>(R.id.createReminder).setOnClickListener {
            reminder.amount =
                dialog.findViewById<TextInputEditText>(R.id.editAmount).text.toString().trim()

            val minutes = if (reminder.reminderType == Reminder.ReminderType.TIME_BASED) {
                timeEditor.getMinutes()
            } else {
                intervalEditor.getMinutes()
            }
            reminder.timeInMinutes = minutes
            if (reminder.reminderType == Reminder.ReminderType.CONTINUOUS_INTERVAL) {
                reminder.intervalStart = intervalStartDateTimeEditor.getDateTimeSecondsSinceEpoch()
                reminder.intervalStartsFromProcessed =
                    dialog.findViewById<MaterialRadioButton>(R.id.intervalStarsFromProcessed).isChecked
            }
            if (reminder.reminderType == Reminder.ReminderType.WINDOWED_INTERVAL) {
                reminder.intervalStartTimeOfDay = dailyStartTimeEditor.getMinutes()
                reminder.intervalEndTimeOfDay = dailyEndTimeEditor.getMinutes()
            }
            if (minutes >= 0 && (reminder.reminderType == Reminder.ReminderType.TIME_BASED || reminder.intervalStart >= 0)) {
                medicineViewModel.medicineRepository.insertReminder(reminder)
                dialog.dismiss()
            } else {
                Toast.makeText(context, R.string.invalid_input, Toast.LENGTH_SHORT).show()
            }
        }
    }

}
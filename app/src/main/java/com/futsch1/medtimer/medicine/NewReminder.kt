package com.futsch1.medtimer.medicine

import android.app.Dialog
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.medicine.editors.DateTimeEditor
import com.futsch1.medtimer.medicine.editors.IntervalEditor
import com.futsch1.medtimer.medicine.editors.TimeEditor
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.time.Instant
import java.time.LocalDate

class NewReminder(
    val activity: FragmentActivity,
    val medicineId: Int,
    val medicineViewModel: MedicineViewModel
) {
    private val dialog: Dialog = Dialog(activity)

    init {
        dialog.setContentView(R.layout.new_reminder)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        setupCreateReminder()
        setupVisibilities()
        dialog.findViewById<MaterialButton>(R.id.cancelCreateReminder).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupVisibilities() {
        dialog.findViewById<RadioGroup>(R.id.reminderType)
            .setOnCheckedChangeListener { _, checkedId ->
                setVisibilities(checkedId)
            }
        setVisibilities(
            dialog.findViewById<RadioGroup>(R.id.reminderType).checkedRadioButtonId
        )
    }

    private fun setVisibilities(checkedId: Int) {
        if (checkedId == R.id.timeBased) {
            dialog.findViewById<TextInputLayout>(R.id.editIntervalTimeLayout).visibility =
                ViewGroup.GONE
            dialog.findViewById<MaterialButtonToggleGroup>(R.id.intervalUnit).visibility =
                ViewGroup.GONE
            dialog.findViewById<TextInputLayout>(R.id.editIntervalStartDateTimeLayout).visibility =
                ViewGroup.GONE
            dialog.findViewById<TextInputLayout>(R.id.editReminderTimeLayout).visibility =
                ViewGroup.VISIBLE
        } else {
            dialog.findViewById<TextInputLayout>(R.id.editIntervalTimeLayout).visibility =
                ViewGroup.VISIBLE
            dialog.findViewById<MaterialButtonToggleGroup>(R.id.intervalUnit).visibility =
                ViewGroup.VISIBLE
            dialog.findViewById<TextInputLayout>(R.id.editIntervalStartDateTimeLayout).visibility =
                ViewGroup.VISIBLE
            dialog.findViewById<TextInputLayout>(R.id.editReminderTimeLayout).visibility =
                ViewGroup.GONE
        }
    }

    private fun setupCreateReminder(
    ) {
        val reminder = Reminder(medicineId)

        val timeEditor = TimeEditor(
            activity,
            dialog.findViewById(R.id.editReminderTime),
            reminder.timeInMinutes,
            { _ -> },
            null
        )

        val intervalEditor = IntervalEditor(
            dialog.findViewById(R.id.editIntervalTime),
            dialog.findViewById(R.id.intervalUnit), 1
        )

        val intervalStartDateTimeEditor = DateTimeEditor(
            activity,
            dialog.findViewById(R.id.editIntervalStartDateTime),
            Instant.now().epochSecond
        )

        dialog.findViewById<MaterialButton>(R.id.createReminder).setOnClickListener {
            reminder.amount =
                dialog.findViewById<TextInputEditText>(R.id.editAmount).text.toString()
            reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000
            reminder.cycleStartDay = LocalDate.now().plusDays(1).toEpochDay()
            reminder.instructions = ""

            val isTimeBased = dialog.findViewById<MaterialRadioButton>(R.id.timeBased).isChecked

            val minutes = if (isTimeBased) {
                timeEditor.getMinutes()
            } else {
                intervalEditor.getMinutes()
            }
            reminder.timeInMinutes = minutes
            if (!isTimeBased) {
                reminder.intervalStart = intervalStartDateTimeEditor.getDateTimeSecondsSinceEpoch()
            }
            if (minutes >= 0 && (isTimeBased || reminder.intervalStart >= 0)) {

                medicineViewModel.insertReminder(reminder)
            }
            dialog.dismiss()
        }
    }
}
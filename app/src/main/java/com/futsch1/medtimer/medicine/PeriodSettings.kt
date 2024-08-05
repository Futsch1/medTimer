package com.futsch1.medtimer.medicine

import android.text.format.DateUtils
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.TimeHelper
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.LocalDate

class PeriodSettings(
    private val advancedReminderSettingsView: View,
    private val fragmentManager: FragmentManager,
    val reminder: Reminder
) {
    private val activeState: RadioGroup =
        advancedReminderSettingsView.findViewById(R.id.activeState)
    private val editPeriodStartDate: EditText =
        advancedReminderSettingsView.findViewById(R.id.periodStartDate)
    private val periodStartActive: CheckBox =
        advancedReminderSettingsView.findViewById(R.id.periodStart)
    private val periodEndActive: CheckBox =
        advancedReminderSettingsView.findViewById(R.id.periodEnd)
    private val editPeriodEndDate: EditText =
        advancedReminderSettingsView.findViewById(R.id.periodEndDate)

    init {
        if (!reminder.active) {
            activeState.check(R.id.inactive)
        } else if (reminder.periodStart == 0L && reminder.periodEnd == 0L) {
            activeState.check(R.id.active)
        } else {
            activeState.check(R.id.timePeriod)
        }
        activeState.setOnCheckedChangeListener { _, checkedId ->
            setPeriodFieldsVisibility(checkedId)
        }
        setPeriodFieldsVisibility(activeState.checkedRadioButtonId)

        setTextFieldToDate(reminder.periodStart, editPeriodStartDate)
        setTextFieldToDate(reminder.periodEnd, editPeriodEndDate)
        periodStartActive.isChecked = reminder.periodStart != 0L
        periodEndActive.isChecked = reminder.periodEnd != 0L

        setupDatePicker(editPeriodStartDate)
        setupDatePicker(editPeriodEndDate)

        setupActiveSwitch(periodStartActive, editPeriodStartDate)
        setupActiveSwitch(periodEndActive, editPeriodEndDate)
    }

    private fun setPeriodFieldsVisibility(checkedId: Int) {
        if (checkedId == R.id.timePeriod) {
            editPeriodStartDate.visibility = View.VISIBLE
            editPeriodEndDate.visibility = View.VISIBLE
            periodStartActive.visibility = View.VISIBLE
            periodEndActive.visibility = View.VISIBLE
        } else {
            editPeriodStartDate.visibility = View.GONE
            editPeriodEndDate.visibility = View.GONE
            periodStartActive.visibility = View.GONE
            periodEndActive.visibility = View.GONE
        }
    }

    private fun setupActiveSwitch(activeCheckBox: CheckBox, textField: EditText) {
        textField.isEnabled = activeCheckBox.isChecked
        activeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            textField.isEnabled = isChecked
            if (isChecked && textField.text.toString().isEmpty()) {
                setTextFieldToDate(LocalDate.now().toEpochDay(), textField)
            }
        }
    }

    private fun setupDatePicker(textField: EditText) {
        textField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                var startDate: LocalDate? =
                    TimeHelper.dateStringToDate(textField.getText().toString())
                if (startDate == null) {
                    startDate = LocalDate.now()
                }
                val datePickerDialog = MaterialDatePicker.Builder.datePicker()
                    .setSelection(startDate!!.toEpochDay() * DateUtils.DAY_IN_MILLIS)
                    .build()
                datePickerDialog.addOnPositiveButtonClickListener { selectedDate: Long ->
                    textField.setText(
                        TimeHelper.daysSinceEpochToDateString(
                            textField.context,
                            selectedDate / DateUtils.DAY_IN_MILLIS
                        )
                    )
                }
                datePickerDialog.show(fragmentManager, "date_picker")
            }
        }
    }

    private fun setTextFieldToDate(daysSinceEpoch: Long, textField: EditText) {
        if (daysSinceEpoch != 0L) {
            textField.setText(
                TimeHelper.daysSinceEpochToDateString(
                    advancedReminderSettingsView.context,
                    daysSinceEpoch
                )
            )
        }
    }

    fun updateReminder() {
        reminder.active = activeState.checkedRadioButtonId != R.id.inactive
        reminder.periodStart =
            if (activeState.checkedRadioButtonId == R.id.timePeriod && periodStartActive.isChecked) getDaysSinceEpoch(
                editPeriodStartDate
            ) else 0L
        reminder.periodEnd =
            if (activeState.checkedRadioButtonId == R.id.timePeriod && periodEndActive.isChecked) getDaysSinceEpoch(
                editPeriodEndDate
            ) else 0L
        if (reminder.periodStart > reminder.periodEnd && reminder.periodEnd != 0L) {
            reminder.periodStart = 0
            reminder.periodEnd = 0
        }
    }

    private fun getDaysSinceEpoch(textField: EditText): Long {
        val dateString = TimeHelper.dateStringToDate(textField.text.toString())
        return dateString?.toEpochDay() ?: 0L
    }
}
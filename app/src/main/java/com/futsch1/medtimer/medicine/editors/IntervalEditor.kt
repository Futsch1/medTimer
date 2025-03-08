package com.futsch1.medtimer.medicine.editors

import android.annotation.SuppressLint
import com.futsch1.medtimer.helpers.Interval
import com.futsch1.medtimer.helpers.IntervalUnit
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText

@SuppressLint("SetTextI18n")
class IntervalEditor(
    private val timeEdit: TextInputEditText,
    private val intervalUnitToggle: MaterialButtonToggleGroup,
    initialValueMinutes: Int
) {
    init {
        val interval = Interval(initialValueMinutes)
        var unit = interval.getUnit()
        timeEdit.setText(interval.getValue().toString())
        intervalUnitToggle.check(intervalUnitToggle.getChildAt(unit.ordinal).id)
        intervalUnitToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            val selectedUnit = checkedIntervalUnit(checkedId)
            if (isChecked && timeEdit.text.toString().isNotBlank()) {
                interval.setValue(timeEdit.text.toString().toInt(), unit)
                var value = interval.getValue(selectedUnit)
                if (value == 0) {
                    value = 1
                    interval.setValue(timeEdit.text.toString().toInt(), selectedUnit)
                }
                timeEdit.setText(value.toString())
                unit = selectedUnit
            }
        }
    }

    fun getMinutes(): Int {
        var value = 1
        try {
            value = timeEdit.text.toString().toInt()
        } catch (e: NumberFormatException) {
            // Intentionally empty
        }
        val unit = checkedIntervalUnit(intervalUnitToggle.checkedButtonId)
        return Interval(if (value > 0) value else 1, unit).minutesValue
    }

    private fun checkedIntervalUnit(checkedId: Int) = when (checkedId) {
        intervalUnitToggle.getChildAt(0).id -> IntervalUnit.MINUTES
        intervalUnitToggle.getChildAt(1).id -> IntervalUnit.HOURS
        else -> IntervalUnit.DAYS
    }
}
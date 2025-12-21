package com.futsch1.medtimer.medicine.editors

import android.annotation.SuppressLint
import androidx.core.widget.doAfterTextChanged
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.Interval
import com.futsch1.medtimer.helpers.IntervalUnit
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText

@SuppressLint("SetTextI18n")
class IntervalEditor(
    private val timeEdit: TextInputEditText,
    private val intervalUnitToggle: MaterialButtonToggleGroup,
    initialValueMinutes: Int,
    private val maxMinutes: Int = Interval.MAX_INTERVAL_MINUTES
) {
    init {
        val interval = Interval(initialValueMinutes, maxMinutes)
        var unit = interval.getUnit()
        timeEdit.setText(interval.getValue().toString())
        intervalUnitToggle.check(intervalUnitToggle.getChildAt(unit.ordinal).id)

        intervalUnitToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val selectedUnit = checkedIntervalUnit(checkedId)
                val text = timeEdit.text.toString()
                if (text.isNotBlank()) {
                    text.toIntOrNull()?.let { currentTextValue ->
                        interval.setValue(currentTextValue, unit)
                        var newValue = interval.getValue(selectedUnit)
                        if (newValue == 0) {
                            newValue = 1
                            interval.setValue(1, selectedUnit)
                        }
                        timeEdit.setText(newValue.toString())
                    }
                }
                unit = selectedUnit
                validate()
            }
        }

        timeEdit.doAfterTextChanged {
            validate()
        }
        validate()
    }

    private fun validate() {
        val minutes = getMinutesRaw()
        if (minutes > maxMinutes) {
            timeEdit.error = timeEdit.context.getString(R.string.invalid_input)
        } else {
            timeEdit.error = null
        }
    }

    fun getMinutes(): Int {
        val minutes = getMinutesRaw()
        return if (minutes > maxMinutes) maxMinutes else minutes
    }

    private fun getMinutesRaw(): Int {
        val textValue = timeEdit.text.toString().toIntOrNull() ?: 1
        val value = if (textValue > 0) textValue else 1
        val unit = checkedIntervalUnit(intervalUnitToggle.checkedButtonId)
        return Interval(value, unit).minutesValue
    }

    private fun checkedIntervalUnit(checkedId: Int) = when (checkedId) {
        intervalUnitToggle.getChildAt(0).id -> IntervalUnit.MINUTES
        intervalUnitToggle.getChildAt(1).id -> IntervalUnit.HOURS
        else -> IntervalUnit.DAYS
    }
}

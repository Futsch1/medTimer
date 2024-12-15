package com.futsch1.medtimer.medicine.editors

import com.futsch1.medtimer.helpers.Interval
import com.futsch1.medtimer.helpers.IntervalUnit
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText

class IntervalEditor(
    private val timeEdit: TextInputEditText,
    private val intervalUnitToggle: MaterialButtonToggleGroup,
    initialValueMinutes: Int
) {
    init {
        val interval = Interval(initialValueMinutes)
        timeEdit.setText(interval.getValue())
        intervalUnitToggle.check(intervalUnitToggle.getChildAt(interval.getUnit().ordinal).id)
    }

    fun getMinutes(): Int {
        val value = timeEdit.text.toString().toInt()
        val unit = when (intervalUnitToggle.checkedButtonId) {
            intervalUnitToggle.getChildAt(0).id -> IntervalUnit.MINUTES
            intervalUnitToggle.getChildAt(1).id -> IntervalUnit.HOURS
            else -> IntervalUnit.DAYS
        }
        return Interval(value, unit).getValue()
    }
}
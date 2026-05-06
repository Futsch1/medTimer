package com.futsch1.medtimer.helpers

import android.content.Context
import com.futsch1.medtimer.R
import com.futsch1.medtimer.model.ReminderTime

enum class IntervalUnit {
    MINUTES, HOURS, DAYS, WEEKS
}

private fun getMinutes(unit: IntervalUnit, value: Int) = when (unit) {
    IntervalUnit.MINUTES -> value
    IntervalUnit.HOURS -> value * 60
    IntervalUnit.DAYS -> value * 60 * 24
    IntervalUnit.WEEKS -> value * 60 * 24 * 7
}

class Interval(var minutesValue: Int, var maxMinutesValue: Int = MAX_INTERVAL_MINUTES) {

    constructor(value: Int, unit: IntervalUnit) : this(
        getMinutes(unit, value)
    )

    constructor(value: ReminderTime, maxMinutesValue: Int = MAX_INTERVAL_MINUTES) : this(value.minutes, maxMinutesValue)

    fun getUnit(): IntervalUnit {
        return when {
            minutesValue % (60 * 24 * 7) == 0 -> IntervalUnit.WEEKS
            minutesValue % (60 * 24) == 0 -> IntervalUnit.DAYS
            minutesValue % 60 == 0 -> IntervalUnit.HOURS
            else -> IntervalUnit.MINUTES
        }
    }

    fun getValue(): Int {
        return when (getUnit()) {
            IntervalUnit.MINUTES -> minutesValue
            IntervalUnit.HOURS -> minutesValue / 60
            IntervalUnit.DAYS -> minutesValue / (60 * 24)
            IntervalUnit.WEEKS -> minutesValue / (60 * 24 * 7)
        }
    }

    fun getValue(unit: IntervalUnit): Int {
        return when (unit) {
            IntervalUnit.MINUTES -> minutesValue
            IntervalUnit.HOURS -> minutesValue / 60
            IntervalUnit.DAYS -> minutesValue / (60 * 24)
            IntervalUnit.WEEKS -> minutesValue / (60 * 24 * 7)
        }
    }

    fun setValue(value: Int, unit: IntervalUnit) {
        minutesValue = getMinutes(unit, value)
        if (minutesValue > maxMinutesValue) {
            minutesValue = maxMinutesValue
        }
    }

    override fun toString(): String {
        return "${getValue()} ${getUnit().toString().lowercase()}"
    }

    fun toTranslatedString(context: Context): String {
        val value = getValue()
        val textId = when (getUnit()) {
            IntervalUnit.MINUTES -> R.plurals.minutes
            IntervalUnit.HOURS -> R.plurals.hours
            IntervalUnit.DAYS -> R.plurals.days
            IntervalUnit.WEEKS -> R.plurals.weeks
        }
        return "$value ${context.resources.getQuantityString(textId, value)}"
    }

    companion object {
        const val MAX_INTERVAL_MINUTES = 365 * 60 * 24
    }
}

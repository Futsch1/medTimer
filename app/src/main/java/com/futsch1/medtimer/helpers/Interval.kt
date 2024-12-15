package com.futsch1.medtimer.helpers

import android.content.Context
import com.futsch1.medtimer.R

enum class IntervalUnit {
    MINUTES, HOURS, DAYS
}

class Interval(var minutesValue: Int) {

    constructor(value: Int, unit: IntervalUnit) : this(
        when (unit) {
            IntervalUnit.MINUTES -> value
            IntervalUnit.HOURS -> value * 60
            IntervalUnit.DAYS -> value * 60 * 24
        }
    )

    fun getUnit(): IntervalUnit {
        return when {
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
        }
    }

    fun getValue(unit: IntervalUnit): Int {
        return when (unit) {
            IntervalUnit.MINUTES -> minutesValue
            IntervalUnit.HOURS -> minutesValue / 60
            IntervalUnit.DAYS -> minutesValue / (60 * 24)
        }
    }

    fun setValue(value: Int, unit: IntervalUnit) {
        minutesValue = when (unit) {
            IntervalUnit.MINUTES -> value
            IntervalUnit.HOURS -> value * 60
            IntervalUnit.DAYS -> value * 60 * 24
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
        }
        return "$value ${context.resources.getQuantityString(textId, value)}"
    }
}
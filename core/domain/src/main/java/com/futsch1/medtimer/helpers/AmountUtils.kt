package com.futsch1.medtimer.helpers

import java.text.NumberFormat
import java.text.ParseException
import java.util.regex.Pattern

// Matches cyclic count suffix, e.g. " (1/3)" or " (12/24)"
private val CYCLIC_COUNT = Pattern.compile(" \\(\\d+/\\d+\\)")

// TODO: should not be needed; the event should be more structured to be able to group without magic normalizations
fun normalizeMedicineName(name: String): String =
    CYCLIC_COUNT.matcher(name).replaceAll("")

fun formatAmount(amount: Double, unit: String): String {
    val fmt = NumberFormat.getNumberInstance()
    fmt.minimumFractionDigits = 0
    fmt.maximumFractionDigits = 2
    return fmt.format(amount) + if (unit.isEmpty()) "" else " $unit"
}

// TODO: data should be structured in such a way not magic string extraction is needed
fun parseAmount(amount: String?): Double? {
    val numberRegex = Pattern.compile("(?:\\d|\\.\\d)[.,\\s\\d]*")
    val matcher = numberRegex.matcher(amount ?: "")

    return if (matcher.find() && matcher.group(0) != null) {
        val numberFormat = NumberFormat.getNumberInstance()
        try {
            numberFormat.parse(matcher.group(0)!!.replace(" ", ""))?.toDouble()
        } catch (_: ParseException) {
            null
        }
    } else {
        null
    }
}

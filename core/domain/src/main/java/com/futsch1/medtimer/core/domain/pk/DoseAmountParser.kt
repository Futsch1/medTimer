package com.futsch1.medtimer.core.domain.pk

/** A parsed dose: the amount and its unit as written ("" if the dose has no unit). */
data class DoseAmount(val amount: Double, val unit: String)

/**
 * Strict dose parser for the level estimate. Unlike the app's general amount parsing, it does not depend on the locale and only
 * accepts a single number with an optional one-word unit, so free-text amounts like "2 x 5mg" or "5mg twice" are never misread.
 *
 * Accepts "12.5mg", "12,5 mg", "2 tablets", "1000 IU", ".5" or "12.5": either "." or "," as decimal separator.
 */
object DoseAmountParser {
    private val PATTERN = Regex("""^\s*(\d*)(?:[.,](\d+))?\s*(\p{L}+)?\s*$""")

    /** Returns the amount and unit, or null if [text] is not a single positive amount with an optional unit. */
    fun parse(text: String): DoseAmount? {
        val match = PATTERN.matchEntire(text) ?: return null
        val (whole, fraction, unit) = match.destructured
        if (whole.isEmpty() && fraction.isEmpty()) {
            return null
        }
        // "1,000mg" or "1.500mg" may use a thousands separator rather than a decimal one; refuse to guess
        if (fraction.length == THOUSANDS_GROUP_DIGITS && whole.isNotEmpty() && whole.trimStart('0').isNotEmpty()) {
            return null
        }
        val value = "${whole.ifEmpty { "0" }}.${fraction.ifEmpty { "0" }}".toDouble()
        return if (value > 0.0) DoseAmount(value, unit) else null
    }

    private const val THOUSANDS_GROUP_DIGITS = 3
}

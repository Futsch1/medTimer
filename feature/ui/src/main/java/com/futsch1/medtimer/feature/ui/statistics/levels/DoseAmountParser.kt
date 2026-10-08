package com.futsch1.medtimer.feature.ui.statistics.levels

import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import java.text.NumberFormat

/** A parsed dose: the amount and its unit as written ("" if the dose has no unit). */
data class DoseAmount(val amount: Double, val unit: String)

/** What a recorded dose amount means for the level estimate, see [DoseAmountParser.read]. */
internal sealed interface DoseReading {
    data class Amount(val dose: DoseAmount) : DoseReading

    /** The amount is followed by further numbers, like "2 x 5mg"; reading only the first would be wrong. */
    data object SeveralNumbers : DoseReading

    /** The number cannot be read completely with the number format, like "10.5" where "," is the decimal separator. */
    data object IncompleteNumber : DoseReading

    /** There is no positive amount at the start, like "?" or "approx. 5mg". */
    data object Unreadable : DoseReading
}

/**
 * Reads a dose for the level estimate. The number is parsed by [MedicineHelper.parseAmount], like stock amounts, so both use the
 * same locale-aware rules, but it must be read completely. Everything after the number is the unit, which must not contain any
 * further digits. Amounts that would only be partially read are reported rather than truncated.
 */
internal object DoseAmountParser {
    // The number part matches what MedicineHelper.parseAmount extracts; the unit is the rest, without digits
    private val PATTERN = Regex("""^\s*((?:\d|\.\d)[.,\s\d]*?)\s*(\D*?)\s*$""")
    private val SEVERAL_NUMBERS = Regex("""^\s*(?:\d|\.\d)[.,\s\d]*[^.,\s\d].*\d""")

    /** @param format the number format to use instead of the device's, e.g. to test other locales */
    fun read(text: String, format: NumberFormat? = null): DoseReading {
        val match = PATTERN.matchEntire(text)
            ?: return if (SEVERAL_NUMBERS.containsMatchIn(text)) DoseReading.SeveralNumbers else DoseReading.Unreadable
        val (number, rawUnit) = match.destructured
        // A separator directly after the number, as in "5. mg", belongs to neither the number nor the unit
        val unit = rawUnit.trimStart('.', ',').trim()
        val complete = parseNumber(number, format, requireComplete = true)
        return when {
            complete != null -> if (complete > 0.0) DoseReading.Amount(DoseAmount(complete, unit)) else DoseReading.Unreadable
            parseNumber(number, format, requireComplete = false) != null -> DoseReading.IncompleteNumber
            else -> DoseReading.Unreadable
        }
    }

    /** Returns the amount and unit, or null if [text] cannot be read as a single positive amount with an optional unit. */
    fun parse(text: String, format: NumberFormat? = null): DoseAmount? = (read(text, format) as? DoseReading.Amount)?.dose

    private fun parseNumber(number: String, format: NumberFormat?, requireComplete: Boolean): Double? =
        if (format != null) MedicineHelper.parseAmount(number, format, requireComplete) else MedicineHelper.parseAmount(number, requireComplete)
}

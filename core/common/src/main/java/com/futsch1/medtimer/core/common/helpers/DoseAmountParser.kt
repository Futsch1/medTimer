package com.futsch1.medtimer.core.common.helpers

import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

/** A parsed primary amount and its unit as written ("" if none could be recovered). */
data class DoseAmount(val amount: Double, val unit: String)

/** [unit] uses the most recent spelling; an empty string means no unit. */
data class UnitCount(val unit: String, val count: Int)

data class DoseUnitSummary(val units: List<UnitCount>, val unreadable: Int) {
    val consistent: Boolean get() = units.size <= 1
    val unit: String? get() = if (units.size == 1) units[0].unit else if (units.isEmpty()) "" else null
}

/** Locale-aware primary-amount parsing and unit recovery, without unit conversion. */
object DoseAmountParser {
    private val AMOUNT_PATTERN = Regex("""(?:\d|\.\d)[.,\s\d]*""")

    // NumberFormat is mutable and not thread-safe; do not share it with amount formatting.
    private val numberFormat = ThreadLocal.withInitial { NumberFormat.getNumberInstance() }

    /** Uses exactly the same primary-number interpretation as [parse], including partial locale parsing. */
    fun parseAmount(text: String?, format: NumberFormat? = null): Double? = parse(text, format)?.amount

    /**
     * Reads the first number anywhere in [text], accepting zero and however much the locale can parse.
     * Later numbers do not affect the amount. The unit is the following text up to another number or an
     * opening parenthesis, so "1 capsule (25 mg)" is read as one capsule. No unit equivalences are guessed.
     */
    fun parse(text: String?, format: NumberFormat? = null): DoseAmount? {
        val input = text ?: return null
        val match = AMOUNT_PATTERN.find(input) ?: return null
        val number = match.value.replace(" ", "")
        val effectiveFormat = format ?: numberFormat.get() ?: return null
        val parsed = effectiveFormat.parse(number, ParsePosition(0)) ?: return null
        // Skip the entire numeric token even when the locale reads only part of it; its remainder is not a unit.
        val unit = input.substring(match.range.last + 1)
            .takeWhile { !it.isDigit() && it != '(' }
            .trimStart { it.isWhitespace() || it == '.' || it == ',' || it == '-' }
            .trimEnd()
        return DoseAmount(parsed.toDouble(), unit)
    }

    /** Units are compared ignoring case, without guessing equivalences. */
    fun key(unit: String): String = unit.lowercase(Locale.ROOT)

    /** Summarizes [amounts] in chronological order, most-used units first and ties in first-seen order. */
    fun summarize(amounts: List<String>): DoseUnitSummary {
        val parsed = amounts.map { parse(it) }
        val units = parsed.filterNotNull()
            .groupBy { key(it.unit) }
            .values
            .sortedByDescending { it.size }
            .map { uses -> UnitCount(uses.last().unit, uses.size) }
        return DoseUnitSummary(units, parsed.count { it == null })
    }
}

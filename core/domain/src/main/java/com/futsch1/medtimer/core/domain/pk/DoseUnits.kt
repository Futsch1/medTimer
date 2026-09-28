package com.futsch1.medtimer.core.domain.pk

import java.util.Locale

/** How often a unit was used by a medicine's recorded doses; [unit] is spelled as in the most recent dose using it, "" for none. */
data class UnitCount(val unit: String, val count: Int)

/**
 * The units used by a medicine's recorded doses, most used first. Since each medicine's level curve is normalized, the unit itself
 * does not matter, but all doses must share it: mixing units (e.g. "mg" and "tablets") cannot be estimated without guessing.
 *
 * @param unreadable doses whose amount could not be parsed at all
 */
data class DoseUnitSummary(val units: List<UnitCount>, val unreadable: Int) {
    val consistent: Boolean get() = units.size <= 1
}

object DoseUnits {
    /** Units are compared ignoring case, so "mg" and "MG" are the same unit. */
    fun key(unit: String): String = unit.lowercase(Locale.ROOT)

    /** Summarizes the units of [amounts], which must be in chronological order. */
    fun summarize(amounts: List<String>): DoseUnitSummary {
        val parsed = amounts.map { DoseAmountParser.parse(it) }
        val units = parsed.filterNotNull()
            .groupBy { key(it.unit) }
            .values
            .sortedByDescending { it.size }
            .map { uses -> UnitCount(uses.last().unit, uses.size) }
        return DoseUnitSummary(units, parsed.count { it == null })
    }
}

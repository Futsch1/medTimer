package com.futsch1.medtimer.core.common.helpers

import java.util.Locale

object DoseUnitInflector {
    fun inflect(unit: String, amount: Double): String {
        val normalized = unit.trim().lowercase(Locale.ROOT)
        val family = DoseUnitComparator.families.firstOrNull {
            it.singulars.size == 1 && it.plurals.size == 1 &&
                (normalized in it.singulars || normalized in it.plurals)
        } ?: return unit

        val inflected = if (amount == 1.0) {
            family.singulars.single()
        } else {
            family.plurals.single()
        }
        return matchCasing(unit.trim(), inflected)
    }

    private fun matchCasing(original: String, inflected: String): String = when {
        original == original.uppercase(Locale.ROOT) -> inflected.uppercase(Locale.ROOT)
        original.firstOrNull()?.isUpperCase() == true ->
            inflected.replaceFirstChar { it.titlecase(Locale.ROOT) }
        else -> inflected
    }
}

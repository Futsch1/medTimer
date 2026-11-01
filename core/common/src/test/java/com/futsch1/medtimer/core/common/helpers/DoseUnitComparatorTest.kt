package com.futsch1.medtimer.core.common.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DoseUnitComparatorTest {
    @Test
    fun `plural variants compare to the plural spelling`() {
        val cases = listOf(
            Triple("tablet", "tablets", "tablets"),
            Triple("Tabletten", "Tablette", "Tabletten"),
            Triple("capsule", "capsules", "capsules"),
            Triple("капсула", "капсул", "капсул"),
            Triple("قطرة", "قطرات", "قطرات"),
        )

        for ((first, second, expected) in cases) {
            assertEquals(expected, DoseUnitComparator.compare(first, second))
            assertEquals(expected, DoseUnitComparator.compare(second, first))
        }
    }

    @Test
    fun `units differing only in case compare in lowercase`() {
        assertEquals("mg", DoseUnitComparator.compare("MG", "mG"))
    }

    @Test
    fun `plural result retains the spelling and casing supplied by the user`() {
        assertEquals("Tabletten", DoseUnitComparator.compare("Tablette", "Tabletten"))
        assertEquals("TABLETS", DoseUnitComparator.compare("tablet", "TABLETS"))
    }

    @Test
    fun `different units do not compare as equal`() {
        assertNull(DoseUnitComparator.compare("mg", "mgs"))
        assertNull(DoseUnitComparator.compare("tablet", "capsule"))
    }
}

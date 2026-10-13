package com.futsch1.medtimer.feature.ui.statistics.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoseUnitsTest {

    @Test
    fun `consistent units ignore case and use the latest spelling`() {
        val summary = DoseUnits.summarize(listOf("5mg", "5 MG", "7.5 mg"))

        assertEquals(listOf(UnitCount("mg", 3)), summary.units)
        assertTrue(summary.consistent)
    }

    @Test
    fun `no unit at all is consistent too`() {
        val summary = DoseUnits.summarize(listOf("12.5", "5"))

        assertEquals(listOf(UnitCount("", 2)), summary.units)
        assertTrue(summary.consistent)
    }

    @Test
    fun `mixed units are listed with the most used first`() {
        val summary = DoseUnits.summarize(listOf("12.5", "5mg", "5mg", "?", "2 x 5mg", "2 tablets", "3 tablets", "4 tablets"))

        assertEquals(listOf(UnitCount("tablets", 3), UnitCount("mg", 2), UnitCount("", 1)), summary.units)
        assertEquals(2, summary.unreadable)
        assertFalse(summary.consistent)
    }

    @Test
    fun `unreadable doses alone do not make the units inconsistent`() {
        val summary = DoseUnits.summarize(listOf("5mg", "?", "some"))

        assertTrue(summary.consistent)
        assertEquals(2, summary.unreadable)
    }
}

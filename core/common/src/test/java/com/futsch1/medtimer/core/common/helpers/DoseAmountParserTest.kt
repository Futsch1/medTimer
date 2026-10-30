package com.futsch1.medtimer.core.common.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

class DoseAmountParserTest {
    private val us = NumberFormat.getNumberInstance(Locale.US)
    private val hungarian = NumberFormat.getNumberInstance(Locale.forLanguageTag("hu-HU"))

    @Test
    fun `amount and unit parsing share permissive first number behavior`() {
        val cases = listOf(
            "3.5" to DoseAmount(3.5, ""), "5" to DoseAmount(5.0, ""), ".5" to DoseAmount(0.5, ""),
            "50000" to DoseAmount(50000.0, ""), "50,000" to DoseAmount(50000.0, ""),
            "50 000" to DoseAmount(50000.0, ""), "1,000.5" to DoseAmount(1000.5, ""),
            "1 000 000.42" to DoseAmount(1000000.42, ""), "3.566 pills" to DoseAmount(3.566, "pills"),
            "Take 6.23 pills" to DoseAmount(6.23, "pills"),
            "Take 6.23 pills at 5 o'clock" to DoseAmount(6.23, "pills at"),
            "Many. 4 to be specific." to DoseAmount(4.0, "to be specific."),
            "0mg" to DoseAmount(0.0, "mg"), "-5mg" to DoseAmount(5.0, "mg"),
            "1 capsule (25 mg)" to DoseAmount(1.0, "capsule"), "2 x 5mg" to DoseAmount(2.0, "x"),
            "1-2 tablets" to DoseAmount(1.0, ""), "5mg twice, then 2" to DoseAmount(5.0, "mg twice, then"),
            "approx. 5mg" to DoseAmount(5.0, "mg")
        )

        for ((text, expected) in cases) {
            assertEquals(text, expected, DoseAmountParser.parse(text, us))
            assertEquals(text, expected.amount, DoseAmountParser.parseAmount(text, us))
        }
    }

    @Test
    fun `unreadable amounts return null for both entry points`() {
        for (text in listOf(null, "", "?", "..", "Take pills", "There is no number here. Only some ,. and spaces.")) {
            assertNull(text, DoseAmountParser.parse(text, us))
            assertNull(text, DoseAmountParser.parseAmount(text, us))
        }
    }

    @Test
    fun `locale parsing can stop within the numeric token without turning its remainder into a unit`() {
        val cases = listOf(
            Triple("10.5mg", hungarian, DoseAmount(10.0, "mg")),
            Triple("0.5", hungarian, DoseAmount(0.0, "")),
            Triple("10,5 mg", hungarian, DoseAmount(10.5, "mg")),
            Triple("1.2.3 mg", us, DoseAmount(1.2, "mg")),
            Triple("12.5mg", NumberFormat.getNumberInstance(Locale.GERMANY), DoseAmount(125.0, "mg")),
            Triple("5. mg", us, DoseAmount(5.0, "mg")),
            Triple("5, then", us, DoseAmount(5.0, "then"))
        )

        for ((text, format, expected) in cases) {
            assertEquals(text, expected, DoseAmountParser.parse(text, format))
            assertEquals(text, expected.amount, DoseAmountParser.parseAmount(text, format))
        }
    }

    @Test
    fun `unit spelling is retained and whitespace is trimmed`() {
        val cases = listOf(
            " 6 MG " to DoseAmount(6.0, "MG"), "5 mg/ml" to DoseAmount(5.0, "mg/ml"),
            "2 tablets after food " to DoseAmount(2.0, "tablets after food"), "500µg" to DoseAmount(500.0, "µg"),
            "1 capsule (after food)" to DoseAmount(1.0, "capsule")
        )

        for ((text, expected) in cases) assertEquals(text, expected, DoseAmountParser.parse(text, us))
    }

    @Test
    fun `consistent units ignore case and use the latest spelling`() {
        val summary = DoseAmountParser.summarize(listOf("5mg", "5 MG", " 7 mG "))

        assertEquals(listOf(UnitCount("mG", 3)), summary.units)
        assertTrue(summary.consistent)
    }

    @Test
    fun `no unit at all is consistent too`() {
        val summary = DoseAmountParser.summarize(listOf("12", "5"))

        assertEquals(listOf(UnitCount("", 2)), summary.units)
        assertTrue(summary.consistent)
    }

    @Test
    fun `mixed units are listed with the most used first including explanatory amounts`() {
        val summary = DoseAmountParser.summarize(listOf("12", "5mg", "5mg", "?", "2 tablets (25mg)", "3 tablets", "4 tablets"))

        assertEquals(listOf(UnitCount("tablets", 3), UnitCount("mg", 2), UnitCount("", 1)), summary.units)
        assertEquals(1, summary.unreadable)
        assertFalse(summary.consistent)
    }

    @Test
    fun `unreadable doses alone do not make the units inconsistent`() {
        val summary = DoseAmountParser.summarize(listOf("5mg", "?", "some"))

        assertTrue(summary.consistent)
        assertEquals(2, summary.unreadable)
    }

    @Test
    fun `equally frequent units keep their first seen order without conversion`() {
        val summary = DoseAmountParser.summarize(listOf("1 capsule", "1 capsules", "5mg", "5 MG"))

        assertEquals(listOf(UnitCount("MG", 2), UnitCount("capsule", 1), UnitCount("capsules", 1)), summary.units)
        assertFalse(summary.consistent)
    }

    @Test
    fun `empty history has no units or unreadable doses`() {
        assertEquals(DoseUnitSummary(emptyList(), 0), DoseAmountParser.summarize(emptyList()))
    }
}

package com.futsch1.medtimer.feature.ui.statistics.levels

import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

/** Numbers are parsed by [MedicineHelper.parseAmount]; explicit number formats keep these tests independent of the machine's locale. */
class DoseAmountParserTest {

    private val us = NumberFormat.getNumberInstance(Locale.US)
    private val hungarian = NumberFormat.getNumberInstance(Locale.forLanguageTag("hu-HU"))

    private fun read(text: String, format: NumberFormat = us) = DoseAmountParser.read(text, format)

    private fun amount(value: Double, unit: String) = DoseReading.Amount(DoseAmount(value, unit))

    @Test
    fun `numbers are parsed like stock amounts in the given locale`() {
        assertEquals(amount(12.5, "mg"), read("12.5mg"))
        assertEquals(amount(0.5, "mg"), read(".5mg"))
        assertEquals(amount(1000.0, "mg"), read("1,000mg"))
        assertEquals(amount(1000.0, "mg"), read("1 000 mg"))
        assertEquals(amount(12.5, "mg"), read("12,5 mg", hungarian))
    }

    @Test
    fun `everything after the number without digits is the unit`() {
        assertEquals(amount(12.0, ""), read("12"))
        assertEquals(amount(6.0, "MG"), read(" 6 MG "))
        assertEquals(amount(5.0, "mg/ml"), read("5 mg/ml"))
        assertEquals(amount(2.0, "tablets after food"), read("2 tablets after food "))
        assertEquals(amount(500.0, "µg"), read("500µg"))
    }

    @Test
    fun `numbers that the locale only reads partially are reported, not truncated`() {
        // In Hungarian, "," is the decimal separator, so the number format would stop at "." and read 10
        assertEquals(DoseReading.IncompleteNumber, read("10.5mg", hungarian))
        assertEquals(DoseReading.IncompleteNumber, read("0.5", hungarian))
        assertEquals(DoseReading.IncompleteNumber, read("1.2.3 mg"))
        // A separator after the number is not part of it
        assertEquals(amount(5.0, "mg"), read("5. mg"))
        assertEquals(amount(5.0, ""), read("5."))
    }

    @Test
    fun `further numbers after the amount are reported, not truncated`() {
        for (text in listOf("2 x 5mg", "1-2 tablets", "5mg twice, then 2", "1 tablet (5mg)")) {
            assertEquals(text, DoseReading.SeveralNumbers, read(text))
        }
    }

    @Test
    fun `amounts that do not start with a positive number are unreadable`() {
        for (text in listOf("?", "", "mg", "approx. 5mg", "-5mg", "0mg")) {
            assertEquals(text, DoseReading.Unreadable, read(text))
        }
        assertNull(DoseAmountParser.parse("?"))
    }
}

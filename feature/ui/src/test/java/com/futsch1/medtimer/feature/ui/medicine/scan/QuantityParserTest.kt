package com.futsch1.medtimer.feature.ui.medicine.scan

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class QuantityParserTest {

    @Test
    fun `recognizes unit-suffixed counts`() {
        assertEquals(30.0, QuantityParser.parse("tachipirina 30 compresse 500mg"))
        assertEquals(20.0, QuantityParser.parse("20 cpr rivestite con film"))
        assertEquals(16.0, QuantityParser.parse("16 capsule"))
        assertEquals(10.0, QuantityParser.parse("10 bustine"))
    }

    @Test
    fun `recognizes count-prefixed quantities`() {
        assertEquals(24.0, QuantityParser.parse("confezione da 24"))
        assertEquals(12.0, QuantityParser.parse("n. 12"))
    }

    @Test
    fun `returns null when nothing matches`() {
        assertNull(QuantityParser.parse("tachipirina 500 mg"))
        assertNull(QuantityParser.parse(""))
    }

    @Test
    fun `ignores a zero count`() {
        assertNull(QuantityParser.parse("0 compresse"))
    }
}

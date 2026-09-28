package com.futsch1.medtimer.core.domain.pk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DoseAmountParserTest {

    private fun parse(text: String) = DoseAmountParser.parse(text)

    @Test
    fun `accepts both decimal separators`() {
        assertEquals(DoseAmount(12.5, "mg"), parse("12.5mg"))
        assertEquals(DoseAmount(12.5, "mg"), parse("12,5mg"))
        assertEquals(DoseAmount(4.16, "mg"), parse("4.16mg"))
        assertEquals(DoseAmount(0.5, "mg"), parse(".5mg"))
        assertEquals(DoseAmount(0.125, "mg"), parse("0.125mg"))
    }

    @Test
    fun `unit is optional and kept as written`() {
        assertEquals(DoseAmount(12.5, ""), parse("12.5"))
        assertEquals(DoseAmount(6.0, "MG"), parse(" 6 MG "))
        assertEquals(DoseAmount(2.0, "tablets"), parse("2 tablets"))
        assertEquals(DoseAmount(1000.0, "IU"), parse("1000 IU"))
        assertEquals(DoseAmount(500.0, "µg"), parse("500µg"))
    }

    @Test
    fun `rejects anything but a single amount with one unit`() {
        assertNull(parse("?"))
        assertNull(parse(""))
        assertNull(parse("mg"))
        assertNull(parse("2 x 5mg"))
        assertNull(parse("5mg twice"))
        assertNull(parse("1-2 tablets"))
        assertNull(parse("5 mg/ml"))
        assertNull(parse("-5mg"))
        assertNull(parse("0mg"))
        assertNull(parse("1.2.3mg"))
    }

    @Test
    fun `refuses amounts that may use a thousands separator`() {
        assertNull(parse("1,000mg"))
        assertNull(parse("1.500"))
        assertEquals(DoseAmount(1000.0, "mg"), parse("1000mg"))
    }
}

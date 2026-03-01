package com.futsch1.medtimer.helpers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AmountUtilsTest {

    @Test
    fun testNormalizeMedicineName() {
        assertEquals("Aspirin", normalizeMedicineName("Aspirin (1/2)"))
        assertEquals("Aspirin", normalizeMedicineName("Aspirin"))
        assertEquals("Aspirin", normalizeMedicineName("Aspirin (11/12)"))
    }

    @Test
    fun testParseAmount() {
        assertEquals(3.5, parseAmount("3.5"))
        assertEquals(5.0, parseAmount("5"))
        assertEquals(0.5, parseAmount(".5"))
        assertEquals(50000.0, parseAmount("50000"))
        assertEquals(50000.0, parseAmount("50,000"))
        assertEquals(50000.0, parseAmount("50 000"))
        assertEquals(1000.5, parseAmount("1,000.5"))
        assertEquals(1000000.42, parseAmount("1 000 000.42"))
        assertEquals(3.566, parseAmount("3.566 pills"))
        assertEquals(6.23, parseAmount("Take 6.23 pills"))
        assertEquals(6.23, parseAmount("Take 6.23 pills at 5 o'clock"))
        assertNull(parseAmount(".."))
        assertNull(parseAmount("There is no number here. Only some ,. and spaces."))
        assertEquals(4.0, parseAmount("Many. 4 to be specific."))
        assertNull(parseAmount("Take pills"))
        var amount = 5.0
        parseAmount("No string")?.let { amount = it }
        assertEquals(5.0, amount)
    }

    @Test
    fun testFormatAmount() {
        assertEquals("3.5", formatAmount(3.5, ""))
        assertEquals("5 pills", formatAmount(5.0, "pills"))
    }

    @Test
    fun testFormatParseRoundTrip() {
        val amount = 123456.0
        val unit = "pills"
        val formatted = formatAmount(amount, unit)
        val parsed = parseAmount(formatted)
        assertEquals(amount, parsed)
    }
}

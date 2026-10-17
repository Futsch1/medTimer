package com.futsch1.medtimer.feature.ui.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnoozeDurationTest {

    private val entries = arrayOf<CharSequence>("5 minutes", "15 minutes", "Custom…", "Set duration…")
    private val entryValues = arrayOf<CharSequence>("5", "15", "-1", SET_SNOOZE_DURATION_VALUE)

    private fun summary(value: String?) = snoozeDurationSummary(value, entries, entryValues) { "$it min" }

    @Test
    fun `should show the list entry for a preset duration`() {
        assertEquals("15 minutes", summary("15"))
    }

    @Test
    fun `should show the entered minutes for a duration that is not in the list`() {
        assertEquals("45 min", summary("45"))
    }

    @Test
    fun `should show no summary when nothing usable is stored`() {
        assertNull(summary(null))
        assertNull(summary("abc"))
    }

    @Test
    fun `should accept positive whole minutes`() {
        assertEquals(45, parseSnoozeMinutes(" 45 "))
    }

    @Test
    fun `should reject empty, zero, negative and non-numeric input`() {
        assertNull(parseSnoozeMinutes(""))
        assertNull(parseSnoozeMinutes("0"))
        assertNull(parseSnoozeMinutes("-5"))
        assertNull(parseSnoozeMinutes("ten"))
        assertNull(parseSnoozeMinutes(null))
    }
}

package com.futsch1.medtimer.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ManualDosesTest {
    @Test
    fun `should keep at most the three newest doses`() {
        val customDoses = ManualDoses("")
        customDoses.addDose("Medicine A" to "1")
        customDoses.addDose("Medicine B" to "2")
        customDoses.addDose("Medicine C" to "3")
        customDoses.addDose("Medicine D" to "4")

        assertEquals(
            listOf("Medicine D" to "4", "Medicine C" to "3", "Medicine B" to "2"),
            customDoses.doses,
        )
    }

    @Test
    fun `should round trip doses through encoding`() {
        val original = ManualDoses("").apply { addDose("Medicine A" to "1 tablet") }

        assertEquals(original.doses, ManualDoses(original.encode()).doses)
    }

    @Test
    fun `should tolerate corrupt encoded data`() {
        assertEquals(emptyList<Pair<String, String>>(), ManualDoses("invalid").doses)
    }
}

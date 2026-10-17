package com.futsch1.medtimer.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ManualDosesTest {
    @Test
    fun `should keep at most the three newest doses`() {
        val customDoses = ManualDoses("")
        customDoses.addDose(ManualDose("Medicine A", 1, "1"))
        customDoses.addDose(ManualDose("Medicine B", 2, "2"))
        customDoses.addDose(ManualDose("Medicine C", 3, "3"))
        customDoses.addDose(ManualDose("Medicine D", 4, "4"))

        assertEquals(
            listOf(
                ManualDose("Medicine D", 4, "4"),
                ManualDose("Medicine C", 3, "3"),
                ManualDose("Medicine B", 2, "2"),
            ),
            customDoses.doses,
        )
    }

    @Test
    fun `should round trip doses through encoding`() {
        val original = ManualDoses("").apply { addDose(ManualDose("Medicine A", 7, "1 tablet")) }

        assertEquals(original.doses, ManualDoses(original.encode()).doses)
    }

    @Test
    fun `should tolerate corrupt encoded data`() {
        assertEquals(emptyList<ManualDose>(), ManualDoses("invalid").doses)
    }
}

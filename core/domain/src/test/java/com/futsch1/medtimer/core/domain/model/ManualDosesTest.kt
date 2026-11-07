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
    fun `relogging a trimmed match moves it to the front and keeps distinct identities`() {
        val doses = ManualDoses("").apply {
            addDose(ManualDose("Medicine A", 1, "1 tablet"))
            addDose(ManualDose("Medicine B", 2, "2 tablets"))
            addDose(ManualDose("Medicine A", 3, "1 tablet"))
            addDose(ManualDose(" Medicine A ", 1, " 1 tablet "))
        }

        assertEquals(
            listOf(
                ManualDose(" Medicine A ", 1, " 1 tablet "),
                ManualDose("Medicine A", 3, "1 tablet"),
                ManualDose("Medicine B", 2, "2 tablets"),
            ),
            doses.doses,
        )
    }

    @Test
    fun `dose identity includes name linked medicine id and amount`() {
        val doses = ManualDoses("", maxDoses = 6).apply {
            addDose(ManualDose("Medicine A", 1, "1 tablet"))
            addDose(ManualDose("Medicine A", 1, "2 tablets"))
            addDose(ManualDose("Medicine B", 1, "1 tablet"))
            addDose(ManualDose("Medicine A", 2, "1 tablet"))
        }

        assertEquals(4, doses.doses.size)
    }

    @Test
    fun `should deduplicate legacy entries without changing stored text`() {
        val doses = ManualDoses("""[{"medicineLabel":" Medicine A ","medicineId":1,"amount":"1 tablet "},{"medicineLabel":"Medicine A","medicineId":1,"amount":" 1 tablet"},{"medicineLabel":"medicine a","medicineId":1,"amount":"1 tablet"}]""")

        assertEquals(
            listOf(
                ManualDose(" Medicine A ", 1, "1 tablet "),
                ManualDose("medicine a", 1, "1 tablet"),
            ),
            doses.doses,
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

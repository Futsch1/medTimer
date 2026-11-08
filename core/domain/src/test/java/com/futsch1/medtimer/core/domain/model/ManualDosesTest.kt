package com.futsch1.medtimer.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

private const val ONE_TABLET = "1 tablet"
private const val TWO_TABLETS = "2 tablets"
private const val MEDICINE_A = "Medicine A"

class ManualDosesTest {
    @Test
    fun `should keep at most the three newest doses`() {
        val customDoses = ManualDoses("")
        customDoses.addDose(ManualDose(MEDICINE_A, 1, "1"))
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
            addDose(ManualDose(MEDICINE_A, 1, ONE_TABLET))
            addDose(ManualDose("Medicine B", 2, TWO_TABLETS))
            addDose(ManualDose(MEDICINE_A, 3, ONE_TABLET))
            addDose(ManualDose(" $MEDICINE_A ", 1, " 1 tablet "))
        }

        assertEquals(
            listOf(
                ManualDose(" $MEDICINE_A ", 1, " 1 tablet "),
                ManualDose(MEDICINE_A, 3, ONE_TABLET),
                ManualDose("Medicine B", 2, TWO_TABLETS),
            ),
            doses.doses,
        )
    }

    @Test
    fun `dose identity includes name linked medicine id and amount`() {
        val doses = ManualDoses("", maxDoses = 6).apply {
            addDose(ManualDose(MEDICINE_A, 1, ONE_TABLET))
            addDose(ManualDose(MEDICINE_A, 1, TWO_TABLETS))
            addDose(ManualDose("Medicine B", 1, ONE_TABLET))
            addDose(ManualDose(MEDICINE_A, 2, ONE_TABLET))
        }

        assertEquals(4, doses.doses.size)
    }

    @Test
    fun `should deduplicate legacy entries without changing stored text`() {
        val doses =
            ManualDoses("""[{"medicineLabel":" " + MEDICINE_A + " ","medicineId":1,"amount":"1 tablet "},{"medicineLabel":MEDICINE_A,"medicineId":1,"amount":" 1 tablet"},{"medicineLabel":MEDICINE_A,"medicineId":1,"amount":ONE_TABLET}]""")

        assertEquals(
            listOf(
                ManualDose(" $MEDICINE_A ", 1, "1 tablet "),
                ManualDose(MEDICINE_A, 1, ONE_TABLET),
            ),
            doses.doses,
        )
    }

    @Test
    fun `should round trip doses through encoding`() {
        val original = ManualDoses("").apply { addDose(ManualDose(MEDICINE_A, 7, ONE_TABLET)) }

        assertEquals(original.doses, ManualDoses(original.encode()).doses)
    }

    @Test
    fun `should tolerate corrupt encoded data`() {
        assertEquals(emptyList<ManualDose>(), ManualDoses("invalid").doses)
    }
}

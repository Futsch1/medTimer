package com.futsch1.medtimer.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MedicineTest {

    private fun medicineWith(expirationDate: LocalDate): Medicine =
        Medicine.default().copy(expirationDate = expirationDate)

    @Test
    fun `hasExpired returns true for a date in the past`() {
        val medicine = medicineWith(LocalDate.of(2020, 1, 1))
        assertEquals(true, medicine.hasExpired())
    }

    @Test
    fun `hasExpired returns false for a date far in the future`() {
        val medicine = medicineWith(LocalDate.of(2099, 1, 1))
        assertEquals(false, medicine.hasExpired())
    }

    @Test
    fun `hasExpired returns false when expirationDate is EPOCH`() {
        val medicine = medicineWith(LocalDate.EPOCH)
        assertEquals(false, medicine.hasExpired())
    }

    @Test
    fun `hasExpired returns false for today`() {
        val medicine = medicineWith(LocalDate.now())
        assertEquals(false, medicine.hasExpired())
    }

    @Test
    fun `hasExpired returns true for yesterday`() {
        val medicine = medicineWith(LocalDate.now().minusDays(1))
        assertEquals(true, medicine.hasExpired())
    }
}

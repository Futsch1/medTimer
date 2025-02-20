package com.futsch1.medtimer

import com.futsch1.medtimer.helpers.MedicineHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [34])
class MedicineHelperTest {
    @Test
    fun testParse() {
        assertEquals(3.5, MedicineHelper.parseAmount("3.5"))
        assertEquals(5.0, MedicineHelper.parseAmount("5"))
        assertEquals(3.566, MedicineHelper.parseAmount("3.566 pills"))
        assertEquals(6.23, MedicineHelper.parseAmount("Take 6.23 pills"))
        assertEquals(6.23, MedicineHelper.parseAmount("Take 6.23 pills at 5 o'clock"))
        assertNull(MedicineHelper.parseAmount("Take pills"))
        var amount = 5.0
        MedicineHelper.parseAmount("No string")?.let { amount = it }
        assertEquals(5.0, amount)
    }

    @Test
    fun testFormat() {
        assertEquals("3.5", MedicineHelper.formatAmount(3.5, ""))
        assertEquals("5 pills", MedicineHelper.formatAmount(5.0, "pills"))
    }
}
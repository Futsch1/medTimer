package com.futsch1.medtimer

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.preferences.PreferencesNames.HIDE_MED_NAME
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
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

    @Test
    fun testNormalizeMedicineName() {
        assertEquals("Aspirin", MedicineHelper.normalizeMedicineName("Aspirin (1/2)"))
        assertEquals("Aspirin", MedicineHelper.normalizeMedicineName("Aspirin"))
        assertEquals("Aspirin", MedicineHelper.normalizeMedicineName("Aspirin (11/12)"))
    }

    @Test
    fun testMedicineNameWithStockText() {
        var contextMock = mock(Context::class.java)
        var preferencesMock = mock(SharedPreferences::class.java)
        Mockito.`when`(preferencesMock.getBoolean(HIDE_MED_NAME, false)).thenReturn(false)
        var preferencesManager = mockStatic(PreferenceManager::class.java)
        preferencesManager.`when`<Any> { PreferenceManager.getDefaultSharedPreferences(contextMock) }.thenReturn(preferencesMock)

        Mockito.`when`(contextMock.getString(R.string.medicine_stock_string, "12 pills"))
            .thenReturn("12 pills left")

        // Standard case without stock
        var medicine = Medicine("test")
        medicine.unit = "pills"
        assertEquals(
            "test",
            MedicineHelper.getMedicineNameWithStockText(contextMock, medicine).toString()
        )

        // Standard case with stock
        medicine.amount = 12.0
        assertEquals(
            "test (12 pills left)",
            MedicineHelper.getMedicineNameWithStockText(contextMock, medicine).toString()
        )

        // Out of stock case
        medicine.outOfStockReminder = Medicine.OutOfStockReminderType.ONCE
        medicine.outOfStockReminderThreshold = 15.0
        assertEquals(
            "test (12 pills left ⚠)",
            MedicineHelper.getMedicineNameWithStockText(contextMock, medicine).toString()
        )
    }

    @Test
    fun testGetMedicineName() {
        var contextMock = mock(Context::class.java)
        var preferencesMock = mock(SharedPreferences::class.java)
        Mockito.`when`(preferencesMock.getBoolean(HIDE_MED_NAME, false)).thenReturn(true)
        var preferencesManager = mockStatic(PreferenceManager::class.java)
        preferencesManager.`when`<Any> { PreferenceManager.getDefaultSharedPreferences(contextMock) }.thenReturn(preferencesMock)

        var medicine = Medicine("test")
        medicine.outOfStockReminder = Medicine.OutOfStockReminderType.OFF
        medicine.outOfStockReminderThreshold = 0.0
        medicine.amount = 0.0
        assertEquals(
            "t***",
            MedicineHelper.getMedicineName(contextMock, medicine, true).toString()
        )
    }
}
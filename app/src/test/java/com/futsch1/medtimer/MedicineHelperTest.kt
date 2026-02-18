package com.futsch1.medtimer

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.preferences.PreferencesNames.HIDE_MED_NAME
import com.futsch1.medtimer.schedulertests.TestHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.time.LocalDate

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [36])
class MedicineHelperTest {
    @Test
    fun testParse() {
        assertEquals(3.5, MedicineHelper.parseAmount("3.5"))
        assertEquals(5.0, MedicineHelper.parseAmount("5"))
        assertEquals(0.5, MedicineHelper.parseAmount(".5"))
        assertEquals(50000.0, MedicineHelper.parseAmount("50000"))
        assertEquals(50000.0, MedicineHelper.parseAmount("50,000"))
        assertEquals(50000.0, MedicineHelper.parseAmount("50 000"))
        assertEquals(1000.5, MedicineHelper.parseAmount("1,000.5"))
        assertEquals(1000000.42, MedicineHelper.parseAmount("1 000 000.42"))
        assertEquals(3.566, MedicineHelper.parseAmount("3.566 pills"))
        assertEquals(6.23, MedicineHelper.parseAmount("Take 6.23 pills"))
        assertEquals(6.23, MedicineHelper.parseAmount("Take 6.23 pills at 5 o'clock"))
        assertEquals(null, MedicineHelper.parseAmount(".."))
        assertEquals(null, MedicineHelper.parseAmount("There is no number here. Only some ,. and spaces."))
        assertEquals(4.0, MedicineHelper.parseAmount("Many. 4 to be specific."))
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
    fun testFormatParseRoundTrip() {
        val amount = 123456.0
        val unit = "pills"
        val formatted = MedicineHelper.formatAmount(amount, unit)
        val parsed = MedicineHelper.parseAmount(formatted)
        assertEquals(amount, parsed)
    }

    @Test
    fun testNormalizeMedicineName() {
        assertEquals("Aspirin", MedicineHelper.normalizeMedicineName("Aspirin (1/2)"))
        assertEquals("Aspirin", MedicineHelper.normalizeMedicineName("Aspirin"))
        assertEquals("Aspirin", MedicineHelper.normalizeMedicineName("Aspirin (11/12)"))
    }

    @Test
    fun testMedicineNameWithStockText() {
        val contextMock = mock(Context::class.java)
        val preferencesMock = mock(SharedPreferences::class.java)
        Mockito.`when`(preferencesMock.getBoolean(HIDE_MED_NAME, false)).thenReturn(false)
        val preferencesManager = mockStatic(PreferenceManager::class.java)
        preferencesManager.`when`<Any> { PreferenceManager.getDefaultSharedPreferences(contextMock) }.thenReturn(preferencesMock)

        Mockito.`when`(contextMock.getString(eq(R.string.medicine_stock_string), anyString()))
            .thenAnswer { invocation ->
                "${invocation.getArgument(1, String::class.java)} left"
            }

        // Standard case without stock
        val medicine = Medicine("test")
        val fullMedicine = FullMedicine()
        fullMedicine.medicine = medicine
        fullMedicine.reminders = listOf()
        medicine.unit = "pills"
        assertEquals(
            "test",
            MedicineHelper.getMedicineNameWithStockText(contextMock, fullMedicine).toString()
        )

        // Expired case
        medicine.expirationDate = LocalDate.now().toEpochDay() - 1
        assertEquals(
            "test (\uD83D\uDEAB)",
            MedicineHelper.getMedicineNameWithStockText(contextMock, fullMedicine).toString()
        )
        medicine.expirationDate = 0

        // Standard case with stock
        medicine.amount = 12.0
        assertEquals(
            "test (12 pills left)",
            MedicineHelper.getMedicineNameWithStockText(contextMock, fullMedicine).toString()
        )

        // Out of stock case
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.outOfStockThreshold = 15.0
        reminder.outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE
        fullMedicine.reminders = listOf(reminder)
        assertEquals(
            "test (12 pills left ⚠)",
            MedicineHelper.getMedicineNameWithStockText(contextMock, fullMedicine).toString()
        )

        // Out of stock and expired case
        medicine.expirationDate = LocalDate.now().toEpochDay() - 1
        assertEquals(
            "test (12 pills left ⚠ \uD83D\uDEAB)",
            MedicineHelper.getMedicineNameWithStockText(contextMock, fullMedicine).toString()
        )

        preferencesManager.close()
    }

    @Test
    fun testGetMedicineName() {
        val contextMock = mock(Context::class.java)
        val preferencesMock = mock(SharedPreferences::class.java)
        Mockito.`when`(preferencesMock.getBoolean(HIDE_MED_NAME, false)).thenReturn(true)
        val preferencesManager = mockStatic(PreferenceManager::class.java)
        preferencesManager.`when`<Any> { PreferenceManager.getDefaultSharedPreferences(contextMock) }.thenReturn(preferencesMock)

        val medicine = Medicine("test")
        val fullMedicine = FullMedicine()
        fullMedicine.medicine = medicine
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.outOfStockThreshold = 15.0
        reminder.outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE
        fullMedicine.reminders = listOf(reminder)
        medicine.amount = 0.0
        assertEquals(
            "t***",
            MedicineHelper.getMedicineName(contextMock, medicine, true)
        )

        preferencesManager.close()
    }
}

package com.futsch1.medtimer

import android.content.Context
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.MedicineStringFormatter
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.schedulertests.TestHelper
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
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
        val contextMock = mock<Context>()
        val preferencesDataSource = mock<PreferencesDataSource>()
        val timeFormatterMock = mock<TimeFormatter>()
        `when`(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default()))

        val formatter = MedicineStringFormatter(contextMock, preferencesDataSource, timeFormatterMock)

        `when`(contextMock.getString(eq(R.string.medicine_stock_string), anyString()))
            .thenAnswer { invocation ->
                "${invocation.getArgument(1, String::class.java)} left"
            }

        // Standard case without stock
        val medicine = MedicineEntity("test")
        val fullMedicine = FullMedicineEntity()
        fullMedicine.medicine = medicine
        fullMedicine.reminders = mutableListOf()
        medicine.unit = "pills"
        assertEquals(
            "test",
            formatter.getMedicineNameWithStockText(fullMedicine).toString()
        )

        // Expired case
        medicine.expirationDate = LocalDate.now().toEpochDay() - 1
        assertEquals(
            "test (\uD83D\uDEAB)",
            formatter.getMedicineNameWithStockText(fullMedicine).toString()
        )
        medicine.expirationDate = 0

        // Standard case with stock
        medicine.amount = 12.0
        assertEquals(
            "test (12 pills left)",
            formatter.getMedicineNameWithStockText(fullMedicine).toString()
        )

        // Out of stock case
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1).copy(
            outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE,
            outOfStockThreshold = 15.0
        )
        fullMedicine.reminders = mutableListOf(reminder)
        assertEquals(
            "test (12 pills left ⚠)",
            formatter.getMedicineNameWithStockText(fullMedicine).toString()
        )

        // Out of stock and expired case
        medicine.expirationDate = LocalDate.now().toEpochDay() - 1
        assertEquals(
            "test (12 pills left ⚠ \uD83D\uDEAB)",
            formatter.getMedicineNameWithStockText(fullMedicine).toString()
        )
    }

    @Test
    fun testGetMedicineName() {
        val userPreferences = mock<UserPreferences>()
        `when`(userPreferences.hideMedicineName).thenReturn(true)

        val medicine = MedicineEntity("test")
        val fullMedicine = FullMedicineEntity()
        fullMedicine.medicine = medicine
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1).copy(
            outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE,
            outOfStockThreshold = 15.0
        )
        fullMedicine.reminders = mutableListOf(reminder)
        medicine.amount = 0.0
        assertEquals(
            "t***",
            MedicineHelper.getMedicineName(medicine, true, userPreferences)
        )
    }
}

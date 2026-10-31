package com.futsch1.medtimer

import android.content.Context
import com.futsch1.medtimer.core.common.helpers.DoseAmountParser
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.core.ui.MedicineStringFormatter
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.TimeFormatter
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MedicineHelperTest {
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
        val parsed = DoseAmountParser.parseAmount(formatted)
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

        val reminder = TestHelper.buildReminder(1, 1, "1 pills", 480, 1).copy(
            outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE,
            outOfStockThreshold = 15.0
        )
        val doseReminder = TestHelper.buildReminder(1, 2, "1 pills", 600, 1)

        val baseMedicine = Medicine.default().copy(name = "test")

        // Standard case without stock
        assertEquals(
            "test",
            formatter.getMedicineNameWithStockText(baseMedicine).toString()
        )

        // Expired case
        assertEquals(
            "test (\uD83D\uDEAB)",
            formatter.getMedicineNameWithStockText(baseMedicine.copy(expirationDate = LocalDate.now().minusDays(1)))
                .toString()
        )

        // Standard case with stock
        assertEquals(
            "test (12 pills left)",
            formatter.getMedicineNameWithStockText(
                baseMedicine.copy(
                    amount = 12.0,
                    reminders = listOf(doseReminder)
                )
            ).toString()
        )

        // Out of stock case
        assertEquals(
            "test (12 pills left ⚠)",
            formatter.getMedicineNameWithStockText(
                baseMedicine.copy(amount = 12.0, reminders = listOf(doseReminder, reminder))
            )
                .toString()
        )

        // Out of stock and expired case
        assertEquals(
            "test (12 pills left ⚠ \uD83D\uDEAB)",
            formatter.getMedicineNameWithStockText(
                baseMedicine.copy(
                    amount = 12.0,
                    reminders = listOf(doseReminder, reminder),
                    expirationDate = LocalDate.now().minusDays(1)
                )
            ).toString()
        )
    }

    @Test
    fun testGetMedicineName() {
        val userPreferences = mock<UserPreferences>()
        `when`(userPreferences.hideMedicineName).thenReturn(true)

        val medicine = Medicine.default().copy(name = "test")
        assertEquals(
            "t***",
            MedicineHelper.getMedicineName(medicine, true, userPreferences)
        )
    }
}

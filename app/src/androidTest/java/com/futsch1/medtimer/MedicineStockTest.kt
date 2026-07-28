package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaErrorAssertions.assertErrorDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.feature.ui.overview.OverviewTestTags
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

class MedicineStockTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun medicineStockTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationTitle = context.getString(R.string.out_of_stock_notification_title)

        medicines.create("Test")

        medicineEditor.setStock(amount = "", refillSize = "")
        medicineEditor.setStock(amount = amount(10.5), unit = "pills", refillSize = amount(10.8))

        // Interval reminder (amount 3.5) 10 minutes from now
        medicineEditor.addIntervalReminder("Of the pills ${amount(3.5)} are to be taken", 10)
        medicineEditor.addStockReminder(threshold = "4")

        pressBack()

        navigation.toOverview()

        // Mark reminder as taken, no out of stock reminder expected (7 left)
        overview.clickEventState(0)
        clickMenuItem(R.string.taken)
        overview.assertEventContains(MedicineHelper.formatAmount(7.0, "pills"))
        notifications.inShade { assertHidden(notificationTitle) }

        // Mark reminder as skipped (10.5 left)
        overview.clickEventState(0)
        clickMenuItem(R.string.skipped)
        notifications.inShade { assertHidden(notificationTitle) }

        // Mark reminder as taken again, no out of stock reminder expected (7 left)
        overview.clickEventState(0)
        clickMenuItem(R.string.taken)
        notifications.inShade { assertHidden(notificationTitle) }

        // Mark next instance as taken, out of stock reminder expected (3.5 left)
        overview.clickEventState(1)
        clickMenuItem(R.string.taken)
        notifications.inShade {
            assertShows(notificationTitle)
            assertShows(amount(3.5))
        }

        navigation.toMedicines()

        medicines.assertNameContains("⚠")
        medicines.assertNameContains("pills")
        medicines.clickItem(0)
        medicineEditor.assertStockAmount(MedicineHelper.formatAmount(3.5, "pills"))

        navigation.toOverview()
        clickTag(OverviewTestTags.LOG_MANUAL_DOSE)
        clickDialogItem("Test")
        writeTo(android.R.id.input, "12")
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        navigation.toMedicines()

        medicines.clickItem(0)
        medicineEditor.refillNow()
        medicineEditor.assertStockAmount(MedicineHelper.formatAmount(10.8, "pills"))

        medicines.showList()
        medicines.assertNameContains(MedicineHelper.formatAmount(10.8, "pills"))
        medicines.assertNameNotContains("⚠")
        medicines.assertNameContains("pills")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun hiddenMedicineNameInStockReminder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        settings.click(R.string.privacy_settings, R.string.hide_med_name)

        medicines.create("TestMed")

        medicineEditor.setStock(amount = "120", unit = "pills", refillSize = "100")

        medicineEditor.addIntervalReminder("So many pills - 130", 10)
        medicineEditor.addStockReminder(threshold = "0")
        pressBack()

        navigation.toOverview()
        overview.clickEventState(0)
        clickMenuItem(R.string.taken)

        notifications.inShade {
            await(context.getString(R.string.out_of_stock_notification_title).substring(0, 30))
            assertShows("T******")
            assertHidden("TestMed")
            clickAction(R.string.refill_amount, MedicineHelper.formatAmount(100.0, "pills"))
        }

        navigation.toMedicines()

        medicines.clickItem(0)
        medicineEditor.assertRefillSize(MedicineHelper.formatAmount(100.0, "pills"))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun reminderAmountWarningTest() {
        medicines.create("Test")

        medicineEditor.setStock(amount = amount(10.5), unit = "pills")
        medicineEditor.addStockReminder(threshold = "4")

        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.timeBasedCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editAmount, "something")

        assertErrorDisplayed(
            com.futsch1.medtimer.feature.ui.R.id.editAmount,
            R.string.invalid_amount
        )
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun bigStockAmounts() {
        medicines.create("Test")

        medicineEditor.setStock(amount = "10005", unit = "pills")

        medicineEditor.inStockSettings {
            assertPreferenceSummary(R.string.amount, MedicineHelper.formatAmount(10005.0, "pills"))
            assertPreferenceSummary(
                R.string.estimated_run_out_date,
                timeFormatter().localDateToString(LocalDate.now().plusDays(365))
            )
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun runOutDate() {
        medicines.create("Test")
        medicineEditor.addReminder("3", LocalTime.of(1, 0))

        medicineEditor.inStockSettings {
            preferences.setValue(R.string.amount, "10")
            assertPreferenceSummary(R.string.estimated_run_out_date, timeFormatter().localDateToString(LocalDate.now().plusDays(4)))

            preferences.setValue(R.string.amount, "13")
            assertPreferenceSummary(R.string.estimated_run_out_date, timeFormatter().localDateToString(LocalDate.now().plusDays(5)))
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun allTaken() {
        settings.click(R.string.display_settings, R.string.combine_notifications)

        medicines.create(TEST_MED)
        medicineEditor.addReminder("3", laterToday())
        medicineEditor.addReminder("2", laterToday())
        medicineEditor.addDailyStockReminder(threshold = "12", time = LocalTime.of(22, 0))

        medicineEditor.setStock(amount = "10")

        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(InstrumentationRegistry.getInstrumentation().targetContext)
        notifications.inShade {
            assertShows(TEST_MED, timeoutMillis = 5_000)
            clickAction(R.string.taken)
        }

        medicineEditor.assertStockAmount("5")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun expirationDateTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationTitle = context.getString(R.string.expiration_reminder)

        val expirationTime = Calendar.getInstance()
        val day = expirationTime.get(Calendar.DAY_OF_MONTH)
        expirationTime.set(Calendar.DAY_OF_MONTH, day + 7)

        medicines.create("Test")
        medicineEditor.inStockSettings {
            clickPreference(R.string.expiration_date)
            pickers.pickDate(expirationTime.time)
            clickPreference(R.string.clear_dates)
        }

        medicineEditor.addExpirationReminder(daysBefore = "10")
        pressBack()

        navigation.toOverview()

        notifications.inShade { assertHidden(notificationTitle) }

        navigation.toMedicines()
        medicines.clickItem(0)
        medicineEditor.inStockSettings {
            clickPreference(R.string.expiration_date)
            pickers.pickDate(expirationTime.time)
        }

        navigation.toOverview()

        overview.assertEventState(0, R.string.reminded)
        overview.clickEventState(0)
        assertMenuItemDisplayed(R.string.acknowledged)
        pressBack()

        notifications.inShade {
            assertShows(notificationTitle)
            dismiss(notificationTitle)
        }

        overview.assertEventState(0, R.string.taken)
        overview.clickEventState(0)
        clickMenuItem(R.string.delete)
        clickDialogPositiveButton()

        notifications.inShade { assertHidden(notificationTitle) }
        overview.assertEventCount(0)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun undoStockOnDeleteTest() {
        medicines.create("Test")
        medicineEditor.addReminder("2", laterToday())

        medicineEditor.setStock(amount = "10", unit = "pills")

        navigation.toOverview()

        overview.clickEventState(0)
        clickMenuItem(R.string.taken)

        navigation.toMedicines()
        medicines.clickItem(0)
        medicineEditor.assertStockAmount(MedicineHelper.formatAmount(8.0, "pills"))

        navigation.toOverview()
        overview.clickEventState(0)
        clickMenuItem(R.string.delete)
        clickDialogPositiveButton()

        navigation.toMedicines()
        medicines.clickItem(0)
        medicineEditor.assertStockAmount(MedicineHelper.formatAmount(10.0, "pills"))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun undoStockOnReraiseTest() {
        medicines.create("Test")
        medicineEditor.addReminder("2", laterToday())

        medicineEditor.setStock(amount = "10", unit = "pills")

        navigation.toOverview()

        overview.clickEventState(0)
        clickMenuItem(R.string.taken)

        navigation.toMedicines()
        medicines.clickItem(0)
        medicineEditor.assertStockAmount(MedicineHelper.formatAmount(8.0, "pills"))

        navigation.toOverview()
        overview.clickEventState(0)
        clickMenuItem(R.string.re_raise_event)
        clickDialogPositiveButton()

        navigation.toMedicines()
        medicines.clickItem(0)
        medicineEditor.assertStockAmount(MedicineHelper.formatAmount(10.0, "pills"))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun dailyStockReminderTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationTitle = context.getString(R.string.out_of_stock_notification_title)

        medicines.create("Test")

        medicineEditor.setStock(amount = amount(10.5))
        medicineEditor.addDailyStockReminder(threshold = "14", time = LocalTime.of(22, 0))

        notifications.inShade { assertHidden(notificationTitle) }
        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(context)
        notifications.inShade { assertShows(notificationTitle) }
    }
}

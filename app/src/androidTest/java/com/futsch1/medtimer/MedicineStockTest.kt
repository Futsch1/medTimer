package com.futsch1.medtimer

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.assertion.BaristaErrorAssertions.assertErrorDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime

class MedicineStockTest : BaseTestHelper() {

    @Test
    //@AllowFlaky(attempts = 1)
    fun medicineStockTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")
        // Interval reminder (amount 3.5) 10 minutes from now
        AndroidTestHelper.createIntervalReminder("Of the pills 3.5 are to be taken", 10)

        clickOn(R.id.openStockTracking)
        writeTo(R.id.amountLeft, "")
        writeTo(R.id.reminderThreshold, "")
        writeTo(R.id.refillSize, "")
        pressBack()

        clickOn(R.id.openStockTracking)
        writeTo(R.id.amountLeft, "10.5")
        writeTo(R.id.stockUnit, "pills")
        clickOn(R.id.medicineStockReminder)
        onData(equalTo(context.getString(R.string.once_below_threshold))).inRoot(RootMatchers.isPlatformPopup())
            .perform(click())
        writeTo(R.id.reminderThreshold, "4")
        writeTo(R.id.refillSize, "10.5")
        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        // Mark reminder as taken, no out of stock reminder expected
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        var o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title))),
            1_000
        )
        internalAssert(o == null)
        device.pressBack()

        // Mark reminder as skipped
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.skippedButton)

        // Mark reminder as taken again, out of stock reminder expected
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)
        device.openNotification()
        o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title))),
            1_000
        )
        internalAssert(o == null)
        device.pressBack()

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        clickListItem(R.id.medicineList, 0)
        clickOn(R.id.openStockTracking)
        assertDisplayed(R.id.amountLeft, "7")
        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        // Mark next instance as taken, out of stock reminder expected
        clickListItemChild(R.id.reminders, 1, R.id.stateButton)
        clickOn(R.id.takenButton)
        device.openNotification()
        o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title).substring(0, 30))),
            1_000
        )
        internalAssert(o != null)
        device.pressBack()

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        assertContains(R.id.medicineName, "⚠")
        assertContains(R.id.medicineName, "pills")

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickOn(R.id.logManualDose)
        clickListItem(null, 1)
        writeTo(android.R.id.input, "12")
        BaristaDialogInteractions.clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        clickListItem(R.id.medicineList, 0)
        clickOn(R.id.openStockTracking)
        clickOn(R.id.refillNow)

        val numberFormat = NumberFormat.getNumberInstance()
        numberFormat.minimumFractionDigits = 0
        numberFormat.maximumFractionDigits = 2

        assertDisplayed(R.id.amountLeft, numberFormat.format(10.5))
        pressBack()
        pressBack()

        assertContains(R.id.medicineName, numberFormat.format(10.5))
        assertNotContains(R.id.medicineName, "⚠")
        assertContains(R.id.medicineName, "pills")
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun hiddenMedicineNameInStockReminder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.privacy_settings)
        clickOn(R.string.hide_med_name)

        AndroidTestHelper.createMedicine("TestMed")

        clickOn(R.id.openStockTracking)
        writeTo(R.id.amountLeft, "120")
        writeTo(R.id.stockUnit, "pills")
        clickOn(R.id.medicineStockReminder)
        onData(equalTo(context.getString(R.string.once_below_threshold))).inRoot(RootMatchers.isPlatformPopup())
            .perform(click())
        pressBack()

        AndroidTestHelper.createIntervalReminder("So many pills - 130", 10)

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title).substring(0, 30))),
            1_000
        )
        internalAssert(device.findObject(By.textContains("T******")) != null)
        internalAssert(device.findObject(By.textContains("TestMed")) == null)
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun reminderAmountWarningTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AndroidTestHelper.createMedicine("Test")

        clickOn(R.id.openStockTracking)
        writeTo(R.id.amountLeft, "10.5")
        writeTo(R.id.stockUnit, "pills")
        clickOn(R.id.medicineStockReminder)
        onData(equalTo(context.getString(R.string.once_below_threshold))).inRoot(RootMatchers.isPlatformPopup())
            .perform(click())
        writeTo(R.id.reminderThreshold, "4")
        pressBack()

        clickOn(R.id.addReminder)
        clickOn(R.id.timeBasedCard)
        writeTo(R.id.editAmount, "something")

        assertErrorDisplayed(R.id.editAmount, R.string.invalid_amount)
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun bigStockAmounts() {
        AndroidTestHelper.createMedicine("Test")

        clickOn(R.id.openStockTracking)
        writeTo(R.id.amountLeft, "10005")
        writeTo(R.id.stockUnit, "pills")

        pressBack()
        clickOn(R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(10005.0, ""))
        assertDisplayed(R.id.runOut, "---")

        pressBack()
        clickOn(R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(10005.0, ""))
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun runOutDate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("3", LocalTime.of(1, 0))

        clickOn(R.id.openStockTracking)
        writeTo(R.id.amountLeft, "10")
        assertDisplayed(R.id.runOut, TimeHelper.localDateToDateString(context, LocalDate.now().plusDays(4)))

        writeTo(R.id.amountLeft, "13")
        assertDisplayed(R.id.runOut, TimeHelper.localDateToDateString(context, LocalDate.now().plusDays(5)))
    }
}

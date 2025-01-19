package com.futsch1.medtimer

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test

class MedicineStockTest : BaseTestHelper() {

    @Test
    @AllowFlaky(attempts = 1)
    fun medicineStockTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")
        // Interval reminder (amount 3.5) 10 minutes from now
        AndroidTestHelper.createIntervalReminder("Of the pills 3.5 are to be taken", 10)

        clickOn(R.id.openStockTracking)
        writeTo(R.id.amountLeft, "10.5")
        clickOn(R.id.medicineStockReminder)
        onData(equalTo(context.getString(R.string.once_below_threshold))).inRoot(RootMatchers.isPlatformPopup())
            .perform(click())
        writeTo(R.id.reminderThreshold, "4")
        writeTo(R.id.reminderThreshold, "4")
        writeTo(R.id.refillSize, "10.5")
        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        clickListItemChild(R.id.latestReminders, 0, R.id.chipTaken)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        var o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title))),
            1_000
        )
        internalAssert(o == null)
        device.pressBack()

        clickListItemChild(R.id.latestReminders, 0, R.id.chipSkipped)
        device.openNotification()
        o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title))),
            1_000
        )
        internalAssert(o == null)
        device.pressBack()

        clickListItemChild(R.id.latestReminders, 0, R.id.chipTaken)
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

        clickListItemChild(R.id.nextReminders, 0, R.id.takenNow)
        device.openNotification()
        o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title))),
            1_000
        )
        internalAssert(o != null)
        device.pressBack()

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        assertContains(R.id.medicineName, "⚠")

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

        assertDisplayed(R.id.amountLeft, "10")
        pressBack()
        pressBack()

        assertContains(R.id.medicineName, "10")
        assertNotContains(R.id.medicineName, "⚠")
    }
}
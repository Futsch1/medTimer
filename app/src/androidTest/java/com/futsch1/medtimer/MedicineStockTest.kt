package com.futsch1.medtimer

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MedicineStockTest : BaseTestHelper() {

    @Test
    //@AllowFlaky(attempts = 1)
    fun medicineStockTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")
        // Interval reminder (amount 3) 10 minutes from now
        AndroidTestHelper.createIntervalReminder("3", 10)

        clickOn(R.id.openStockTracking)
        writeTo(R.id.amountLeft, "10")
        clickOn(R.id.medicineStockReminder)
        onData(equalTo(context.getString(R.string.once_below_threshold))).inRoot(RootMatchers.isPlatformPopup())
            .perform(click())
        writeTo(R.id.reminderThreshold, "4")
        onView(withId(R.id.reminderThreshold)).perform(replaceText("4"), closeSoftKeyboard())
        writeTo(R.id.refillSize, "10")
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
        assertNull(o)
        device.pressBack()

        clickListItemChild(R.id.latestReminders, 0, R.id.chipSkipped)
        device.openNotification()
        o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title))),
            1_000
        )
        assertNull(o)
        device.pressBack()

        clickListItemChild(R.id.latestReminders, 0, R.id.chipTaken)
        device.openNotification()
        o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title))),
            1_000
        )
        assertNull(o)
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
        assertNotNull(o)
        device.pressBack()

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        assertContains(R.id.medicineName, "⚠")

        clickListItem(R.id.medicineList, 0)
        clickOn(R.id.openStockTracking)
        clickOn(R.id.refillNow)

        assertDisplayed(R.id.amountLeft, "14")
        pressBack()
        pressBack()

        assertContains(R.id.medicineName, "14")
        assertNotContains(R.id.medicineName, "⚠")
    }
}
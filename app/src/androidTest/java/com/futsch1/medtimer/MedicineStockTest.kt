package com.futsch1.medtimer

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@LargeTest
class MedicineStockTest : BaseTestHelper() {

    @JvmField
    @Rule
    var mActivityScenarioRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(
        MainActivity::class.java
    )

    @Test
    fun medicineStockTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")
        // Interval reminder (amount 3) 10 minutes from now
        AndroidTestHelper.createIntervalReminder("3", 10)

        onView(withId(R.id.openStockTracking)).perform(click())
        onView(withId(R.id.amountLeft)).perform(replaceText("10"), closeSoftKeyboard())
        onView(withId(R.id.medicineStockReminder)).perform(click())
        onData(equalTo(context.getString(R.string.once_below_threshold))).inRoot(RootMatchers.isPlatformPopup())
            .perform(click())
        onView(withId(R.id.reminderThreshold)).perform(replaceText("4"), closeSoftKeyboard())
        onView(withId(R.id.refillSize)).perform(replaceText("10"), closeSoftKeyboard())
        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        onView(
            RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.chipTaken)
        ).perform(
            scrollTo(), click()
        )
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        var o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title))),
            1_000
        )
        assertNull(o)
        device.pressBack()

        onView(
            RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.chipSkipped)
        ).perform(
            click()
        )
        device.openNotification()
        o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title))),
            1_000
        )
        assertNull(o)
        device.pressBack()

        onView(
            RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.chipTaken)
        ).perform(
            click()
        )
        device.openNotification()
        o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title))),
            1_000
        )
        assertNull(o)
        device.pressBack()

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        onView(
            RecyclerViewMatcher(R.id.medicineList).atPositionOnView(0, R.id.medicineCard)
        ).perform(
            click()
        )
        onView(withId(R.id.openStockTracking)).perform(click())
        onView(withId(R.id.amountLeft)).check(matches(withText("7")))
        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        onView(RecyclerViewMatcher(R.id.nextReminders).atPositionOnView(0, R.id.takenNow)).perform(
            scrollTo(), click()
        )
        device.openNotification()
        o = device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title))),
            1_000
        )
        assertNotNull(o)
        device.pressBack()

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        onView(withId(R.id.medicineName)).check(matches(withText(containsString("⚠"))))

        onView(
            RecyclerViewMatcher(R.id.medicineList).atPositionOnView(0, R.id.medicineCard)
        ).perform(
            click()
        )
        onView(withId(R.id.openStockTracking)).perform(click())
        onView(withId(R.id.refillNow)).perform(click())

        onView(withId(R.id.amountLeft)).check(matches(withText("14")))
        pressBack()
        pressBack()

        onView(withId(R.id.medicineName)).check(matches(withText(containsString("14"))))
        onView(withId(R.id.medicineName)).check(matches(not(withText(containsString("⚠")))))
    }
}
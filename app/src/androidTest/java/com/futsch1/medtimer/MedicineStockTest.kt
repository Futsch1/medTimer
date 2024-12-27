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
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import org.hamcrest.CoreMatchers.equalTo
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
        val context =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")
        // Interval reminder (amount 3) 10 minutes from now
        onView(withId(R.id.addReminder)).perform(click())

        onView(withId(R.id.editAmount))
            .perform(replaceText("3"), closeSoftKeyboard())
        onView(withId(R.id.intervalBased)).perform(click())
        onView(withId(R.id.intervalMinutes)).perform(click())
        onView(withId(R.id.editIntervalTime))
            .perform(replaceText("10"), closeSoftKeyboard())
        onView(withId(R.id.createReminder)).perform(click())

        onView(withId(R.id.openStockTracking)).perform(click())
        onView(withId(R.id.amountLeft)).perform(replaceText("10"), closeSoftKeyboard())
        onView(withId(R.id.medicineStockReminder)).perform(click())
        onData(equalTo(context.getString(R.string.once_below_threshold))).inRoot(RootMatchers.isPlatformPopup())
            .perform(click())
        onView(withId(R.id.reminderThreshold)).perform(replaceText("7"), closeSoftKeyboard())

        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        onView(RecyclerViewMatcher(R.id.nextReminders).atPositionOnView(0, R.id.takenNow)).perform(
            scrollTo(), click()
        )

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        onView(
            RecyclerViewMatcher(R.id.medicineList).atPositionOnView(
                0,
                R.id.medicineCard
            )
        ).perform(click())
        onView(withId(R.id.openStockTracking)).perform(click())
        onView(withId(R.id.amountLeft)).check(matches(withText("7")))
    }
}
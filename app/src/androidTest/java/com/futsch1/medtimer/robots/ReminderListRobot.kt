package com.futsch1.medtimer.robots

import android.os.SystemClock
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import org.hamcrest.Matcher
import org.hamcrest.Matchers

/** The reminders listed on a medicine, addressed by their position in the list. */
class ReminderListRobot(
    private val composeUi: ComposeUi,
    private val settings: ReminderSettingsRobot
) {

    fun assertCount(expected: Int) = assertListItemCount(REMINDER_LIST, expected)

    /** Position-independent: reminder order follows the reminder times, which move with the clock. */
    fun assertContains(text: String) {
        onView(
            Matchers.allOf(
                ViewMatchers.withSubstring(text),
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(REMINDER_LIST)),
            )
            // Effective visibility rather than isDisplayed: a card further down the list is bound but
            // off-screen, and this asserts the reminder exists rather than where it sits.
        ).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    fun assertContainsTime(text: String) {
        onView(
            Matchers.allOf(
                ViewMatchers.withId(REMINDER_TIME),
                ViewMatchers.withSubstring(text),
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(REMINDER_LIST)),
            )
        ).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    /** Opens the advanced settings of the reminder at [position], runs [block] and comes back out. */
    fun inSettingsOf(position: Int, block: ReminderSettingsRobot.() -> Unit) {
        openSettings(position)
        settings.block()
        pressBack()
    }

    /** Opening and closing the settings is what redraws a row whose reminder type changed. */
    fun reopenSettings(position: Int) = inSettingsOf(position) {}

    /** Adds a reminder linked to the one at [position]; the settings screen closes itself afterwards. */
    fun addLinkedReminder(position: Int, amount: String? = null, hours: Int = 0, minutes: Int) {
        openSettings(position)
        settings.addLinkedReminder(amount, hours, minutes)
    }

    fun delete(position: Int) {
        openSettings(position)
        // Deleting closes the settings screen itself, so there is nothing left to navigate back from.
        settings.delete()
    }

    fun duplicate(position: Int) {
        openSettings(position)
        settings.duplicate()
    }

    /**
     * Scrolls to the end of the reminders and taps the amount of the last one, as a user does, then checks
     * that the amount ends up above the on-screen keyboard rather than underneath it.
     */
    fun assertLastAmountStaysAboveKeyboard() {
        onReminderList("scroll to the end of the reminders") { list ->
            // In landscape the list scrolls on its own; in portrait the screen around it does.
            list.scrollToPosition(list.lastPosition)
            list.scrollBy(0, list.computeVerticalScrollRange())
            generateSequence(list.parent) { it.parent }.filterIsInstance<NestedScrollView>().firstOrNull()
                ?.let { it.scrollTo(0, it.getChildAt(0).height) }
        }
        tapLastAmount()

        val deadline = SystemClock.uptimeMillis() + KEYBOARD_TIMEOUT_MS
        var placement = AmountPlacement(keyboardTop = null, amountBottom = 0, focused = false)
        while (SystemClock.uptimeMillis() < deadline) {
            onLastAmount("measure the last amount") { placement = it.placement() }
            if (placement.isAboveKeyboard) return
            SystemClock.sleep(POLL_MS)
        }
        throw AssertionError(
            if (placement.keyboardTop == null) "The on-screen keyboard did not show"
            else "The last amount (focused: ${placement.focused}) ends at y=${placement.amountBottom}, below the keyboard top at y=${placement.keyboardTop}"
        )
    }

    private data class AmountPlacement(val keyboardTop: Int?, val amountBottom: Int, val focused: Boolean) {
        val isAboveKeyboard get() = keyboardTop != null && amountBottom <= keyboardTop
    }

    /** A real touch through the input system, so the field takes focus and shows the keyboard as for a user. */
    private fun tapLastAmount() {
        val center = IntArray(2)
        onLastAmount("locate the last amount") { amount ->
            amount.getLocationOnScreen(center)
            center[0] += amount.width / 2
            center[1] += amount.height / 2
        }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(center[0], center[1])
    }

    private fun View.placement(): AmountPlacement {
        val keyboardHeight = ViewCompat.getRootWindowInsets(this)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        val location = IntArray(2)
        getLocationInWindow(location)
        return AmountPlacement(
            keyboardTop = (rootView.height - keyboardHeight).takeIf { keyboardHeight > 0 },
            amountBottom = location[1] + height,
            focused = hasFocus(),
        )
    }

    private val RecyclerView.lastPosition get() = checkNotNull(adapter).itemCount - 1

    private fun onLastAmount(description: String, block: (View) -> Unit) = onReminderList(description) { list ->
        val itemView = list.findViewHolderForAdapterPosition(list.lastPosition)?.itemView
            ?: throw AssertionError("The last reminder is not laid out")
        block(itemView.findViewById(EDIT_AMOUNT))
    }

    private fun onReminderList(description: String, block: (RecyclerView) -> Unit) {
        onView(ViewMatchers.withId(REMINDER_LIST)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = ViewMatchers.isAssignableFrom(RecyclerView::class.java)
            override fun getDescription() = description
            override fun perform(uiController: UiController, view: View) {
                block(view as RecyclerView)
                uiController.loopMainThreadUntilIdle()
            }
        })
    }

    private fun openSettings(position: Int) {
        clickListItemChild(REMINDER_LIST, position, com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)
        composeUi.settle()
    }

    private companion object {
        val REMINDER_LIST = com.futsch1.medtimer.feature.ui.R.id.reminderList
        val REMINDER_TIME = com.futsch1.medtimer.feature.ui.R.id.editReminderTime
        val EDIT_AMOUNT = com.futsch1.medtimer.feature.ui.R.id.editAmount
        const val KEYBOARD_TIMEOUT_MS = 30_000L
        const val POLL_MS = 500L
    }
}

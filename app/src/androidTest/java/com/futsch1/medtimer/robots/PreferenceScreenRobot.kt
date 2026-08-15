package com.futsch1.medtimer.robots

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.futsch1.medtimer.utilities.pollUntil
import com.futsch1.medtimer.utilities.viewAppears
import org.hamcrest.Matcher
import org.hamcrest.Matchers

/**
 * Rows of an androidx preference screen - still Views, and still the shape of most of the app's
 * settings. Barista's text assertions match any displayed view in any window, so a bare value like
 * "5" hits whatever else happens to show a 5; these scope to one row's title or summary.
 */
class PreferenceScreenRobot(private val ui: ComposeUi, private val dialogs: DialogRobot) {

    fun click(@StringRes titleRes: Int) {
        val title = ui.getString(titleRes)
        scrollToRow(title)
        onView(titleMatcher(title)).perform(ViewActions.click())
    }

    /** Leaves a nested preference screen opened by [click]. */
    fun back() = pressBack()

    /** Backs out of up to [screens] screens: a press with nothing to pop finishes the activity. */
    fun leave(screens: Int) {
        repeat(screens) {
            if (!onPreferenceScreen()) return
            pressBack()
        }
    }

    private fun onPreferenceScreen(): Boolean =
        viewAppears(Matchers.allOf(ViewMatchers.withId(androidx.preference.R.id.recycler_view), ViewMatchers.isDisplayed()))

    /** Opens the row's edit dialog and replaces its value. */
    fun setValue(@StringRes titleRes: Int, value: String) {
        click(titleRes)
        writeTo(android.R.id.edit, value)
        closeKeyboard()
        dialogs.confirm()
    }

    fun assertSummary(@StringRes titleRes: Int, expected: String) {
        val title = ui.getString(titleRes)
        scrollToRow(title)

        pollUntil { summaryText(title)?.contains(expected) == true }
        onView(summaryMatcher(title))
            .check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString(expected))))
    }

    /** Rows below the fold are not matchable until the list has scrolled them into place. */
    private fun scrollToRow(title: String) {
        onView(Matchers.allOf(ViewMatchers.withId(androidx.preference.R.id.recycler_view), ViewMatchers.isDisplayed()))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    ViewMatchers.hasDescendant(titleMatcher(title))
                )
            )
    }

    /** Reads the summary rather than asserting on it, so a value that is not there yet can be retried. */
    private fun summaryText(title: String): String? {
        var text: String? = null
        onView(summaryMatcher(title)).check { view, _ -> text = (view as? TextView)?.text?.toString() }
        return text
    }

    private fun titleMatcher(title: String): Matcher<View> = Matchers.allOf(
        ViewMatchers.withId(android.R.id.title),
        ViewMatchers.withText(title),
        ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
    )

    private fun summaryMatcher(title: String): Matcher<View> = Matchers.allOf(
        ViewMatchers.withId(android.R.id.summary),
        ViewMatchers.hasSibling(titleMatcher(title)),
    )
}

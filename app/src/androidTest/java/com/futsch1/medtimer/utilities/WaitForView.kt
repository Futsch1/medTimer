package com.futsch1.medtimer.utilities

import android.os.SystemClock
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.Root
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.Matcher
import java.util.concurrent.TimeoutException

const val POLL_INTERVAL = 100L

/**
 * Waits for a view matching [matcher] to appear, then returns a [ViewInteraction] for it.
 * Use instead of [onView] when the view appears from async operations Espresso doesn't track.
 * Optional [inRoot] scopes the interaction to that root.
 */
fun awaitView(
    matcher: Matcher<View>,
    timeoutMillis: Long = DEFAULT_POLL_TIMEOUT,
    inRoot: Matcher<Root>? = null
): ViewInteraction {
    onView(isRoot())
        .let { if (inRoot != null) it.inRoot(inRoot) else it }
        .perform(waitForView(matcher, timeoutMillis))
    return onView(matcher).let { if (inRoot != null) it.inRoot(inRoot) else it }
}

private fun waitForView(matcher: Matcher<View>, timeoutMillis: Long): ViewAction =
    object : ViewAction {
        override fun getConstraints() = isRoot()

        override fun getDescription() = "wait up to $timeoutMillis ms for: $matcher"

        override fun perform(uiController: UiController, view: View) {
            val deadline = SystemClock.uptimeMillis() + timeoutMillis
            while (TreeIterables.breadthFirstViewTraversal(view).none { matcher.matches(it) }) {
                if (SystemClock.uptimeMillis() >= deadline) {
                    throw PerformException.Builder()
                        .withActionDescription(description)
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(TimeoutException("No view matching $matcher found within $timeoutMillis ms"))
                        .build()
                }
                uiController.loopMainThreadForAtLeast(POLL_INTERVAL)
            }
        }
    }

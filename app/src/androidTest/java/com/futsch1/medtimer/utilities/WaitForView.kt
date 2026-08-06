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

private const val DEFAULT_POLL_TIMEOUT = 15_000L
private const val POLL_INTERVAL = 100L

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

/**
 * Whether a view matching [matcher] shows up within [timeoutMillis], as a plain answer.
 * Unlike [awaitView] a miss is not a failure, so it does not trip the screenshot-on-failure handler -
 * for callers that have something else to try when the view is not there.
 */
fun viewAppears(matcher: Matcher<View>, timeoutMillis: Long = 0): Boolean {
    var appeared = false
    onView(isRoot()).perform(object : ViewAction {
        override fun getConstraints() = isRoot()

        override fun getDescription() = "check up to $timeoutMillis ms for: $matcher"

        override fun perform(uiController: UiController, view: View) {
            val deadline = SystemClock.uptimeMillis() + timeoutMillis
            do {
                appeared = TreeIterables.breadthFirstViewTraversal(view).any { matcher.matches(it) }
                if (appeared) return
                uiController.loopMainThreadForAtLeast(POLL_INTERVAL)
            } while (SystemClock.uptimeMillis() < deadline)
        }
    })
    return appeared
}

/**
 * Whether every view matching [matcher] is gone within [timeoutMillis], as a plain answer.
 * For a confirmed dialog whose window is still up, so the next interaction lands behind it, not on it.
 */
fun viewDisappears(matcher: Matcher<View>, timeoutMillis: Long = DEFAULT_POLL_TIMEOUT): Boolean {
    var gone = false
    onView(isRoot()).perform(object : ViewAction {
        override fun getConstraints() = isRoot()

        override fun getDescription() = "wait up to $timeoutMillis ms for: $matcher to disappear"

        override fun perform(uiController: UiController, view: View) {
            val deadline = SystemClock.uptimeMillis() + timeoutMillis
            do {
                // A detached root is a window that has already gone, children and all.
                gone = !view.isAttachedToWindow ||
                        TreeIterables.breadthFirstViewTraversal(view).none { matcher.matches(it) }
                if (gone) return
                uiController.loopMainThreadForAtLeast(POLL_INTERVAL)
            } while (SystemClock.uptimeMillis() < deadline)
        }
    })
    return gone
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

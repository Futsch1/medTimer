package com.futsch1.medtimer

import android.graphics.Rect
import android.view.View
import androidx.core.graphics.toRectF
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Description
import org.hamcrest.Matcher
import java.util.concurrent.atomic.AtomicReference

object RecyclerViewDragAction {

    private const val DRAG_DURATION_MS = 1500
    private const val DRAG_SETTLE_MS = 1000L

    /**
     * Drag-reorders the item at [fromPosition] to [toPosition] in the RecyclerView with id [recyclerViewId].
     *
     * The gesture is injected with UiAutomator's `input draganddrop` (a real long-press drag) rather than
     * Espresso's UiController. Espresso's perform() synchronises on the main looper being idle, but the
     * RecyclerView is hosted inside a Compose AndroidViewBinding (the adaptive navigation shell); on API <= 28,
     * where Compose draws layers with ViewLayer instead of a hardware RenderNode, the View<->Compose interop
     * re-invalidates every frame for the duration of the drag, so the looper never idles and Espresso times out
     * with AppNotIdleException. Injecting the drag outside Espresso avoids the idle requirement during the
     * gesture; the settle afterwards runs on the test thread, leaving the main thread free to finish the drop
     * animation so the following Espresso assertions can synchronise on idle normally.
     */
    fun drag(recyclerViewId: Int, fromPosition: Int, toPosition: Int) {
        val from = AtomicReference<IntArray>()
        val to = AtomicReference<IntArray>()
        onView(withId(recyclerViewId)).perform(captureDragCoordinates(fromPosition, toPosition, from, to))

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val start = from.get()
        val end = to.get()
        device.executeShellCommand("input draganddrop ${start[0]} ${start[1]} ${end[0]} ${end[1]} $DRAG_DURATION_MS")

        Thread.sleep(DRAG_SETTLE_MS)
    }

    private fun captureDragCoordinates(
        fromPosition: Int,
        toPosition: Int,
        from: AtomicReference<IntArray>,
        to: AtomicReference<IntArray>,
    ): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> =
            object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
                override fun describeTo(description: Description) {
                    description.appendText("is a RecyclerView")
                }

                override fun matchesSafely(recyclerView: RecyclerView): Boolean = true
            }

        override fun getDescription(): String = "capture drag coordinates from $fromPosition to $toPosition"

        override fun perform(uiController: UiController, view: View) {
            val recyclerView = view as RecyclerView
            val layoutManager = checkNotNull(recyclerView.layoutManager) { "RecyclerView has no LayoutManager" }
            val startView = checkNotNull(layoutManager.findViewByPosition(fromPosition)) { "No laid-out view at position $fromPosition" }
            val targetView = checkNotNull(layoutManager.findViewByPosition(toPosition)) { "No laid-out view at position $toPosition" }

            val startRectF = Rect().apply { startView.getGlobalVisibleRect(this) }.toRectF()
            from.set(intArrayOf(startRectF.centerX().toInt(), startRectF.centerY().toInt()))

            val targetRectF = Rect().apply { targetView.getGlobalVisibleRect(this) }.toRectF()

            // Drop onto the far edge of the target row (its bottom when moving down, its top when moving up) so
            // ItemTouchHelper settles the dragged item past it into the intended slot.
            val targetY = if (fromPosition < toPosition) targetRectF.bottom else targetRectF.top
            to.set(intArrayOf(targetRectF.centerX().toInt(), targetY.toInt()))
        }
    }
}

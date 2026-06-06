package com.futsch1.medtimer

import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toRectF
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.futsch1.medtimer.RecyclerViewDragAction.LONG_PRESS_HOLD_MS
import org.hamcrest.Description
import org.hamcrest.Matcher
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

object RecyclerViewDragAction {

    // The medicine list reorders via ItemTouchHelper.SimpleCallback, which leaves isLongPressDragEnabled() at its
    // default of true: a drag only starts after the pointer is held stationary past the long-press timeout
    // (ViewConfiguration.getLongPressTimeout(), 500ms by default). Hold comfortably longer so the gesture engages
    // even on a slow CI emulator.
    private const val LONG_PRESS_HOLD_MS = 800L
    private const val HOLD_PULSE_MS = 50L
    private const val MOVE_STEPS = 20
    private const val MOVE_STEP_MS = 30L
    private const val DRAG_SETTLE_MS = 1000L

    /**
     * Drag-reorders the item at [fromPosition] to [toPosition] in the RecyclerView with id [recyclerViewId].
     *
     * The gesture is injected as a raw [MotionEvent] stream via UiAutomation rather than through Espresso's
     * UiController. Espresso's perform() synchronises on the main looper being idle, but the RecyclerView is hosted
     * inside a Compose AndroidViewBinding (the adaptive navigation shell); on API <= 28, where Compose draws layers
     * with ViewLayer instead of a hardware RenderNode, the View<->Compose interop re-invalidates every frame for the
     * duration of the drag, so the looper never idles and Espresso times out with AppNotIdleException. Injecting at
     * the input layer avoids the idle requirement during the gesture.
     *
     * Unlike `input draganddrop`, this hand-built gesture holds the pointer stationary at the start for
     * [LONG_PRESS_HOLD_MS] so ItemTouchHelper's long-press drag actually engages before the pointer moves. Without
     * that hold the first MOVE can arrive before onLongPress fires, the gesture degrades to a tap that opens the
     * tapped medicine, and the following assertions fail because the list is no longer on screen.
     */
    fun drag(recyclerViewId: Int, fromPosition: Int, toPosition: Int) {
        val from = AtomicReference<IntArray>()
        val to = AtomicReference<IntArray>()
        onView(withId(recyclerViewId)).perform(captureDragCoordinates(fromPosition, toPosition, from, to))

        val start = from.get()
        val end = to.get()
        val downTime = SystemClock.uptimeMillis()

        inject(MotionEvent.ACTION_DOWN, downTime, start[0], start[1])

        // Keep the pointer alive at the down position (within touch slop, so the long-press timer is not cancelled)
        // until ItemTouchHelper has had time to start the drag.
        var held = 0L
        while (held < LONG_PRESS_HOLD_MS) {
            Thread.sleep(HOLD_PULSE_MS)
            held += HOLD_PULSE_MS
            inject(MotionEvent.ACTION_MOVE, downTime, start[0], start[1])
        }

        // Walk the pointer to the target so the reorder registers step by step.
        for (step in 1..MOVE_STEPS) {
            val fraction = step.toFloat() / MOVE_STEPS
            val x = (start[0] + (end[0] - start[0]) * fraction).roundToInt()
            val y = (start[1] + (end[1] - start[1]) * fraction).roundToInt()
            inject(MotionEvent.ACTION_MOVE, downTime, x, y)
            Thread.sleep(MOVE_STEP_MS)
        }

        inject(MotionEvent.ACTION_UP, downTime, end[0], end[1])

        Thread.sleep(DRAG_SETTLE_MS)
    }

    private fun inject(action: Int, downTime: Long, x: Int, y: Int) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x.toFloat(), y.toFloat(), 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(event, true)
        event.recycle()
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

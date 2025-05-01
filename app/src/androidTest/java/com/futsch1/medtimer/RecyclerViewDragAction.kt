package com.futsch1.medtimer

import android.graphics.Rect
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.graphics.toRectF
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.MotionEvents
import androidx.test.espresso.action.Press
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

object RecyclerViewDragAction {

    fun drag(fromPosition: Int, toPosition: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
                    override fun describeTo(description: Description) {
                        description.appendText("is a RecyclerView")
                    }

                    override fun matchesSafely(recyclerView: RecyclerView): Boolean {
                        return true
                    }
                }
            }

            override fun getDescription(): String {
                return "drag from position $fromPosition to position $toPosition"
            }

            fun interpolateDragging(
                start: FloatArray,
                end: FloatArray,
                count: Int,
            ): List<FloatArray> {
                return List(count) { i ->
                    floatArrayOf(
                        start[0] + (end[0] - start[0]) * i / count,
                        start[1] + (end[1] - start[1]) * i / count,
                    )
                }
            }

            override fun perform(uiController: UiController, view: View) {
                val recyclerView = view as RecyclerView
                val layoutManager = recyclerView.layoutManager
                val startView = layoutManager?.findViewByPosition(fromPosition)
                val targetView = layoutManager?.findViewByPosition(toPosition)
                val precision = Press.PINPOINT.describePrecision()
                val startRect = Rect()
                startView?.getGlobalVisibleRect(startRect)
                val startRectF = startRect.toRectF()
                val from = floatArrayOf(startRectF.centerX(), startRectF.centerY())
                val targetRect = Rect()
                targetView?.getGlobalVisibleRect(targetRect)
                val targetRectF = targetRect.toRectF()
                val to = floatArrayOf(targetRectF.centerX(), if (fromPosition < toPosition) targetRectF.bottom else targetRectF.top)

                val downEvent = MotionEvents.sendDown(
                    uiController, from, precision,
                    InputDevice.SOURCE_TOUCHSCREEN, //Necessary for SelectionTracker
                    MotionEvent.BUTTON_PRIMARY,
                ).down

                try {
                    val longPressTimeout = (ViewConfiguration.getLongPressTimeout() * 1.5)
                    uiController.loopMainThreadForAtLeast(longPressTimeout.toLong())

                    val steps = interpolateDragging(from, to, 50)

                    uiController.loopMainThreadUntilIdle()

                    for (step in steps) {
                        if (!MotionEvents.sendMovement(uiController, downEvent, step)) {
                            MotionEvents.sendCancel(uiController, downEvent)
                        }
                    }

                    if (!MotionEvents.sendUp(uiController, downEvent, to)) {
                        MotionEvents.sendCancel(uiController, downEvent)
                    }
                    uiController.loopMainThreadUntilIdle()
                } finally {
                    downEvent.recycle()
                }
            }
        }
    }
}
package com.futsch1.medtimer.overview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs

interface OnSwipeListener {
    fun onSwipeLeft()
    fun onSwipeRight()
    fun onSwipeUp()
    fun onSwipeDown()
}

class FragmentSwipeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var gestureDetector = GestureDetector(context, GestureListener())
    private var swipeThreshold = ViewConfiguration.get(context).scaledTouchSlop
    private var swipeVelocityThreshold = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    var onSwipeListener: OnSwipeListener? = null

    private var startX = 0f
    private var startY = 0f
    private var isIntercepting = false

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = e2.x - (e1?.x ?: 0f)
            val diffY = e2.y - (e1?.y ?: 0f)

            if (abs(diffX) > abs(diffY)) { // Horizontal swipe
                if (handleHorizontalSwipe(diffX, velocityX)) return true
            } else { // Vertical swipe
                if (handleVerticalSwipe(diffY, velocityY)) return true
            }
            return false // Let other touch listeners handle it if it's not a swipe
        }

        private fun handleVerticalSwipe(diffY: Float, velocityY: Float): Boolean {
            if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                if (diffY > 0) {
                    onSwipeListener?.onSwipeDown()
                } else {
                    onSwipeListener?.onSwipeUp()
                }
                return true
            }
            return false
        }

        private fun handleHorizontalSwipe(diffX: Float, velocityX: Float): Boolean {
            if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                if (diffX > 0) {
                    onSwipeListener?.onSwipeRight()
                } else {
                    onSwipeListener?.onSwipeLeft()
                }
                return true
            }
            return false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event!!) || super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        val event = ev ?: return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                isIntercepting = false
                gestureDetector.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                val diffX = abs(event.x - startX)
                val diffY = abs(event.y - startY)
                
                // If horizontal movement is dominant and exceeds threshold, intercept
                if (diffX > swipeThreshold && diffX > diffY) {
                    isIntercepting = true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isIntercepting = false
            }
        }
        
        if (isIntercepting) {
            return true
        }

        // Still feed the gesture detector so it can detect flings if we decide to intercept later
        gestureDetector.onTouchEvent(event)
        return super.onInterceptTouchEvent(ev)
    }
}
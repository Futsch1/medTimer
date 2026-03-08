package com.futsch1.medtimer.helpers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

abstract class SwipeHelper protected constructor(context: Context, dragDirs: Int, icon: Int) : ItemTouchHelper.SimpleCallback(dragDirs, SWIPE_DIRS) {
    private val intrinsicWidth: Int
    private val intrinsicHeight: Int

    private val clearPaint: Paint = Paint()
    private val swipeIcon: Drawable = ContextCompat.getDrawable(context, icon)!!
    private val background = ColorDrawable()
    var fromPosition: Int = -1
    var toPosition: Int = 0

    init {
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        intrinsicHeight = swipeIcon.intrinsicHeight
        intrinsicWidth = swipeIcon.intrinsicWidth

        setDefaultSwipeDirs(SWIPE_DIRS)
    }

    override fun onChildDrawOver(
        c: Canvas, recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
        actionState: Int, isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val isCanceled = (dX == 0f) && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false)
            return
        }

        if (dX < 0) {
            val itemHeight = itemView.bottom - itemView.top
            
            drawSwipeBar(c, dX, itemView, itemHeight)
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }

    private fun drawSwipeBar(c: Canvas, dX: Float, itemView: View, itemHeight: Int) {
        val swipeColor = -0x750000
        background.color = swipeColor
        val backgroundBounds = getBackgroundBounds(itemView, dX)
        background.bounds = backgroundBounds

        val itemBounds = getIconBounds(itemView, itemHeight)

        val alpha = getAlpha(itemView)

        swipeIcon.alpha = alpha
        background.setAlpha(alpha)
        swipeIcon.bounds = itemBounds
        background.draw(c)
        swipeIcon.draw(c)
    }

    private fun getBackgroundBounds(itemView: View, dX: Float): Rect {
        val bounds = Rect()
        bounds.top = itemView.top
        bounds.bottom = itemView.bottom
        bounds.left = (itemView.right + dX).toInt()
        bounds.right = itemView.right

        return bounds
    }

    private fun getIconBounds(itemView: View, itemHeight: Int): Rect {
        val bounds = Rect()
        bounds.top = itemView.top + (itemHeight - intrinsicHeight) / 2
        val itemMargin = (itemHeight - intrinsicHeight) / 2
        bounds.left = itemView.right - itemMargin - intrinsicWidth
        bounds.right = bounds.left + intrinsicWidth
        bounds.bottom = bounds.top + intrinsicHeight
        return bounds
    }

    private fun getAlpha(itemView: View): Int {
        var alpha = (((-itemView.translationX / itemView.width) * 200).toInt())
        if (alpha > 255) alpha = 255
        return alpha
    }

    fun interface SwipedCallback {
        fun onSwiped(viewHolder: RecyclerView.ViewHolder)
    }

    interface MovedCallback {
        fun onMoved(fromPosition: Int, toPosition: Int)

        fun onMoveCompleted(fromPosition: Int, toPosition: Int)
    }

    companion object {
        private const val DRAG_DIRS = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END
        private const val SWIPE_DIRS = ItemTouchHelper.LEFT

        fun createSwipeHelper(context: Context, swipedCallback: SwipedCallback, movedCallback: MovedCallback?): ItemTouchHelper {
            val swipeHelper: SwipeHelper = object : SwipeHelper(context, if (movedCallback != null) DRAG_DIRS else 0, android.R.drawable.ic_menu_delete) {
                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    return if (movedCallback != null) {
                        this.toPosition = target.getBindingAdapterPosition()
                        movedCallback.onMoved(viewHolder.getBindingAdapterPosition(), this.toPosition)
                        true
                    } else {
                        false
                    }
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    if (direction == SWIPE_DIRS) {
                        swipedCallback.onSwiped(viewHolder)
                    }
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                        fromPosition = viewHolder.getBindingAdapterPosition()
                    } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && fromPosition != -1) {
                        movedCallback?.onMoveCompleted(fromPosition, toPosition)
                        fromPosition = -1
                    }
                    super.onSelectedChanged(viewHolder, actionState)
                }
            }
            return ItemTouchHelper(swipeHelper)
        }
    }
}
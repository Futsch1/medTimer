package com.futsch1.medtimer;

import static android.graphics.PorterDuff.Mode.CLEAR;
import static androidx.recyclerview.widget.ItemTouchHelper.LEFT;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A simple and lightweight class that extends SimpleCallBack and can be used to attach a swipe listener to RecyclerView items.
 * Currently, it only supports left or right swipes with a LinearLayoutManager.
 * <p>
 * Example usage is given below:
 * RecyclerSwipeHelper mRecyclerSwipeHelper = new RecyclerSwipeHelper(swipeRightColor, swipeLeftColor, swipeRightIconResource, swipeLeftIconResource, context) {
 * &nbsp;
 * \@Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
 * mAdapter.notifyItemChanged(viewHolder.getBindingAdapterPosition());
 * //Handle left swipe
 * if (direction == ItemTouchHelper.LEFT)
 * //handle right swipe
 * else
 * }
 * };
 * ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(mRecyclerSwipeHelper);
 * mItemTouchHelper.attachToRecyclerView(mRecyclerView);
 * <p>
 * You must call RecyclerView.Adapter.notifyItemChanged() in the onSwiped method to reset the swiped item back to its original state.
 * <p>
 * Further, if you wish to prevent an item from being swiped on, then in your adapter's onBindViewHolder() method, use ViewHolder.ItemView.setTag() method and set the tag to @link @TAG_NO_SWIPE;
 */

public abstract class SwipeHelper extends SimpleCallback {

    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final int swipeLeftColor;

    private final Paint clearPaint;
    private final Drawable swipeLeftIcon;
    private final ColorDrawable background = new ColorDrawable();


    public SwipeHelper(@ColorInt int swipeLeftColor,
                       @DrawableRes int swipeLeftIconResource, Context context) {
        super(0, LEFT);

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(CLEAR));

        this.swipeLeftColor = swipeLeftColor;

        this.swipeLeftIcon = ContextCompat.getDrawable(context, swipeLeftIconResource);

        if (swipeLeftIcon == null)
            throw new Resources.NotFoundException("There was an error trying to load the drawables");

        intrinsicHeight = swipeLeftIcon.getIntrinsicHeight();
        intrinsicWidth = swipeLeftIcon.getIntrinsicWidth();
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        boolean isCanceled = (dX == 0f) && !isCurrentlyActive;

        if (isCanceled) {
            clearCanvas(c, itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false);
            return;
        }

        if (dX < 0) {
            background.setColor(swipeLeftColor);
            background.setBounds((int) (itemView.getRight() + dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());

            int itemTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int itemMargin = (itemHeight - intrinsicHeight) / 2;
            int itemLeft = itemView.getRight() - itemMargin - intrinsicWidth;
            int itemRight = itemView.getRight() - itemMargin;
            int itemBottom = itemTop + intrinsicHeight;

            int alpha = ((int) ((-itemView.getTranslationX() / itemView.getWidth()) * 300));
            if (alpha > 255) alpha = 255;

            swipeLeftIcon.setAlpha(alpha);
            background.setAlpha(alpha);
            swipeLeftIcon.setBounds(itemLeft, itemTop, itemRight, itemBottom);
            background.draw(c);
            swipeLeftIcon.draw(c);

        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        if (c != null) c.drawRect(left, top, right, bottom, clearPaint);
    }
}
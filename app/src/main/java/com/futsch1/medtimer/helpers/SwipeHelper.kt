package com.futsch1.medtimer.helpers;

import static android.graphics.PorterDuff.Mode.CLEAR;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback;
import androidx.recyclerview.widget.RecyclerView;


public abstract class SwipeHelper extends SimpleCallback {

    private static final int DRAG_DIRS = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END;
    private static final int SWIPE_DIRS = ItemTouchHelper.LEFT;

    private final int intrinsicWidth;
    private final int intrinsicHeight;

    private final Paint clearPaint;
    private final Drawable swipeIcon;
    private final ColorDrawable background = new ColorDrawable();
    int fromPosition = -1;
    int toPosition;

    protected SwipeHelper(Context context, int dragDirs, int icon) {
        super(dragDirs, SWIPE_DIRS);

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(CLEAR));

        this.swipeIcon = ContextCompat.getDrawable(context, icon);

        if (swipeIcon == null) {
            throw new Resources.NotFoundException("There was an error trying to load the drawables");
        }

        intrinsicHeight = swipeIcon.getIntrinsicHeight();
        intrinsicWidth = swipeIcon.getIntrinsicWidth();

        setDefaultSwipeDirs(SWIPE_DIRS);
    }

    public static ItemTouchHelper createSwipeHelper(Context context, SwipedCallback swipedCallback, MovedCallback movedCallback) {
        SwipeHelper swipeHelper = new SwipeHelper(context, movedCallback != null ? DRAG_DIRS : 0, android.R.drawable.ic_menu_delete) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (movedCallback != null) {
                    this.toPosition = target.getBindingAdapterPosition();
                    movedCallback.onMoved(viewHolder.getBindingAdapterPosition(), this.toPosition);
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == SWIPE_DIRS) {
                    swipedCallback.onSwiped(viewHolder);
                }
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    fromPosition = viewHolder.getBindingAdapterPosition();
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && fromPosition != -1) {
                    if (movedCallback != null) {
                        movedCallback.onMoveCompleted(fromPosition, toPosition);
                    }
                    fromPosition = -1;
                }
                super.onSelectedChanged(viewHolder, actionState);
            }
        };
        return new ItemTouchHelper(swipeHelper);
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
            drawSwipeBar(c, dX, itemView, itemHeight);
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        if (c != null) {
            c.drawRect(left, top, right, bottom, clearPaint);
        }
    }

    private void drawSwipeBar(@NonNull Canvas c, float dX, View itemView, int itemHeight) {
        int swipeColor = 0xFF8B0000;
        background.setColor(swipeColor);
        Rect backgroundBounds = getBackgroundBounds(itemView, dX);
        background.setBounds(backgroundBounds);

        Rect itemBounds = getIconBounds(itemView, itemHeight);

        int alpha = getAlpha(itemView);

        swipeIcon.setAlpha(alpha);
        background.setAlpha(alpha);
        swipeIcon.setBounds(itemBounds);
        background.draw(c);
        swipeIcon.draw(c);
    }

    private Rect getBackgroundBounds(View itemView, float dX) {
        Rect bounds = new Rect();
        bounds.top = itemView.getTop();
        bounds.bottom = itemView.getBottom();
        bounds.left = (int) (itemView.getRight() + dX);
        bounds.right = itemView.getRight();

        return bounds;
    }

    private Rect getIconBounds(View itemView, int itemHeight) {
        Rect bounds = new Rect();
        bounds.top = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
        int itemMargin = (itemHeight - intrinsicHeight) / 2;
        bounds.left = itemView.getRight() - itemMargin - intrinsicWidth;
        bounds.right = bounds.left + intrinsicWidth;
        bounds.bottom = bounds.top + intrinsicHeight;
        return bounds;
    }

    private int getAlpha(View itemView) {
        int alpha = ((int) (
                (-itemView.getTranslationX() / itemView.getWidth()) * 200));
        if (alpha > 255) alpha = 255;
        return alpha;
    }

    public interface SwipedCallback {
        void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder);
    }

    public interface MovedCallback {
        void onMoved(int fromPosition, int toPosition);

        void onMoveCompleted(int fromPosition, int toPosition);
    }
}
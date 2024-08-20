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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback;
import androidx.recyclerview.widget.RecyclerView;


public abstract class SwipeHelper extends SimpleCallback {

    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final int swipeColor;
    private final int swipeDirection;

    private final Paint clearPaint;
    private final Drawable swipeIcon;
    private final ColorDrawable background = new ColorDrawable();

    protected SwipeHelper(Context context, int direction, int color, int icon) {
        super(0, direction);

        swipeDirection = direction;

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(CLEAR));

        this.swipeColor = color;

        this.swipeIcon = ContextCompat.getDrawable(context, icon);

        if (swipeIcon == null) {
            throw new Resources.NotFoundException("There was an error trying to load the drawables");
        }

        intrinsicHeight = swipeIcon.getIntrinsicHeight();
        intrinsicWidth = swipeIcon.getIntrinsicWidth();

        setDefaultSwipeDirs(swipeDirection);
    }

    public static ItemTouchHelper createLeftSwipeTouchHelper(Context context, SwipedCallback callback) {
        SwipeHelper swipeHelper = new SwipeHelper(context, ItemTouchHelper.LEFT, 0xFF8B0000, android.R.drawable.ic_menu_delete) {
            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.LEFT) {
                    callback.onSwiped(viewHolder);
                }
            }
        };
        return new ItemTouchHelper(swipeHelper);
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

        if (isSwipeLeft() ? dX < 0 : dX > 0) {
            drawSwipeBar(c, dX, itemView, itemHeight);
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        if (c != null) {
            c.drawRect(left, top, right, bottom, clearPaint);
        }
    }

    private boolean isSwipeLeft() {
        return (swipeDirection & ItemTouchHelper.LEFT) == ItemTouchHelper.LEFT;
    }

    private void drawSwipeBar(@NonNull Canvas c, float dX, View itemView, int itemHeight) {
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
        bounds.left = (int) (isSwipeLeft() ? itemView.getRight() + dX : 0);
        bounds.right = (int) (isSwipeLeft() ? itemView.getRight() : dX);

        return bounds;
    }

    private Rect getIconBounds(View itemView, int itemHeight) {
        Rect bounds = new Rect();
        bounds.top = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
        int itemMargin = (itemHeight - intrinsicHeight) / 2;
        bounds.left = isSwipeLeft() ? itemView.getRight() - itemMargin - intrinsicWidth : itemView.getLeft() + itemMargin;
        bounds.right = bounds.left + intrinsicWidth;
        bounds.bottom = bounds.top + intrinsicHeight;
        return bounds;
    }

    private int getAlpha(View itemView) {
        int alpha = ((int) (isSwipeLeft() ?
                ((-itemView.getTranslationX() / itemView.getWidth()) * 200) :
                ((itemView.getTranslationX() / itemView.getWidth()) * 200)));
        if (alpha > 255) alpha = 255;
        return alpha;
    }

    public interface SwipedCallback {
        void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder);
    }
}
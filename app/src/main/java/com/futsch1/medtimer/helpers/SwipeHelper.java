package com.futsch1.medtimer.helpers;

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
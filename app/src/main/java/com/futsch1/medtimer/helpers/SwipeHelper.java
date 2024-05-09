package com.futsch1.medtimer.helpers;

import static android.graphics.PorterDuff.Mode.CLEAR;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
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

        if (swipeIcon == null)
            throw new Resources.NotFoundException("There was an error trying to load the drawables");

        intrinsicHeight = swipeIcon.getIntrinsicHeight();
        intrinsicWidth = swipeIcon.getIntrinsicWidth();
    }

    public void setup(@NonNull Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPref.getString("delete_items", "0").equals("0")) {
            setDefaultSwipeDirs(swipeDirection);
        } else {
            setDefaultSwipeDirs(0);
        }
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

        if (swipeDirection == ItemTouchHelper.LEFT ? dX < 0 : dX > 0) {
            background.setColor(swipeColor);
            int left = (int) (swipeDirection == ItemTouchHelper.LEFT ? itemView.getRight() + dX : 0);
            int right = (int) (swipeDirection == ItemTouchHelper.LEFT ? itemView.getRight() : dX);
            background.setBounds(left, itemView.getTop(), right, itemView.getBottom());

            int itemTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int itemMargin = (itemHeight - intrinsicHeight) / 2;
            int itemLeft = swipeDirection == ItemTouchHelper.LEFT ? itemView.getRight() - itemMargin - intrinsicWidth : itemView.getLeft() + itemMargin;
            int itemRight = swipeDirection == ItemTouchHelper.LEFT ? itemView.getRight() - itemMargin : itemView.getLeft() + itemMargin + intrinsicWidth;
            int itemBottom = itemTop + intrinsicHeight;

            int alpha = ((int) (swipeDirection == ItemTouchHelper.LEFT ? ((-itemView.getTranslationX() / itemView.getWidth()) * 300) :
                    ((itemView.getTranslationX() / itemView.getWidth()) * 300)));
            if (alpha > 255) alpha = 255;

            swipeIcon.setAlpha(alpha);
            background.setAlpha(alpha);
            swipeIcon.setBounds(itemLeft, itemTop, itemRight, itemBottom);
            background.draw(c);
            swipeIcon.draw(c);

        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        if (c != null) c.drawRect(left, top, right, bottom, clearPaint);
    }
}
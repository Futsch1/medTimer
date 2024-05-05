package com.futsch1.medtimer.statistics;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import com.google.android.material.color.MaterialColors;

public class ChartHelper {
    private final Context context;

    public ChartHelper(Context context) {
        this.context = context;
    }

    int getColor(int colorId) {
        return MaterialColors.getColor(context, colorId, "TakenSkippedChart");
    }

    float dpToPx(float dp) {
        Resources r = context.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                r.getDisplayMetrics()
        );
    }
}

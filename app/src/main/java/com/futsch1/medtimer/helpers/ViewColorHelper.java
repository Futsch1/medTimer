package com.futsch1.medtimer.helpers;

import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.elevation.SurfaceColors;

import java.util.List;

public class ViewColorHelper {

    private ViewColorHelper() {
        // Intentionally empty
    }

    public static void setCardBackground(MaterialCardView cardView, List<TextView> textViews, @ColorInt int backgroundColor) {
        int defaultTextViewColor = MaterialColors.getColor(cardView, com.google.android.material.R.attr.colorOnSurface);
        double contrastTextView = ColorUtils.calculateContrast(defaultTextViewColor, backgroundColor | 0xFF000000);
        int cardDefaultBackground = SurfaceColors.getColorForElevation(cardView.getContext(), cardView.getElevation());
        double contrastBackground = ColorUtils.calculateContrast(cardDefaultBackground, backgroundColor | 0xFF000000);

        setTextColor(textViews, contrastTextView < contrastBackground ? cardDefaultBackground : defaultTextViewColor);
        cardView.setCardBackgroundColor(backgroundColor);
    }

    private static void setTextColor(List<TextView> textViews, @ColorInt int color) {
        for (TextView textView : textViews) {
            textView.setTextColor(color);
        }
    }

    public static void setButtonBackground(Button button, @ColorInt int backgroundColor) {
        int primaryColor = MaterialColors.getColor(button, com.google.android.material.R.attr.colorOnSurface);
        int onPrimaryColor = MaterialColors.getColor(button, com.google.android.material.R.attr.colorOnPrimary);
        double primaryContrast = ColorUtils.calculateContrast(primaryColor, backgroundColor | 0xFF000000);
        double onPrimaryContrast = ColorUtils.calculateContrast(onPrimaryColor, backgroundColor | 0xFF000000);
        setTextColor(button, primaryContrast > onPrimaryContrast ? primaryColor : onPrimaryColor);
        button.setBackgroundColor(backgroundColor);
    }

    private static void setTextColor(Button button, @ColorInt int buttonTextColor) {
        button.setTextColor(buttonTextColor);
    }

    public static void setDefaultColors(MaterialCardView cardView, List<TextView> textViews) {
        int defaultTextViewColor = MaterialColors.getColor(cardView, com.google.android.material.R.attr.colorOnSurface);
        int cardDefaultBackground = SurfaceColors.getColorForElevation(cardView.getContext(), cardView.getElevation());

        cardView.setCardBackgroundColor(cardDefaultBackground);
        setTextColor(textViews, defaultTextViewColor);
    }
}

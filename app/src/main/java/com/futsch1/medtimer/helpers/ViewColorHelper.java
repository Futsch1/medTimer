package com.futsch1.medtimer.helpers;

import android.content.Context;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ViewColorHelper {

    private ViewColorHelper() {
        // Intentionally empty
    }

    public static void setCardBackground(MaterialCardView cardView, List<TextView> textViews, @ColorInt int backgroundColor) {
        int defaultTextViewColor = getThemeColor(cardView.getContext(), com.google.android.material.R.attr.colorOnSurface);
        double contrastTextView = ColorUtils.calculateContrast(defaultTextViewColor, backgroundColor | 0xFF000000);
        int cardDefaultBackground = getThemeColor(cardView.getContext(), com.google.android.material.R.attr.colorSurface);
        double contrastBackground = ColorUtils.calculateContrast(cardDefaultBackground, backgroundColor | 0xFF000000);
        setTextColor(textViews, contrastTextView < contrastBackground ? cardDefaultBackground : defaultTextViewColor);
        cardView.setCardBackgroundColor(backgroundColor);
    }

    private static int getThemeColor(Context context, int attribute) {
        TypedValue themeColor = new TypedValue();
        context.getTheme().resolveAttribute(attribute, themeColor, true);
        return themeColor.data;
    }

    private static void setTextColor(List<TextView> textViews, @ColorInt int color) {
        for (TextView textView : textViews) {
            textView.setTextColor(color);
        }
    }

    public static void setButtonBackground(Button button, @ColorInt int backgroundColor) {
        int primaryColor = getThemeColor(button.getContext(), com.google.android.material.R.attr.colorPrimary);
        int onPrimaryColor = getThemeColor(button.getContext(), com.google.android.material.R.attr.colorOnPrimary);
        double primaryContrast = ColorUtils.calculateContrast(primaryColor, backgroundColor | 0xFF000000);
        double onPrimaryContrast = ColorUtils.calculateContrast(onPrimaryColor, backgroundColor | 0xFF000000);
        setTextColor(button, primaryContrast > onPrimaryContrast ? primaryColor : onPrimaryColor);
        button.setBackgroundColor(backgroundColor);
    }

    private static void setTextColor(Button button, @ColorInt int buttonTextColor) {
        button.setTextColor(buttonTextColor);
    }

    public static void setDefaultColors(MaterialCardView cardView, List<TextView> textViews) {
        int defaultTextViewColor = getThemeColor(cardView.getContext(), com.google.android.material.R.attr.colorOnSurfaceVariant);
        int cardDefaultBackground = getThemeColor(cardView.getContext(), com.google.android.material.R.attr.colorSurface);
        cardView.setCardBackgroundColor(cardDefaultBackground);
        setTextColor(textViews, defaultTextViewColor);
    }
}

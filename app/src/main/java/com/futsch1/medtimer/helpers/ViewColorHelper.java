package com.futsch1.medtimer.helpers;

import android.content.Context;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.card.MaterialCardView;

public class ViewColorHelper {
    public static void setCardBackground(MaterialCardView cardView, TextView[] textViews, int backgroundColor) {
        int defaultTextViewColor = getThemeColor(cardView.getContext(), com.google.android.material.R.attr.colorOnSurfaceVariant);
        double contrastTextView = ColorUtils.calculateContrast(defaultTextViewColor, backgroundColor);
        int cardDefaultBackground = getThemeColor(cardView.getContext(), com.google.android.material.R.attr.colorSurface);
        double contrastBackground = ColorUtils.calculateContrast(cardDefaultBackground, backgroundColor);
        setTextColor(textViews, contrastTextView < contrastBackground ? cardDefaultBackground : defaultTextViewColor);
        cardView.setCardBackgroundColor(backgroundColor);
    }

    private static int getThemeColor(Context context, int attribute) {
        TypedValue themeColor = new TypedValue();
        context.getTheme().resolveAttribute(attribute, themeColor, true);
        return themeColor.data;
    }

    private static void setTextColor(TextView[] textViews, int color) {
        for (TextView textView : textViews) {
            textView.setTextColor(color);
        }
    }

    public static void setButtonBackground(Button button, int backgroundColor) {
        int primaryColor = getThemeColor(button.getContext(), com.google.android.material.R.attr.colorPrimary);
        int onPrimaryColor = getThemeColor(button.getContext(), com.google.android.material.R.attr.colorOnPrimary);
        double primaryContrast = ColorUtils.calculateContrast(primaryColor, backgroundColor);
        double onPrimaryContrast = ColorUtils.calculateContrast(onPrimaryColor, backgroundColor);
        setTextColor(button, primaryContrast > onPrimaryContrast ? primaryColor : onPrimaryColor);
        button.setBackgroundColor(backgroundColor);
    }

    private static void setTextColor(Button button, int buttonTextColor) {
        button.setTextColor(buttonTextColor);
    }

    public static void setDefaultColors(MaterialCardView cardView, TextView[] textViews) {
        int defaultTextViewColor = getThemeColor(cardView.getContext(), com.google.android.material.R.attr.colorOnSurfaceVariant);
        int cardDefaultBackground = getThemeColor(cardView.getContext(), com.google.android.material.R.attr.colorSurface);
        cardView.setCardBackgroundColor(cardDefaultBackground);
        setTextColor(textViews, defaultTextViewColor);
    }
}

package com.futsch1.medtimer.helpers;

import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.elevation.SurfaceColors;

import java.util.List;

public class ViewColorHelper {

    private ViewColorHelper() {
        // Intentionally empty
    }

    public static void setViewBackground(View view, List<TextView> textViews, @ColorInt int backgroundColor) {
        int defaultTextViewColor = getColorOnSurface(view);
        double contrastTextView = ColorUtils.calculateContrast(defaultTextViewColor, backgroundColor | 0xFF000000);
        int cardDefaultBackground = SurfaceColors.getColorForElevation(view.getContext(), view.getElevation());
        double contrastBackground = ColorUtils.calculateContrast(cardDefaultBackground, backgroundColor | 0xFF000000);

        setTextColor(textViews, contrastTextView < contrastBackground ? cardDefaultBackground : defaultTextViewColor);
        if (view instanceof MaterialCardView materialCardView) {
            materialCardView.setCardBackgroundColor(backgroundColor);
        } else if (view.getBackground() instanceof GradientDrawable shapeDrawable) {
            shapeDrawable.setColor(backgroundColor);
        } else {
            view.setBackgroundColor(backgroundColor);
        }
    }

    private static int getColorOnSurface(View cardView) {
        return MaterialColors.getColor(cardView, com.google.android.material.R.attr.colorOnSurface);
    }

    private static void setTextColor(List<TextView> textViews, @ColorInt int color) {
        for (TextView textView : textViews) {
            textView.setTextColor(color);
        }
    }

    public static void setIconToImageView(View view, ImageView imageView, int iconId) {
        if (iconId != 0) {
            Drawable iconDrawable = new MedicineIcons(view.getContext()).getIconDrawable(iconId);
            setDrawableTint(view, iconDrawable);
            imageView.setImageDrawable(iconDrawable);
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    public static void setDrawableTint(View view, Drawable drawable) {
        int backgroundColor = getBackground(view);
        DrawableCompat.setTint(drawable, getColorOnView(view, backgroundColor));
    }

    private static int getBackground(View view) {
        int defaultColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface);
        if (view instanceof MaterialCardView materialCardView) {
            return materialCardView.getCardBackgroundColor().getDefaultColor();
        } else if (view.getBackground() instanceof ColorDrawable colorDrawable) {
            return colorDrawable.getColor();
        } else if (view.getBackground() instanceof GradientDrawable shapeDrawable) {
            ColorStateList colorStateList = shapeDrawable.getColor();
            return colorStateList != null ? colorStateList.getDefaultColor() : defaultColor;
        } else {
            return defaultColor;
        }
    }

    private static int getColorOnView(View view, @ColorInt int backgroundColor) {
        int primaryColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface);
        int onPrimaryColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnPrimary);
        double primaryContrast = ColorUtils.calculateContrast(primaryColor, backgroundColor | 0xFF000000);
        double onPrimaryContrast = ColorUtils.calculateContrast(onPrimaryColor, backgroundColor | 0xFF000000);
        return primaryContrast > onPrimaryContrast ? primaryColor : onPrimaryColor;
    }

    public static void setButtonBackground(Button button, @ColorInt int backgroundColor) {
        setTextColor(button, getColorOnView(button, backgroundColor));
        button.setBackgroundColor(backgroundColor);
    }

    private static void setTextColor(Button button, @ColorInt int buttonTextColor) {
        button.setTextColor(buttonTextColor);
    }

    public static void setDefaultColors(View view, List<TextView> textViews) {
        int defaultTextViewColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface);

        setTextColor(textViews, defaultTextViewColor);
        if (view instanceof MaterialCardView materialCardView) {
            int cardDefaultBackground = SurfaceColors.getColorForElevation(view.getContext(), view.getElevation());
            materialCardView.setCardBackgroundColor(cardDefaultBackground);
        } else if (view.getBackground() instanceof GradientDrawable shapeDrawable) {
            int defaultBackground = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface);
            shapeDrawable.setColor(defaultBackground);
        } else {
            int defaultBackground = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface);
            view.setBackgroundColor(defaultBackground);
        }
    }
}

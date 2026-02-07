package com.futsch1.medtimer.helpers

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.elevation.SurfaceColors

object ViewColorHelper {
    fun setViewBackground(view: View, textViews: List<TextView>, @ColorInt backgroundColor: Int) {
        val defaultTextViewColor = getColorOnSurface(view)
        val contrastTextView = ColorUtils.calculateContrast(defaultTextViewColor, backgroundColor or -0x1000000)
        val cardDefaultBackground = SurfaceColors.getColorForElevation(view.context, view.elevation)
        val contrastBackground = ColorUtils.calculateContrast(cardDefaultBackground, backgroundColor or -0x1000000)

        setTextColor(textViews, if (contrastTextView < contrastBackground) cardDefaultBackground else defaultTextViewColor)
        if (view is MaterialCardView) {
            view.setCardBackgroundColor(backgroundColor)
        } else if (view.background is GradientDrawable) {
            (view.background as GradientDrawable).setColor(backgroundColor)
        } else {
            view.setBackgroundColor(backgroundColor)
        }
    }

    private fun getColorOnSurface(cardView: View): Int {
        return MaterialColors.getColor(cardView, R.attr.colorOnSurface)
    }

    private fun setTextColor(textViews: List<TextView>, @ColorInt color: Int) {
        for (textView in textViews) {
            textView.setTextColor(color)
        }
    }

    fun setIconToImageView(view: View, imageView: ImageView, iconId: Int) {
        if (iconId != 0) {
            val iconDrawable = MedicineIcons(view.context).getIconDrawable(iconId)
            setDrawableTint(view, iconDrawable)
            imageView.setImageDrawable(iconDrawable)
            imageView.setVisibility(View.VISIBLE)
        } else {
            imageView.setVisibility(View.GONE)
        }
    }

    fun setDrawableTint(view: View, drawable: Drawable) {
        val backgroundColor = getBackground(view)
        DrawableCompat.setTint(drawable, getColorOnView(view, backgroundColor))
    }

    private fun getBackground(view: View): Int {
        val defaultColor = MaterialColors.getColor(view, R.attr.colorSurface)
        return when {
            view is MaterialCardView -> {
                view.cardBackgroundColor.defaultColor
            }

            view.background is ColorDrawable -> {
                (view.background as ColorDrawable).color
            }

            view.background is GradientDrawable -> {
                val colorStateList: ColorStateList? = (view.background as GradientDrawable).color
                colorStateList?.defaultColor ?: defaultColor
            }

            else -> {
                defaultColor
            }
        }
    }

    private fun getColorOnView(view: View, @ColorInt backgroundColor: Int): Int {
        val primaryColor = MaterialColors.getColor(view, R.attr.colorOnSurface)
        val onPrimaryColor = MaterialColors.getColor(view, R.attr.colorOnPrimary)
        val primaryContrast = ColorUtils.calculateContrast(primaryColor, backgroundColor or -0x1000000)
        val onPrimaryContrast = ColorUtils.calculateContrast(onPrimaryColor, backgroundColor or -0x1000000)
        return if (primaryContrast > onPrimaryContrast) primaryColor else onPrimaryColor
    }

    fun setButtonBackground(button: Button, @ColorInt backgroundColor: Int) {
        setTextColor(button, getColorOnView(button, backgroundColor))
        button.setBackgroundColor(backgroundColor)
    }

    private fun setTextColor(button: Button, @ColorInt buttonTextColor: Int) {
        button.setTextColor(buttonTextColor)
    }

    fun setDefaultColors(view: View, textViews: List<TextView>) {
        val defaultTextViewColor = MaterialColors.getColor(view, R.attr.colorOnSurface)

        setTextColor(textViews, defaultTextViewColor)
        if (view is MaterialCardView) {
            val cardDefaultBackground = SurfaceColors.getColorForElevation(view.context, view.elevation)
            view.setCardBackgroundColor(cardDefaultBackground)
        } else if (view.background is GradientDrawable) {
            val defaultBackground = MaterialColors.getColor(view, R.attr.colorSurface)
            (view.background as GradientDrawable).setColor(defaultBackground)
        } else {
            val defaultBackground = MaterialColors.getColor(view, R.attr.colorSurface)
            view.setBackgroundColor(defaultBackground)
        }
    }
}

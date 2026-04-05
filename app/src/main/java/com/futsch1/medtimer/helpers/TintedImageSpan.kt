package com.futsch1.medtimer.helpers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.withSave

class TintedImageSpan(
    private val drawable: Drawable,
    verticalAlignment: Int = ALIGN_BASELINE
) : DynamicDrawableSpan(verticalAlignment) {

    init {
        // Ensure bounds are set, otherwise it won't draw
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    }

    override fun getDrawable(): Drawable = drawable

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        drawable.mutate()
        // Dynamically grab the color from the current Paint object
        DrawableCompat.setTint(drawable, paint.color)

        canvas.withSave {
            val transY = when (verticalAlignment) {
                ALIGN_BASELINE -> y - drawable.bounds.bottom
                ALIGN_BOTTOM -> bottom - drawable.bounds.bottom
                else -> y - drawable.bounds.bottom
            }
            translate(x, transY.toFloat())
            drawable.draw(this)
        }
    }
}

fun addImageToSpan(imageResourceId: Int, builder: SpannableStringBuilder, context: Context) {
    val drawable = ContextCompat.getDrawable(context, imageResourceId) ?: return
    val imageSpan = TintedImageSpan(drawable, DynamicDrawableSpan.ALIGN_BASELINE)
    val len = builder.length
    builder.append("  ")
    builder.setSpan(imageSpan, len, len + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
}

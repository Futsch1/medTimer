package com.futsch1.medtimer.helpers

import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.LineBackgroundSpan

class DividerSpan(
    private val thickness: Float,
    private val padding: Float
) : LineBackgroundSpan {

    override fun drawBackground(
        canvas: Canvas, paint: Paint,
        left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int, lineNumber: Int
    ) {
        // We draw the line centered vertically between 'top' and 'bottom'
        val centerY = (top + bottom) / 2f
        canvas.drawRect(
            left.toFloat() + padding,
            centerY - (thickness / 2),
            right.toFloat() - padding,
            centerY + (thickness / 2),
            paint
        )
    }
}

fun addDividerToSpan(builder: SpannableStringBuilder) {
    val dividerSpan = DividerSpan(
        1f, 2f
    )
    val len = builder.length
    builder.append("\n")
    builder.setSpan(dividerSpan, len, len + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
}
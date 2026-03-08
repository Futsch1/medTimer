package com.futsch1.medtimer.helpers

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.google.android.material.color.MaterialColors

fun Context.getMaterialColor(@AttrRes attrId: Int, errorMessageComponent: String?): Int =
    MaterialColors.getColor(this, attrId, errorMessageComponent)

fun Context.getMaterialColor(@AttrRes attrId: Int, @ColorInt defaultValue: Int): Int =
    MaterialColors.getColor(this, attrId, defaultValue)

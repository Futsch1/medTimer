package com.futsch1.medtimer.helpers

import android.content.Context
import com.futsch1.medtimer.statistics.TakenSkippedChart
import com.google.android.material.color.MaterialColors

fun Context.getMaterialColor(attrId: Int): Int =
    MaterialColors.getColor(this, attrId, TakenSkippedChart::class.simpleName)

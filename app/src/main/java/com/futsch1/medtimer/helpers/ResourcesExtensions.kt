package com.futsch1.medtimer.helpers

import android.content.res.Resources
import android.util.TypedValue

fun Resources.dpToPx(dp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics)

package com.futsch1.medtimer.overview

import android.content.Context
import com.futsch1.medtimer.R

enum class OverviewState {
    PENDING,
    RAISED,
    TAKEN,
    SKIPPED
}

fun OverviewState.toString(context: Context): String {
    return when (this) {
        OverviewState.PENDING -> context.getString(R.string.please_wait)
        OverviewState.TAKEN -> context.getString(R.string.taken)
        OverviewState.SKIPPED -> context.getString(R.string.skipped)
        OverviewState.RAISED -> context.getString(R.string.reminded)
    }
}

fun OverviewState.getImage(): Int {
    return when (this) {
        OverviewState.PENDING -> R.drawable.alarm
        OverviewState.TAKEN -> R.drawable.check2_circle
        OverviewState.SKIPPED -> R.drawable.x_circle
        OverviewState.RAISED -> R.drawable.bell
    }
}
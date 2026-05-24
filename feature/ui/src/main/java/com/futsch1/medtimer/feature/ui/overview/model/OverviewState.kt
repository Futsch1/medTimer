package com.futsch1.medtimer.feature.ui.overview.model

import android.content.Context
import com.futsch1.medtimer.feature.ui.R

enum class OverviewState {
    PENDING,
    RAISED,
    TAKEN,
    SKIPPED,
    LOCATION
}

fun OverviewState.toString(context: Context): String {
    return when (this) {
        OverviewState.PENDING -> context.getString(com.futsch1.medtimer.core.ui.R.string.please_wait)
        OverviewState.TAKEN -> context.getString(com.futsch1.medtimer.core.ui.R.string.taken)
        OverviewState.SKIPPED -> context.getString(com.futsch1.medtimer.core.ui.R.string.skipped)
        OverviewState.RAISED -> context.getString(com.futsch1.medtimer.core.ui.R.string.reminded)
        OverviewState.LOCATION -> context.getString(com.futsch1.medtimer.core.ui.R.string.snooze_until_home)
    }
}

fun OverviewState.getImage(): Int {
    return when (this) {
        OverviewState.PENDING -> com.futsch1.medtimer.core.ui.R.drawable.alarm
        OverviewState.TAKEN -> com.futsch1.medtimer.core.ui.R.drawable.check2_circle
        OverviewState.SKIPPED -> com.futsch1.medtimer.core.ui.R.drawable.x_circle
        OverviewState.RAISED -> com.futsch1.medtimer.core.ui.R.drawable.bell
        OverviewState.LOCATION -> com.futsch1.medtimer.core.ui.R.drawable.geo_alt_fill
    }
}
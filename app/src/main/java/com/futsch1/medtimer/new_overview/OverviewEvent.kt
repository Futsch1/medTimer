package com.futsch1.medtimer.new_overview

import android.text.Spanned


enum class OverviewState {
    PENDING,
    RAISED,
    TAKEN,
    SKIPPED
}

data class OverviewEvent(val id: Int, val timestamp: Long, val text: Spanned, val icon: Int, val color: Int?, val state: OverviewState)

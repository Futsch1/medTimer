package com.futsch1.medtimer.core.ui

/**
 * Anchors for the containers instrumented tests scope their selectors to.
 *
 * Selection is by accessible name (content description, label, visible text) wherever the UI already
 * carries one; these tags exist for the structure those names are looked up inside, so a query
 * cannot match a same-named node on another screen or in another list.
 */
object ScreenTestTags {
    const val OVERVIEW = "screen_overview"
    const val MEDICINES = "screen_medicines"
    const val STATISTICS = "screen_statistics"

    /** Every screen's [com.futsch1.medtimer.core.ui.component.MedTimerTopAppBar]. */
    const val TOP_APP_BAR = "screen_top_app_bar"
}

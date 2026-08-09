package com.futsch1.medtimer.robots

import androidx.compose.ui.test.assertIsSelected
import com.futsch1.medtimer.NavTestTags

/**
 * The bottom navigation. Tapping Overview while it is already shown resets its day to today.
 *
 * The tabs are the one place still selected by tag: NavigationSuiteScaffold exposes no modifier for
 * the bar itself, so there is no container to scope their names to.
 */
class NavigationRobot(private val ui: ComposeUi) {

    fun toOverview() = ui.scope(NavTestTags.OVERVIEW).clickSelf()

    fun toMedicines() = ui.scope(NavTestTags.MEDICINES).clickSelf()

    fun toAnalysis() = ui.scope(NavTestTags.STATISTICS).clickSelf()

    /** The Overview tab being selected is what distinguishes it from any other screen. */
    fun assertOverviewShown() {
        ui.scope(NavTestTags.OVERVIEW).self().assertIsSelected()
    }
}

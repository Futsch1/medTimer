package com.futsch1.medtimer.robots

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import com.futsch1.medtimer.NavTestTags

/**
 * The bottom navigation. Tapping the tab already shown does nothing; leaving and re-entering a tab
 * resets it, which is why tests navigate away and back to get the Overview onto today again.
 */
class NavigationRobot(private val ui: ComposeUi) {

    fun toOverview() = ui.clickTag(NavTestTags.OVERVIEW)

    fun toMedicines() = ui.clickTag(NavTestTags.MEDICINES)

    fun toAnalysis() = ui.clickTag(NavTestTags.STATISTICS)

    /** The Overview tab being selected is what distinguishes it from any other screen. */
    fun assertOverviewShown() {
        ui.rule.onNodeWithTag(NavTestTags.OVERVIEW).assertIsSelected()
    }
}

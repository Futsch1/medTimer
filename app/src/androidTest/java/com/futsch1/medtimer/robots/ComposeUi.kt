package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onIdle
import androidx.test.platform.app.InstrumentationRegistry

/** App-wide Compose interactions: tags, the arc action menu, and content descriptions. */
class ComposeUi(val rule: ComposeTestRule) {

    val queries = SemanticsQueries(rule)

    fun getString(@StringRes textRes: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(textRes)

    fun clickTag(tag: String) {
        rule.onNodeWithTag(tag).performClick()
        settle()
    }

    fun clickMenuItem(@StringRes textRes: Int) {
        val text = getString(textRes)
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(text).performClick()
        settle()
    }

    fun clickContentDescription(@StringRes textRes: Int) {
        val description = getString(textRes)
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) {
            rule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription(description).performClick()
        settle()
    }

    fun assertMenuItemDisplayed(@StringRes textRes: Int) {
        rule.onNodeWithText(getString(textRes)).assertIsDisplayed()
    }

    fun assertMenuItemNotDisplayed(@StringRes textRes: Int) {
        rule.onNodeWithText(getString(textRes)).assertDoesNotExist()
    }

    /**
     * Registered Espresso IdlingResources (e.g., async generateTestData) need an explicit wait since
     * later steps often drive UiAutomator directly, bypassing Espresso's own idle check.
     */
    fun settle() {
        rule.waitForIdle()
        onIdle()
    }
}

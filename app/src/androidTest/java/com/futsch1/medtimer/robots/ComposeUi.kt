package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso.onIdle
import androidx.test.platform.app.InstrumentationRegistry

/**
 * The way into the Compose semantics tree.
 * It hands out [UiScope]s and nothing else:
 * there is no entry point that searches the whole tree,
 * so every selector is anchored to a container by construction rather than by convention.
 */
class ComposeUi(private val rule: ComposeTestRule) {

    /** Selectors name things the way the UI does, so the strings have to come from the app. */
    fun getString(@StringRes textRes: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(textRes)

    fun scope(tag: String): UiScope = UiScope(rule, hasTestTag(tag))

    fun settle() {
        rule.waitForIdle()
        onIdle()
    }
}

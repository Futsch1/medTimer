package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.futsch1.medtimer.core.ui.R as CoreUiR

/** The onboarding carousel, re-openable from the app menu. */
class AppIntroRobot(private val ui: ComposeUi, private val menus: MenuRobot) {

    fun show() = menus.clickAppOption(CoreUiR.string.show_intro)

    fun assertPage(@StringRes titleRes: Int, @StringRes descriptionRes: Int) {
        assertDisplayed(com.github.appintro.R.id.title, ui.getString(titleRes))
        assertDisplayed(com.github.appintro.R.id.description, ui.getString(descriptionRes))
    }

    fun skip() = clickOn(com.github.appintro.R.id.skip)
}

package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import com.futsch1.medtimer.core.ui.ScreenTestTags
import com.futsch1.medtimer.feature.ui.AppOptionsTestTags
import com.futsch1.medtimer.feature.ui.medicine.EditMedicineTestTags
import com.futsch1.medtimer.feature.ui.medicine.MedicinesMenuTestTags
import com.futsch1.medtimer.core.ui.R as CoreUiR

/**
 * The three overflow menus hanging off the top bar.
 * Each opens by the accessible name of its button and scopes its entries to its own popup,
 * so the two "Export as ..." pairs and the labels shared with the edit-medicine menu cannot be confused for one another.
 */
class MenuRobot(private val ui: ComposeUi) {

    private val topBar get() = ui.scope(ScreenTestTags.TOP_APP_BAR)

    fun clickAppOption(@StringRes labelRes: Int) = openAppOptions().click(label(labelRes))

    fun clickAppOptionNamed(@StringRes descriptionRes: Int) =
        openAppOptions().click(hasContentDescription(ui.getString(descriptionRes)))

    fun clickMedicinesOption(@StringRes labelRes: Int) =
        open(CoreUiR.string.tab_medicine, MedicinesMenuTestTags.MENU).click(label(labelRes))

    fun clickEditMedicineOption(@StringRes labelRes: Int) =
        open(CoreUiR.string.more_options, EditMedicineTestTags.MENU).click(label(labelRes))

    fun openTagFilter() = topBar.click(hasContentDescription(ui.getString(CoreUiR.string.filter)))

    private fun openAppOptions() = open(CoreUiR.string.more_options, AppOptionsTestTags.MENU)

    private fun open(@StringRes descriptionRes: Int, menuTag: String): UiScope {
        topBar.click(hasContentDescription(ui.getString(descriptionRes)))
        return ui.scope(menuTag)
    }

    private fun label(@StringRes labelRes: Int) = hasText(ui.getString(labelRes))
}

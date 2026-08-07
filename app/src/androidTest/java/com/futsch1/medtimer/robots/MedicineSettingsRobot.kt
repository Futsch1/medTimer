package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.futsch1.medtimer.core.ui.R as CoreUiR

/** The per-medicine settings screen: colour, icon, notification importance, skip behaviour. */
class MedicineSettingsRobot(
    private val menus: MenuRobot,
    private val preferences: PreferenceScreenRobot,
    private val pickers: IconColorPickerRobot,
) {

    /** Opens the medicine's settings, runs [block] and returns to the editor. */
    fun inSettings(block: MedicineSettingsRobot.() -> Unit) {
        open()
        block()
        pressBack()
    }

    /** Selecting the icon closes the settings screen, so this ends on the editor. */
    fun setColorAndIcon(hex: String, iconPosition: Int) {
        open()
        preferences.click(CoreUiR.string.color)
        preferences.click(CoreUiR.string.select_color)
        pickers.enterHex(hex)
        pressBack()
        pickers.selectIcon(iconPosition)
    }

    fun setNotificationImportance(@StringRes importanceRes: Int) {
        clickOn(CoreUiR.string.notification_importance)
        clickOn(importanceRes)
    }

    fun toggleCannotBeSkipped() = clickOn(CoreUiR.string.medicine_cannot_be_skipped)

    private fun open() = menus.clickEditMedicineOption(CoreUiR.string.medicine_settings)
}

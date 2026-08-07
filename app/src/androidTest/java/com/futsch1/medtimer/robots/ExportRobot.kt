package com.futsch1.medtimer.robots

import androidx.annotation.StringRes

/** Exporting from the app menu, which always ends in the system share sheet. */
class ExportRobot(private val menus: MenuRobot, private val shareSheet: ShareSheetRobot) {

    /** [descriptionRes] is the export entry's spoken name, which says both what and in which format. */
    fun export(@StringRes descriptionRes: Int) {
        menus.clickAppOptionNamed(descriptionRes)
        shareSheet.assertShownAndDismiss()
    }
}

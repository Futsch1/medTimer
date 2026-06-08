package com.futsch1.medtimer.feature.ui.overview.actions

import android.view.Menu

class ActionsMenu(menu: Menu, val actions: Actions) {

    init {
        for (button in Button.entries) {
            val menuItem = menu.findItem(button.associatedId)

            menuItem.isVisible = actions.visibleButtons.contains(button)
        }
    }
}
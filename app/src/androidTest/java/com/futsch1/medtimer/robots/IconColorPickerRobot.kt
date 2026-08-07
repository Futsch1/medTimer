package com.futsch1.medtimer.robots

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn

/**
 * The third-party colour and icon pickers. Both expose neither ids the app owns nor accessible names,
 * so they are matched by resource name - the suite's most brittle selector, kept to this one file.
 */
class IconColorPickerRobot {

    fun enterHex(hex: String) {
        onView(withResourceName("hexEdit")).perform(ViewActions.clearText(), ViewActions.typeText(hex))
        closeSoftKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.confirmSelectColor)
    }

    fun selectIcon(position: Int) {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.selectIcon)
        onView(withResourceName("icd_rcv_icon_list")).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, ViewActions.click())
        )
    }
}

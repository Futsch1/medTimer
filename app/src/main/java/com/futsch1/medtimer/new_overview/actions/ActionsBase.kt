package com.futsch1.medtimer.new_overview.actions

import android.view.View
import android.widget.PopupWindow
import com.futsch1.medtimer.R
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

open class ActionsBase(view: View, popupWindow: PopupWindow) {
    val takenButton: ExtendedFloatingActionButton = view.findViewById(R.id.takenButton)
    val skippedButton: ExtendedFloatingActionButton = view.findViewById(R.id.skippedButton)
    val reRaiseButton: ExtendedFloatingActionButton = view.findViewById(R.id.reraiseButton)
    val deleteButton: ExtendedFloatingActionButton = view.findViewById(R.id.deleteButton)

    init {
        view.setOnClickListener {
            popupWindow.dismiss()
        }
    }
}
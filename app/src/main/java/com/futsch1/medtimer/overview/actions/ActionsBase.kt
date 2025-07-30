package com.futsch1.medtimer.overview.actions

import android.view.View
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import com.futsch1.medtimer.R
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton


open class ActionsBase(view: View, popupWindow: PopupWindow) {
    val takenButton: ExtendedFloatingActionButton = view.findViewById(R.id.takenButton)
    val skippedButton: ExtendedFloatingActionButton = view.findViewById(R.id.skippedButton)
    val reRaiseButton: ExtendedFloatingActionButton = view.findViewById(R.id.reraiseButton)
    val deleteButton: ExtendedFloatingActionButton = view.findViewById(R.id.deleteButton)
    val anchorTakenButton: View = view.findViewById(R.id.anchorTakenButton)
    val anchorSkippedButton: View = view.findViewById(R.id.anchorSkippedButton)

    init {
        view.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    fun hideDeleteAndReraise() {
        deleteButton.visibility = View.INVISIBLE
        reRaiseButton.visibility = View.INVISIBLE

        setAngle(anchorTakenButton, 70f)
        setAngle(anchorSkippedButton, 110f)
    }

    private fun setAngle(view: View, f: Float) {
        val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.circleAngle = f
        view.setLayoutParams(layoutParams)
    }
}
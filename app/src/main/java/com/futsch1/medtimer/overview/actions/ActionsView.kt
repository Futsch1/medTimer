package com.futsch1.medtimer.overview.actions

import android.view.View
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.futsch1.medtimer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ActionsView(
    val view: View,
    popupWindow: PopupWindow?,
    coroutineScope: CoroutineScope,
    actions: ActionsBase
) {
    val takenButton: View = view.findViewById(R.id.takenButton)
    val acknowledgedButton: View = view.findViewById(R.id.acknowledgedButton)
    val skippedButton: View = view.findViewById(R.id.skippedButton)
    val reRaiseButton: View = view.findViewById(R.id.reraiseButton)
    val rescheduleButton: View = view.findViewById(R.id.rescheduleButton)
    val deleteButton: View = view.findViewById(R.id.deleteButton)
    val anchorTakenButton: View? = view.findViewById(R.id.anchorTakenButton)
    val anchorAcknowledgedButton: View? = view.findViewById(R.id.anchorAcknowledgedButton)
    val anchorSkippedButton: View? = view.findViewById(R.id.anchorSkippedButton)
    val anchorReraiseButton: View? = view.findViewById(R.id.anchorReraiseButton)
    val anchorRescheduleButton: View? = view.findViewById(R.id.anchorRescheduleButton)
    val anchorDeleteButton: View? = view.findViewById(R.id.anchorDeleteButton)

    val mapButtonToView = mapOf(
        ActionsBase.Button.TAKEN to takenButton,
        ActionsBase.Button.ACKNOWLEDGED to acknowledgedButton,
        ActionsBase.Button.SKIPPED to skippedButton,
        ActionsBase.Button.RERAISE to reRaiseButton,
        ActionsBase.Button.RESCHEDULE to rescheduleButton,
        ActionsBase.Button.DELETE to deleteButton
    )
    val mapButtonToAnchor = mapOf(
        ActionsBase.Button.TAKEN to anchorTakenButton,
        ActionsBase.Button.ACKNOWLEDGED to anchorAcknowledgedButton,
        ActionsBase.Button.SKIPPED to anchorSkippedButton,
        ActionsBase.Button.RERAISE to anchorReraiseButton,
        ActionsBase.Button.RESCHEDULE to anchorRescheduleButton,
        ActionsBase.Button.DELETE to anchorDeleteButton
    )


    val visible: Boolean
        get() = ActionsBase.Button.entries.any { mapButtonToView[it]?.isVisible == true }


    init {
        view.setOnClickListener {
            popupWindow?.dismiss()
        }

        // Depending on the number of visible buttons, set the angles of the anchors
        // 4 buttons -> 30, 70, 110, 150
        // 3 buttons -> 50, 90, 130
        // 2 buttons -> 70, 110
        // 1 button -> 90
        val visibleButtons = actions.visibleButtons
        val angleLists = mapOf(
            4 to listOf(30f, 70f, 110f, 150f),
            3 to listOf(50f, 90f, 130f),
            2 to listOf(70f, 110f),
            1 to listOf(90f)
        )
        val angles = angleLists[visibleButtons.size]
        var anglesIndex = 0
        for (button in ActionsBase.Button.entries) {
            mapButtonToView[button]?.setOnClickListener {
                coroutineScope.launch {
                    actions.buttonClicked(button)
                }
                popupWindow?.dismiss()
            }
            val visibility = if (visibleButtons.contains(button)) View.VISIBLE else View.INVISIBLE
            mapButtonToView[button]?.visibility = visibility
            if (visibility == View.VISIBLE) {
                setAngle(mapButtonToAnchor[button], angles!![anglesIndex++])
            }
        }
    }

    fun setAngle(view: View?, f: Float) {
        if (view == null) {
            return
        }
        val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.circleAngle = f
        view.setLayoutParams(layoutParams)
    }

}
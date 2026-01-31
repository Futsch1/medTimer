package com.futsch1.medtimer.overview.actions

import android.view.View
import android.widget.PopupWindow
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewState

class StockEventActions(
    event: OverviewReminderEvent,
    view: View,
    popupWindow: PopupWindow
) : ReminderEventActions(event, view, popupWindow) {
    init {
        if (event.state == OverviewState.RAISED) {
            hideAll()
        }
        hideTakenSkippedReraise()

        deleteButton.setOnClickListener {
            processDeleteReminderEvent(view.context, event.reminderEvent)
            popupWindow.dismiss()
        }

    }

    private fun hideTakenSkippedReraise() {
        takenButton.visibility = View.INVISIBLE
        skippedButton.visibility = View.INVISIBLE
        reRaiseOrScheduleButton.visibility = View.INVISIBLE

        setAngle(anchorDeleteButton, 90f)
    }

    private fun hideAll() {
        takenButton.visibility = View.INVISIBLE
        skippedButton.visibility = View.INVISIBLE
        deleteButton.visibility = View.INVISIBLE
        reRaiseOrScheduleButton.visibility = View.INVISIBLE
    }
}

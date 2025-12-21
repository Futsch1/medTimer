package com.futsch1.medtimer.medicine.advancedSettings

import android.app.AlertDialog
import android.content.Context
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.Interval
import com.futsch1.medtimer.medicine.editors.IntervalEditor

class EditIntervalDialog(context: Context, reminder: Reminder, val intervalUpdatedCallback: (Int) -> Unit) {
    private val dialog: AlertDialog = AlertDialog.Builder(context)
        .setTitle(R.string.interval)
        .setPositiveButton(android.R.string.ok) { _, _ -> intervalUpdatedCallback(intervalEditor.getMinutes()) }
        .setNegativeButton(android.R.string.cancel) { _, _ -> dialog.cancel() }
        .setView(R.layout.include_edit_interval)
        .create()

    init {
        dialog.show()
    }

    private val intervalEditor: IntervalEditor = IntervalEditor(
        dialog.findViewById(R.id.editIntervalTime),
        dialog.findViewById(R.id.intervalUnit), reminder.timeInMinutes,
        if (reminder.reminderType == Reminder.ReminderType.WINDOWED_INTERVAL) 24 * 60 else Interval.MAX_INTERVAL_MINUTES
    )
}
package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.google.android.material.button.MaterialButton
import java.time.Instant
import java.time.LocalDate

class NewReminderTypeDialog(
    val context: Context,
    val activity: FragmentActivity,
    val medicine: Medicine,
    val medicineViewModel: MedicineViewModel
) {
    private val dialog: Dialog = Dialog(context)

    private fun continueCreate(reminderType: Reminder.ReminderType) {
        val reminder = Reminder(medicine.medicineId)
        setDefaults(reminder)
        when (reminderType) {
            Reminder.ReminderType.CONTINUOUS_INTERVAL -> {
                reminder.intervalStart = Instant.now().epochSecond
            }

            Reminder.ReminderType.WINDOWED_INTERVAL -> {
                reminder.windowedInterval = true
            }

            else -> {
                // Intentionally empty
            }
        }
        dialog.dismiss()
        NewReminderDialog(context, activity, medicine, medicineViewModel, reminder)
    }

    init {
        dialog.setContentView(R.layout.dialog_new_reminder_type)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<CardView>(R.id.timeBasedCard).setOnClickListener {
            continueCreate(Reminder.ReminderType.TIME_BASED)
        }
        dialog.findViewById<CardView>(R.id.continuousIntervalCard).setOnClickListener {
            continueCreate(Reminder.ReminderType.CONTINUOUS_INTERVAL)
        }
        dialog.findViewById<CardView>(R.id.windowedIntervalCard).setOnClickListener {
            continueCreate(Reminder.ReminderType.WINDOWED_INTERVAL)
        }

        dialog.findViewById<MaterialButton>(R.id.cancelCreateReminder).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setDefaults(reminder: Reminder) {
        reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000
        reminder.cycleStartDay = LocalDate.now().plusDays(1).toEpochDay()
        reminder.instructions = ""
    }
}
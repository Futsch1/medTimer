package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.google.android.material.button.MaterialButton
import java.time.Instant
import java.time.LocalDate

class NewReminderTypeDialog(
    val context: Context,
    val activity: FragmentActivity,
    val fullMedicine: FullMedicine,
    val medicineViewModel: MedicineViewModel
) {
    private val dialog: Dialog = Dialog(context)

    private fun continueCreate(reminderType: Reminder.ReminderType) {
        val reminder = Reminder(fullMedicine.medicine.medicineId)
        setDefaults(reminder)
        when (reminderType) {
            Reminder.ReminderType.CONTINUOUS_INTERVAL -> {
                reminder.intervalStart = Instant.now().epochSecond
            }

            Reminder.ReminderType.WINDOWED_INTERVAL -> {
                reminder.windowedInterval = true
            }

            Reminder.ReminderType.OUT_OF_STOCK -> {
                reminder.outOfStockThreshold = if (fullMedicine.medicine.amount > 0.0) fullMedicine.medicine.amount else 1.0
                reminder.outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE
            }

            Reminder.ReminderType.EXPIRATION_DATE -> {
                reminder.expirationReminderType = Reminder.ExpirationReminderType.ONCE
            }

            Reminder.ReminderType.TIME_BASED -> Unit

            Reminder.ReminderType.LINKED, Reminder.ReminderType.REFILL -> {
                // May never happen
                assert(false)
            }
        }
        dialog.dismiss()

        if (reminder.isOutOfStockOrExpirationReminder) {
            NewReminderStockDialog(context, activity, fullMedicine.medicine, medicineViewModel, reminder)
        } else {
            NewReminderDialog(context, activity, fullMedicine, medicineViewModel, reminder)
        }
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
        dialog.findViewById<CardView>(R.id.stockReminderCard).setOnClickListener {
            continueCreate(Reminder.ReminderType.OUT_OF_STOCK)
        }
        dialog.findViewById<CardView>(R.id.expirationDateReminderCard).setOnClickListener {
            continueCreate(Reminder.ReminderType.EXPIRATION_DATE)
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
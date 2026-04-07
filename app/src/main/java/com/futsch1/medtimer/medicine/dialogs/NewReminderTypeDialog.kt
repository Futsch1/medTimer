package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.R
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderType
import com.google.android.material.button.MaterialButton
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate

class NewReminderTypeDialog @AssistedInject constructor(
    @Assisted val activity: FragmentActivity,
    @Assisted val medicine: Medicine,
    val newReminderDialogFactory: NewReminderDialog.Factory,
    val newReminderStockDialogFactory: NewReminderStockDialog.Factory
) {
    @AssistedFactory
    interface Factory {
        fun create(activity: FragmentActivity, medicine: Medicine): NewReminderTypeDialog
    }

    private val dialog: Dialog = Dialog(activity)

    private fun continueCreate(reminderType: ReminderType) {
        var reminder = Reminder.default().copy(
            medicineRelId = medicine.id,
            createdTime = Instant.now(),
            cycleStartDay = LocalDate.now().plusDays(1),
            instructions = ""
        )
        when (reminderType) {
            ReminderType.CONTINUOUS_INTERVAL -> {
                reminder = reminder.copy(intervalStart = Instant.now())
            }

            ReminderType.WINDOWED_INTERVAL -> {
                reminder = reminder.copy(windowedInterval = true)
            }

            ReminderType.OUT_OF_STOCK -> {
                reminder = reminder.copy(
                    outOfStockThreshold = if (medicine.amount > 0.0) medicine.amount else 1.0,
                    outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE
                )
            }

            ReminderType.EXPIRATION_DATE -> {
                reminder = reminder.copy(expirationReminderType = Reminder.ExpirationReminderType.ONCE)
            }

            ReminderType.TIME_BASED -> Unit

            ReminderType.LINKED, ReminderType.REFILL -> {
                // May never happen
                assert(false)
            }
        }
        dialog.dismiss()

        if (reminder.isOutOfStockOrExpirationReminder) {
            newReminderStockDialogFactory.create(activity, medicine, reminder)
        } else {
            newReminderDialogFactory.create(activity, medicine, reminder)
        }
    }

    init {
        dialog.setContentView(R.layout.dialog_new_reminder_type)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<CardView>(R.id.timeBasedCard).setOnClickListener {
            continueCreate(ReminderType.TIME_BASED)
        }
        dialog.findViewById<CardView>(R.id.continuousIntervalCard).setOnClickListener {
            continueCreate(ReminderType.CONTINUOUS_INTERVAL)
        }
        dialog.findViewById<CardView>(R.id.windowedIntervalCard).setOnClickListener {
            continueCreate(ReminderType.WINDOWED_INTERVAL)
        }
        dialog.findViewById<CardView>(R.id.stockReminderCard).setOnClickListener {
            continueCreate(ReminderType.OUT_OF_STOCK)
        }
        dialog.findViewById<CardView>(R.id.expirationDateReminderCard).setOnClickListener {
            continueCreate(ReminderType.EXPIRATION_DATE)
        }

        dialog.findViewById<MaterialButton>(R.id.cancelCreateReminder).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}

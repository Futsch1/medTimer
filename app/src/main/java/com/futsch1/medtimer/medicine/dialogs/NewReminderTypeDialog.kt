package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEntity
import com.google.android.material.button.MaterialButton
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate

class NewReminderTypeDialog @AssistedInject constructor(
    @Assisted val activity: FragmentActivity,
    @Assisted val fullMedicine: FullMedicineEntity,
    val medicineRepository: MedicineRepository,
    val newReminderDialogFactory: NewReminderDialog.Factory,
    val newReminderStockDialogFactory: NewReminderStockDialog.Factory
) {
    @AssistedFactory
    interface Factory {
        fun create(activity: FragmentActivity, fullMedicine: FullMedicineEntity): NewReminderTypeDialog
    }

    private val dialog: Dialog = Dialog(activity)

    private fun continueCreate(reminderType: ReminderEntity.ReminderType) {
        val reminder = ReminderEntity(fullMedicine.medicine.medicineId)
        setDefaults(reminder)
        when (reminderType) {
            ReminderEntity.ReminderType.CONTINUOUS_INTERVAL -> {
                reminder.intervalStart = Instant.now().epochSecond
            }

            ReminderEntity.ReminderType.WINDOWED_INTERVAL -> {
                reminder.windowedInterval = true
            }

            ReminderEntity.ReminderType.OUT_OF_STOCK -> {
                reminder.outOfStockThreshold = if (fullMedicine.medicine.amount > 0.0) fullMedicine.medicine.amount else 1.0
                reminder.outOfStockReminderType = ReminderEntity.OutOfStockReminderType.ONCE
            }

            ReminderEntity.ReminderType.EXPIRATION_DATE -> {
                reminder.expirationReminderType = ReminderEntity.ExpirationReminderType.ONCE
            }

            ReminderEntity.ReminderType.TIME_BASED -> Unit

            ReminderEntity.ReminderType.LINKED, ReminderEntity.ReminderType.REFILL -> {
                // May never happen
                assert(false)
            }
        }
        dialog.dismiss()

        if (reminder.isOutOfStockOrExpirationReminder) {
            newReminderStockDialogFactory.create(activity, fullMedicine.medicine, reminder)
        } else {
            newReminderDialogFactory.create(activity, fullMedicine, reminder)
        }
    }

    init {
        dialog.setContentView(R.layout.dialog_new_reminder_type)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<CardView>(R.id.timeBasedCard).setOnClickListener {
            continueCreate(ReminderEntity.ReminderType.TIME_BASED)
        }
        dialog.findViewById<CardView>(R.id.continuousIntervalCard).setOnClickListener {
            continueCreate(ReminderEntity.ReminderType.CONTINUOUS_INTERVAL)
        }
        dialog.findViewById<CardView>(R.id.windowedIntervalCard).setOnClickListener {
            continueCreate(ReminderEntity.ReminderType.WINDOWED_INTERVAL)
        }
        dialog.findViewById<CardView>(R.id.stockReminderCard).setOnClickListener {
            continueCreate(ReminderEntity.ReminderType.OUT_OF_STOCK)
        }
        dialog.findViewById<CardView>(R.id.expirationDateReminderCard).setOnClickListener {
            continueCreate(ReminderEntity.ReminderType.EXPIRATION_DATE)
        }

        dialog.findViewById<MaterialButton>(R.id.cancelCreateReminder).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setDefaults(reminder: ReminderEntity) {
        reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000
        reminder.cycleStartDay = LocalDate.now().plusDays(1).toEpochDay()
        reminder.instructions = ""
    }
}
package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import android.graphics.Color
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.ReminderNotificationChannelManager
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder

class StockHandlingWork(val context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    override fun doWork(): Result {
        val reminderEventId = inputData.getInt(ActivityCodes.EXTRA_REMINDER_EVENT_ID, 0)
        val medicineRepository = MedicineRepository(context as Application?)
        val reminderEvent = medicineRepository.getReminderEvent(reminderEventId)
            ?: return Result.failure()
        val reminder = medicineRepository.getReminder(reminderEvent.reminderId)
            ?: return Result.failure()
        val medicine = medicineRepository.getMedicine(reminder.medicineRelId)
            ?: return Result.failure()

        processStock(medicine, reminder)
        medicineRepository.updateMedicine(medicine)

        return Result.success()
    }

    private fun processStock(medicine: Medicine, reminder: Reminder) {
        // Extract number from reminder amount setting using regex
        val amountRegex = Regex("""(\d+)""")
        val match = amountRegex.find(reminder.amount)
        val amount = match?.groupValues?.get(1)?.toInt() ?: 0

        medicine.medicationAmount -= amount
        if (medicine.medicationAmount < 0) {
            medicine.medicationAmount = 0
        }

        checkForThreshold(medicine, amount)
    }

    private fun checkForThreshold(medicine: Medicine, amount: Int) {
        if (medicine.medicationAmount <= medicine.medicationAmountReminderThreshold && (medicine.medicationStockReminder == Medicine.MedicationStockReminder.ALWAYS ||
                    (medicine.medicationStockReminder == Medicine.MedicationStockReminder.ONCE && medicine.medicationAmount + amount > medicine.medicationAmountReminderThreshold))
        ) {
            val color = if (medicine.useColor) Color.valueOf(medicine.color) else null
            val notificationImportance =
                if (medicine.notificationImportance == ReminderNotificationChannelManager.Importance.HIGH.value) ReminderNotificationChannelManager.Importance.HIGH else ReminderNotificationChannelManager.Importance.DEFAULT

            Notifications(context).showOutOfStockNotification(
                medicine.name,
                amount.toString(),
                color,
                medicine.iconId,
                notificationImportance
            )
        }
    }


}

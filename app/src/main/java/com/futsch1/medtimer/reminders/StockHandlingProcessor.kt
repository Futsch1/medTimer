package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import java.time.Instant

/**
 * Responsible for updating medicine stock levels.
 *
 * This class retrieves the medicine ID and the amount to subtract from the input data.
 * It updates the stock in the database, ensures the amount does not drop below zero,
 * and triggers a notification if the stock level crosses the configured threshold.
 */
class StockHandlingProcessor(val reminderContext: ReminderContext) {
    val medicineRepository = reminderContext.medicineRepository

    fun processStock(amount: Double, medicineId: Int, processedInstant: Instant) {
        val medicine = medicineRepository.getMedicine(medicineId) ?: return

        processStock(medicine, amount, processedInstant)
        medicineRepository.updateMedicine(medicine.medicine)

    }

    private fun processStock(fullMedicine: FullMedicine, decreaseAmount: Double, processedInstant: Instant) {
        val medicine = fullMedicine.medicine
        medicine.amount -= decreaseAmount
        if (medicine.amount < 0) {
            medicine.amount = 0.0
        }

        checkForThreshold(fullMedicine, decreaseAmount, processedInstant)
        Log.d(LogTags.STOCK_HANDLING, "Decrease stock for medicine ${medicine.name} by $decreaseAmount resulting in ${medicine.amount}.")
    }

    private fun checkForThreshold(fullMedicine: FullMedicine, decreaseAmount: Double, processedInstant: Instant) {
        for (reminder in fullMedicine.reminders) {
            if (reminder.reminderType == Reminder.ReminderType.OUT_OF_STOCK && fullMedicine.medicine.amount <= reminder.outOfStockThreshold) {
                val showEvent =
                    when (reminder.outOfStockReminderType) {
                        Reminder.OutOfStockReminderType.ONCE -> {
                            fullMedicine.medicine.amount + decreaseAmount > reminder.outOfStockThreshold
                        }

                        Reminder.OutOfStockReminderType.ALWAYS -> true
                        else -> {
                            false
                        }
                    }

                if (showEvent) {
                    Log.i(LogTags.STOCK_HANDLING, "Show out of stock reminder rID ${reminder.reminderId}")
                    val scheduledReminder = ScheduledReminder(fullMedicine, reminder, processedInstant)
                    val reminderNotificationData = ReminderNotificationData.fromScheduledReminders(listOf(scheduledReminder))
                    ShowReminderNotificationProcessor(reminderContext).showReminder(reminderNotificationData)
                }
            }
        }
    }


}

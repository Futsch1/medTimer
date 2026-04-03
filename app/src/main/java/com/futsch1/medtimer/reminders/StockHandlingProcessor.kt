package com.futsch1.medtimer.reminders

import android.content.Context
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

class StockHandlingProcessor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val medicineRepository: MedicineRepository
) {
    suspend fun processStock(amount: Double, medicineId: Int, processedInstant: Instant) {
        val fullMedicine = medicineRepository.decreaseStock(medicineId, amount) ?: return
        Log.d(LogTags.STOCK_HANDLING, "Decrease stock for medicine ${fullMedicine.medicine.name} by $amount resulting in ${fullMedicine.medicine.amount}.")
        checkForThreshold(fullMedicine, amount, processedInstant)
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
                    ReminderProcessorBroadcastReceiver.requestShowReminderNotification(context, reminderNotificationData)
                }
            }
        }
    }
}

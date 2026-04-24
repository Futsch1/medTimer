package com.futsch1.medtimer.reminders

import android.content.Context
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

class StockHandlingProcessor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val medicineRepository: MedicineRepository
) {
    suspend fun processStock(amount: Double, medicineId: Int, processedInstant: Instant) {
        val medicine = medicineRepository.decreaseStock(medicineId, amount) ?: return
        Log.d(LogTags.STOCK_HANDLING, "Decrease stock for medicine ${medicine.name} by $amount resulting in ${medicine.amount}.")
        checkForThreshold(medicine, amount, processedInstant)
    }

    private fun checkForThreshold(medicine: Medicine, decreaseAmount: Double, processedInstant: Instant) {
        for (reminder in medicine.reminders) {
            if (reminder.outOfStockReminderType == Reminder.OutOfStockReminderType.OFF || medicine.amount > reminder.outOfStockThreshold) {
                continue
            }
            val showEvent =
                when (reminder.outOfStockReminderType) {
                    Reminder.OutOfStockReminderType.ONCE -> {
                        medicine.amount + decreaseAmount > reminder.outOfStockThreshold
                    }

                    Reminder.OutOfStockReminderType.ALWAYS -> true
                    else -> {
                        false
                    }
                }

            if (showEvent) {
                Log.i(LogTags.STOCK_HANDLING, "Show out of stock reminder rID ${reminder.id}")
                val scheduledReminder = ScheduledReminder(medicine, reminder, processedInstant)
                val reminderNotificationData = ReminderNotificationData.fromScheduledReminders(listOf(scheduledReminder))
                ReminderProcessorBroadcastReceiver.requestShowReminderNotification(context, reminderNotificationData)
            }
        }
    }
}

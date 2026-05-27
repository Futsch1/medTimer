package com.futsch1.medtimer.feature.reminders

import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import dagger.Lazy
import java.time.Instant
import javax.inject.Inject

data class StockChange(val after: Double)

class StockHandlingProcessor @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val showReminderNotificationProcessor: Lazy<ShowReminderNotificationProcessor>
) {
    suspend fun processStock(amount: Double, medicineId: Int, processedInstant: Instant): StockChange? {
        val medicine = medicineRepository.decreaseStock(medicineId, amount) ?: return null
        Log.d(LogTags.STOCK_HANDLING, "Decrease stock for medicine ${medicine.name} by $amount resulting in ${medicine.amount}.")
        checkForThreshold(medicine, amount, processedInstant)
        return StockChange(after = medicine.amount)
    }

    private suspend fun checkForThreshold(medicine: Medicine, decreaseAmount: Double, processedInstant: Instant) {
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
                showReminderNotificationProcessor.get().showReminder(reminderNotificationData)
            }
        }
    }
}

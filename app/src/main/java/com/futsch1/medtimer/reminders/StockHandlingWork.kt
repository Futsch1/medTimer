package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
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

        medicine.amount -= amount
        if (medicine.amount < 0) {
            medicine.amount = 0
        }

        checkForThreshold(medicine, amount)
    }

    private fun checkForThreshold(medicine: Medicine, amount: Int) {
        if (medicine.amount <= medicine.outOfStockReminderThreshold && (medicine.outOfStockReminder == Medicine.OutOfStockReminderType.ALWAYS ||
                    (medicine.outOfStockReminder == Medicine.OutOfStockReminderType.ONCE && medicine.amount + amount > medicine.outOfStockReminderThreshold))
        ) {
            Notifications(context).showOutOfStockNotification(
                medicine
            )
        }
    }


}

package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository

class StockHandlingWorker(val context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    override fun doWork(): Result {
        val amount = inputData.getDouble(ActivityCodes.EXTRA_AMOUNT, Double.NaN)
        if (amount.isNaN()) {
            return Result.failure()
        }
        val medicineId = inputData.getInt(ActivityCodes.EXTRA_MEDICINE_ID, -1)
        val medicineRepository = MedicineRepository(context as Application?)
        val medicine = medicineRepository.getOnlyMedicine(medicineId)
            ?: return Result.failure()

        processStock(medicine, amount)
        medicineRepository.updateMedicine(medicine)
        // Make sure that the database is flushed to avoid races between subsequent stock handling events
        medicineRepository.flushDatabase()

        return Result.success()
    }

    private fun processStock(medicine: Medicine, amount: Double) {
        medicine.amount -= amount
        if (medicine.amount < 0) {
            medicine.amount = 0.0
        }

        checkForThreshold(medicine, amount)
        Log.d(LogTags.STOCK_HANDLING, "Decrease stock for medicine ${medicine.name} by $amount resulting in ${medicine.amount}.")
    }

    private fun checkForThreshold(medicine: Medicine, amount: Double) {
        if (medicine.amount <= medicine.outOfStockReminderThreshold && (medicine.outOfStockReminder == Medicine.OutOfStockReminderType.ALWAYS ||
                    (medicine.outOfStockReminder == Medicine.OutOfStockReminderType.ONCE && medicine.amount + amount > medicine.outOfStockReminderThreshold))
        ) {
            Notifications(context).showOutOfStockNotification(
                medicine
            )
        }
    }


}

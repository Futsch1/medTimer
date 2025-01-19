package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.MedicineHelper

class StockHandlingWork(val context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    override fun doWork(): Result {
        val amount = inputData.getString(ActivityCodes.EXTRA_AMOUNT) ?: return Result.failure()
        val medicineId = inputData.getInt(ActivityCodes.EXTRA_MEDICINE_ID, -1)
        val medicineRepository = MedicineRepository(context as Application?)
        val medicine = medicineRepository.getMedicine(medicineId)
            ?: return Result.failure()

        processStock(medicine, amount)
        medicineRepository.updateMedicine(medicine)

        return Result.success()
    }

    private fun processStock(medicine: Medicine, reminderAmount: String) {
        val amount: Double? = MedicineHelper.parseAmount(reminderAmount)
        if (amount != null) {
            medicine.amount -= amount
            if (medicine.amount < 0) {
                medicine.amount = 0.0
            }

            checkForThreshold(medicine, amount)
        }
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

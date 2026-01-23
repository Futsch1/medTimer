package com.futsch1.medtimer.medicine.stockSettings

import android.content.Context
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.EntityDataStore
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper

class MedicineDataStore(
    override val entityId: Int, val context: Context, val medicineRepository: MedicineRepository,
) : EntityDataStore<Medicine>() {
    override var entity: Medicine = medicineRepository.getMedicine(entityId)!!.medicine

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "amount" -> MedicineHelper.formatAmount(entity.amount, "")
            "stock_unit" -> entity.unit
            "stock_reminder" -> entity.outOfStockReminder.ordinal.toString()
            "stock_threshold" -> MedicineHelper.formatAmount(entity.outOfStockReminderThreshold, "")
            "stock_refill_size" -> MedicineHelper.formatAmount(entity.refillSize, "")
            "production_date" -> TimeHelper.daysSinceEpochToDateString(context, entity.productionDate)
            "expiration_date" -> TimeHelper.daysSinceEpochToDateString(context, entity.expirationDate)
            "expiration_reminder" -> entity.expirationReminder.ordinal.toString()
            else -> defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            "amount" -> MedicineHelper.parseAmount(value)?.let { entity.amount = it }
            "stock_unit" -> entity.unit = value!!
            "stock_reminder" -> entity.outOfStockReminder = Medicine.OutOfStockReminderType.entries[value!!.toInt()]
            "stock_threshold" -> MedicineHelper.parseAmount(value)?.let { entity.outOfStockReminderThreshold = it }
            "stock_refill_size" -> MedicineHelper.parseAmount(value)?.let { entity.refillSizes = arrayListOf(it) }
            "production_date" -> entity.productionDate = TimeHelper.stringToLocalDate(context, value!!)!!.toEpochDay()
            "expiration_date" -> entity.expirationDate = TimeHelper.stringToLocalDate(context, value!!)!!.toEpochDay()
            "expiration_reminder" -> entity.expirationReminder = Medicine.ExpirationReminderType.entries[value!!.toInt()]
        }
        medicineRepository.updateMedicine(entity)
    }
}
package com.futsch1.medtimer.medicine.stockSettings

import android.content.Context
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.EntityDataStore
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper

class MedicineDataStore(
    override val entityId: Int, val context: Context, val medicineRepository: MedicineRepository,
) : EntityDataStore<FullMedicine>() {
    override var entity: FullMedicine = medicineRepository.getMedicine(entityId)!!

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "amount" -> MedicineHelper.formatAmount(entity.medicine.amount, "")
            "stock_unit" -> entity.medicine.unit
            "stock_refill_size" -> MedicineHelper.formatAmount(entity.medicine.refillSize, "")
            "production_date" -> TimeHelper.daysSinceEpochToDateString(context, entity.medicine.productionDate)
            "expiration_date" -> TimeHelper.daysSinceEpochToDateString(context, entity.medicine.expirationDate)
            else -> defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            "amount" -> MedicineHelper.parseAmount(value)?.let { entity.medicine.amount = it }
            "stock_unit" -> entity.medicine.unit = value!!
            "stock_refill_size" -> MedicineHelper.parseAmount(value)?.let { entity.medicine.refillSizes = arrayListOf(it) }
            "production_date" -> entity.medicine.productionDate = TimeHelper.stringToLocalDate(context, value!!)!!.toEpochDay()
            "expiration_date" -> entity.medicine.expirationDate = TimeHelper.stringToLocalDate(context, value!!)!!.toEpochDay()
        }
        medicineRepository.updateMedicine(entity.medicine)
    }

    override fun putLong(key: String?, value: Long) {
        when (key) {
            "production_date" -> entity.medicine.productionDate = value
            "expiration_date" -> entity.medicine.expirationDate = value
        }
        medicineRepository.updateMedicine(entity.medicine)
    }
}
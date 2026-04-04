package com.futsch1.medtimer.medicine.stockSettings

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.helpers.EntityDataStore
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MedicineDataStore @AssistedInject constructor(
    @Assisted override var entity: FullMedicineEntity,
    private val medicineRepository: MedicineRepository,
    private val timeFormatter: TimeFormatter,
    @param:ApplicationScope private val coroutineScope: CoroutineScope
) : EntityDataStore<FullMedicineEntity>() {

    @AssistedFactory
    interface Factory {
        fun create(entity: FullMedicineEntity): MedicineDataStore
    }

    override val entityId: Int get() = entity.medicine.medicineId

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "amount" -> MedicineHelper.formatAmount(entity.medicine.amount, "")
            "stock_unit" -> entity.medicine.unit
            "stock_refill_size" -> MedicineHelper.formatAmount(entity.medicine.refillSize, "")
            "production_date" -> timeFormatter.daysSinceEpochToDateString(entity.medicine.productionDate)
            "expiration_date" -> timeFormatter.daysSinceEpochToDateString(entity.medicine.expirationDate)
            else -> defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            "amount" -> MedicineHelper.parseAmount(value)?.let { entity.medicine.amount = it }
            "stock_unit" -> entity.medicine.unit = value!!
            "stock_refill_size" -> MedicineHelper.parseAmount(value)?.let { entity.medicine.refillSizes = arrayListOf(it) }
            "production_date" -> entity.medicine.productionDate = timeFormatter.stringToLocalDate(value!!)!!.toEpochDay()
            "expiration_date" -> entity.medicine.expirationDate = timeFormatter.stringToLocalDate(value!!)!!.toEpochDay()
        }
        coroutineScope.launch {
            medicineRepository.update(entity.medicine)
        }
    }

    override fun putLong(key: String?, value: Long) {
        when (key) {
            "production_date" -> entity.medicine.productionDate = value
            "expiration_date" -> entity.medicine.expirationDate = value
        }
        coroutineScope.launch {
            medicineRepository.update(entity.medicine)
        }
    }
}

package com.futsch1.medtimer.medicine.stockSettings

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.ModelDataStore
import com.futsch1.medtimer.helpers.TimeFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MedicineDataStore @AssistedInject constructor(
    @Assisted override var modelData: FullMedicineEntity,
    private val medicineRepository: MedicineRepository,
    private val timeFormatter: TimeFormatter,
    @param:ApplicationScope private val coroutineScope: CoroutineScope
) : ModelDataStore<FullMedicineEntity>() {

    @AssistedFactory
    interface Factory {
        fun create(entity: FullMedicineEntity): MedicineDataStore
    }

    override val modelDataId: Int get() = modelData.medicine.medicineId

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "amount" -> MedicineHelper.formatAmount(modelData.medicine.amount, "")
            "stock_unit" -> modelData.medicine.unit
            "stock_refill_size" -> MedicineHelper.formatAmount(modelData.medicine.refillSize, "")
            "production_date" -> timeFormatter.daysSinceEpochToDateString(modelData.medicine.productionDate)
            "expiration_date" -> timeFormatter.daysSinceEpochToDateString(modelData.medicine.expirationDate)
            else -> defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            "amount" -> MedicineHelper.parseAmount(value)?.let { modelData.medicine.amount = it }
            "stock_unit" -> modelData.medicine.unit = value!!
            "stock_refill_size" -> MedicineHelper.parseAmount(value)?.let { modelData.medicine.refillSizes = arrayListOf(it) }
            "production_date" -> modelData.medicine.productionDate = timeFormatter.stringToLocalDate(value!!)!!.toEpochDay()
            "expiration_date" -> modelData.medicine.expirationDate = timeFormatter.stringToLocalDate(value!!)!!.toEpochDay()
        }
        coroutineScope.launch {
            medicineRepository.update(modelData.medicine)
        }
    }

    override fun putLong(key: String?, value: Long) {
        when (key) {
            "production_date" -> modelData.medicine.productionDate = value
            "expiration_date" -> modelData.medicine.expirationDate = value
        }
        coroutineScope.launch {
            medicineRepository.update(modelData.medicine)
        }
    }
}

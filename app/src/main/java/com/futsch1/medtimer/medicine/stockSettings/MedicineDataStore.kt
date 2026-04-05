package com.futsch1.medtimer.medicine.stockSettings

import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.ModelDataStore
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.model.Medicine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class MedicineDataStore @AssistedInject constructor(
    @Assisted override var modelData: Medicine,
    private val medicineRepository: MedicineRepository,
    private val timeFormatter: TimeFormatter,
    @param:ApplicationScope private val coroutineScope: CoroutineScope
) : ModelDataStore<Medicine>() {

    @AssistedFactory
    interface Factory {
        fun create(modelData: Medicine): MedicineDataStore
    }

    override val modelDataId: Int get() = modelData.id

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "amount" -> MedicineHelper.formatAmount(modelData.amount, "")
            "stock_unit" -> modelData.unit
            "stock_refill_size" -> MedicineHelper.formatAmount(modelData.refillSize, "")
            "production_date" -> timeFormatter.localDateToString(modelData.productionDate)
            "expiration_date" -> timeFormatter.localDateToString(modelData.expirationDate)
            else -> defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            "amount" -> MedicineHelper.parseAmount(value)?.let { modelData = modelData.copy(amount = it) }
            "stock_unit" -> modelData = modelData.copy(unit = value!!)
            "stock_refill_size" -> MedicineHelper.parseAmount(value)?.let { modelData = modelData.copy(refillSize = it) }
            "production_date" -> modelData = modelData.copy(productionDate = timeFormatter.stringToLocalDate(value!!)!!)
            "expiration_date" -> modelData = modelData.copy(expirationDate = timeFormatter.stringToLocalDate(value!!)!!)
        }
        coroutineScope.launch {
            medicineRepository.update(modelData)
        }
    }

    override fun putLong(key: String?, value: Long) {
        when (key) {
            "production_date" -> modelData = modelData.copy(productionDate = LocalDate.ofEpochDay(value))
            "expiration_date" -> modelData = modelData.copy(expirationDate = LocalDate.ofEpochDay(value))
        }
        coroutineScope.launch {
            medicineRepository.update(modelData)
        }
    }
}

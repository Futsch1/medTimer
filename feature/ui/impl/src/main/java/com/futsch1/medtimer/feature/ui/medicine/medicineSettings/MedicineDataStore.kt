package com.futsch1.medtimer.feature.ui.medicine.medicineSettings

import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.ModelDataStore
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
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
    fun interface Factory {
        fun create(modelData: Medicine): MedicineDataStore
    }

    override val modelDataId: Int get() = modelData.id

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return when (key) {
            "use_color" -> modelData.useColor
            "cannot_be_skipped" -> modelData.cannotBeSkipped
            else -> defValue
        }
    }

    override fun putBoolean(key: String?, value: Boolean) {
        when (key) {
            "use_color" -> modelData = modelData.copy(useColor = value)
            "cannot_be_skipped" -> modelData = modelData.copy(cannotBeSkipped = value)
        }
        coroutineScope.launch {
            medicineRepository.update(modelData)
        }
    }

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "amount" -> MedicineHelper.formatAmount(modelData.amount, "")
            "stock_unit" -> modelData.unit
            "stock_refill_size" -> MedicineHelper.formatAmount(modelData.refillSize, "")
            "production_date" -> timeFormatter.localDateToString(modelData.productionDate)
            "expiration_date" -> timeFormatter.localDateToString(modelData.expirationDate)
            "notification_importance" -> if (modelData.notificationImportance == Medicine.NotificationImportance.DEFAULT) {
                "0"
            } else {
                if (modelData.showNotificationAsAlarm) "2" else "1"
            }

            else -> defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            "amount" -> MedicineHelper.parseAmount(value)
                ?.let { modelData = modelData.copy(amount = it) }

            "stock_unit" -> modelData = modelData.copy(unit = value!!)
            "stock_refill_size" -> MedicineHelper.parseAmount(value)
                ?.let { modelData = modelData.copy(refillSize = it) }

            "production_date" -> modelData =
                modelData.copy(productionDate = timeFormatter.stringToLocalDate(value!!)!!)

            "expiration_date" -> modelData =
                modelData.copy(expirationDate = timeFormatter.stringToLocalDate(value!!)!!)

            "notification_importance" -> modelData =
                modelData.copy(
                    notificationImportance = if (value == "0") Medicine.NotificationImportance.DEFAULT else Medicine.NotificationImportance.HIGH,
                    showNotificationAsAlarm = value == "2"
                )
        }
        coroutineScope.launch {
            medicineRepository.update(modelData)
        }
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return when (key) {
            "color" -> modelData.color
            else -> defValue
        }
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
            "color" -> modelData = modelData.copy(color = value)
        }
        coroutineScope.launch {
            medicineRepository.update(modelData)
        }
    }

    override fun putLong(key: String?, value: Long) {
        when (key) {
            "production_date" -> modelData =
                modelData.copy(productionDate = LocalDate.ofEpochDay(value))

            "expiration_date" -> modelData =
                modelData.copy(expirationDate = LocalDate.ofEpochDay(value))
        }
        coroutineScope.launch {
            medicineRepository.update(modelData)
        }
    }
}

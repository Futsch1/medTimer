package com.futsch1.medtimer.medicine.stockSettings

import android.content.Context
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.EntityDataStore

class MedicineDataStore(
    override val entityId: Int, val context: Context, val medicineRepository: MedicineRepository,
) : EntityDataStore<Medicine>() {
    override var entity: Medicine = medicineRepository.getMedicine(entityId)!!.medicine

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return when (key) {

            else -> defValue
        }
    }

    override fun putBoolean(key: String?, value: Boolean) {
        when (key) {
        }
        medicineRepository.updateMedicine(entity)
    }

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            else -> defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
        }
        medicineRepository.updateMedicine(entity)
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return when (key) {
            else -> defValue
        }
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
        }
        medicineRepository.updateMedicine(entity)
    }
}
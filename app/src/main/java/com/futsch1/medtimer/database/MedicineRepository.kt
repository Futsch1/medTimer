package com.futsch1.medtimer.database

import com.futsch1.medtimer.model.Medicine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

open class MedicineRepository(
    private val medicineDao: MedicineDao
) {
    fun getFullAllFlow(): Flow<List<FullMedicineEntity>> = medicineDao.getMedicinesFlow()

    suspend fun get(medicineId: Int): Medicine? {
        return medicineDao.getMedicine(medicineId)?.toModel()
    }

    fun getFlow(medicineId: Int): Flow<Medicine?> {
        return medicineDao.getMedicineFlow(medicineId).map { it?.toModel() }
    }

    suspend fun getAll(): List<Medicine> {
        return medicineDao.getMedicines().map { it.toModel() }
    }

    fun getAllFlow(): Flow<List<Medicine>> {
        return medicineDao.getMedicinesFlow().map { medicines -> medicines.map { it.toModel() } }
    }

    suspend fun create(medicine: Medicine): Long {
        return medicineDao.insertMedicine(medicine.toEntity())
    }

    suspend fun delete(medicineId: Int) {
        medicineDao.getOnlyMedicine(medicineId)?.let { medicineDao.deleteMedicine(it) }
    }

    suspend fun update(medicine: Medicine) {
        medicineDao.updateMedicine(medicine.toEntity())
    }

    suspend fun updateAll(medicines: List<Medicine>) {
        medicineDao.updateMedicines(medicines.map { it.toEntity() })
    }

    suspend fun decreaseStock(medicineId: Int, decreaseAmount: Double): Medicine? {
        return medicineDao.decreaseStock(medicineId, decreaseAmount)?.toModel()
    }

    suspend fun getHighestSortOrder(): Double {
        return medicineDao.getHighestSortOrder()
    }

    suspend fun move(fromPosition: Int, toPosition: Int) {
        val medicines = medicineDao.getMedicines().toMutableList()
        try {
            val moveMedicine = medicines.removeAt(fromPosition)
            medicines.add(toPosition, moveMedicine)
            moveMedicine.medicine.sortOrder = (medicines[toPosition + 1].medicine.sortOrder + medicines[toPosition - 1].medicine.sortOrder) / 2
            medicineDao.updateMedicine(moveMedicine.medicine)
        } catch (_: IndexOutOfBoundsException) {
            // Intentionally left blank
        }
    }

    suspend fun deleteAll() {
        medicineDao.deleteAll()
    }
}

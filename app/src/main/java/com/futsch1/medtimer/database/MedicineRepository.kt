package com.futsch1.medtimer.database

import kotlinx.coroutines.flow.Flow

open class MedicineRepository(
    private val medicineDao: MedicineDao
) {
    fun getFullAllFlow(): Flow<List<FullMedicineEntity>> = medicineDao.getMedicinesFlow()

    suspend fun get(medicineId: Int): MedicineEntity? {
        return medicineDao.getOnlyMedicine(medicineId)
    }

    fun getFullFlow(medicineId: Int): Flow<FullMedicineEntity?> {
        return medicineDao.getMedicineFlow(medicineId)
    }

    suspend fun getFull(medicineId: Int): FullMedicineEntity? {
        return medicineDao.getMedicine(medicineId)
    }

    suspend fun getFullAll(): List<FullMedicineEntity> {
        return medicineDao.getMedicines()
    }

    suspend fun create(medicine: MedicineEntity): Long {
        return medicineDao.insertMedicine(medicine)
    }

    suspend fun delete(medicineId: Int) {
        medicineDao.getOnlyMedicine(medicineId)?.let { medicineDao.deleteMedicine(it) }
    }

    suspend fun update(medicine: MedicineEntity) {
        medicineDao.updateMedicine(medicine)
    }

    suspend fun updateAll(medicines: List<MedicineEntity>) {
        medicineDao.updateMedicines(medicines)
    }

    suspend fun decreaseStock(medicineId: Int, decreaseAmount: Double): FullMedicineEntity? {
        return medicineDao.decreaseStock(medicineId, decreaseAmount)
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
            update(moveMedicine.medicine)
        } catch (_: IndexOutOfBoundsException) {
            // Intentionally left blank
        }
    }

    suspend fun deleteAll() {
        medicineDao.deleteAll()
    }
}

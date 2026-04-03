package com.futsch1.medtimer.database

import kotlinx.coroutines.flow.Flow

open class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val tagDao: TagDao
) {
    fun getFullAllFlow(): Flow<List<FullMedicine>> = medicineDao.getFullAllFlow()

    suspend fun get(medicineId: Int): Medicine? {
        return medicineDao.get(medicineId)
    }

    fun getFullFlow(medicineId: Int): Flow<FullMedicine?> {
        return medicineDao.getFullFlow(medicineId)
    }

    suspend fun getFull(medicineId: Int): FullMedicine? {
        return medicineDao.getFull(medicineId)
    }

    suspend fun getFullAll(): List<FullMedicine> {
        return medicineDao.getFullAll()
    }

    suspend fun create(medicine: Medicine): Long {
        return medicineDao.create(medicine)
    }

    suspend fun delete(medicineId: Int) {
        tagDao.deleteMedicineToTagForMedicine(medicineId)
        medicineDao.get(medicineId)?.let { medicineDao.delete(it) }
    }

    suspend fun update(medicine: Medicine) {
        medicineDao.update(medicine)
    }

    suspend fun updateAll(medicines: List<Medicine>) {
        medicineDao.updateAll(medicines)
    }

    suspend fun decreaseStock(medicineId: Int, decreaseAmount: Double): FullMedicine? {
        return medicineDao.decreaseStock(medicineId, decreaseAmount)
    }

    suspend fun getHighestSortOrder(): Double {
        return medicineDao.getHighestSortOrder()
    }

    suspend fun move(fromPosition: Int, toPosition: Int) {
        val medicines = medicineDao.getFullAll().toMutableList()
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

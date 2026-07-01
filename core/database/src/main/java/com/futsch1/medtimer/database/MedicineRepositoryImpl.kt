package com.futsch1.medtimer.database

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.database.dao.MedicineDao
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.database.toModel.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MedicineRepositoryImpl(
    private val medicineDao: MedicineDao
) : MedicineRepository {
    override suspend fun fetch(medicineId: Int): Medicine? {
        return medicineDao.getFull(medicineId)?.toModel()
    }

    override fun getFlow(medicineId: Int): Flow<Medicine?> {
        return medicineDao.getFlow(medicineId).map { it?.toModel() }
    }

    override suspend fun getAll(): List<Medicine> {
        return medicineDao.getAll().map { it.toModel() }
    }

    override fun getAllFlow(): Flow<List<Medicine>> {
        return medicineDao.getAllFlow().map { medicines -> medicines.map { it.toModel() } }
    }

    override suspend fun create(medicine: Medicine): Int {
        return medicineDao.create(medicine.toEntity()).toInt()
    }

    override suspend fun delete(medicineId: Int) {
        medicineDao.fetch(medicineId)?.let { medicineDao.delete(it) }
    }

    override suspend fun update(medicine: Medicine) {
        medicineDao.update(medicine.toEntity())
    }

    override suspend fun updateAll(medicines: List<Medicine>) {
        medicineDao.updateAll(medicines.map { it.toEntity() })
    }

    override suspend fun decreaseStock(medicineId: Int, decreaseAmount: Double): Medicine? {
        return medicineDao.decreaseStock(medicineId, decreaseAmount)?.toModel()
    }

    override suspend fun getHighestSortOrder(): Double {
        return medicineDao.getHighestSortOrder()
    }

    override suspend fun move(id: Int, toPosition: Int) {
        val medicines = medicineDao.getAll().toMutableList()
        val fromPosition = medicines.indexOfFirst { it.medicine.medicineId == id }
        if (fromPosition == toPosition || medicines.size < 2) return

        val moveMedicine = medicines.removeAt(fromPosition)
        medicines.add(toPosition, moveMedicine)

        val newSortOrder = when (toPosition) {
            0 -> medicines[1].medicine.sortOrder - 1.0
            medicines.size - 1 -> medicines[toPosition - 1].medicine.sortOrder + 1.0
            else -> (medicines[toPosition + 1].medicine.sortOrder + medicines[toPosition - 1].medicine.sortOrder) / 2.0
        }

        moveMedicine.medicine.sortOrder = newSortOrder
        medicineDao.update(moveMedicine.medicine)
    }

    override suspend fun deleteAll() {
        medicineDao.deleteAll()
    }
}

package com.futsch1.medtimer.core.domain.repository

import com.futsch1.medtimer.core.domain.model.Medicine
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    suspend fun fetch(medicineId: Int): Medicine?
    fun getFlow(medicineId: Int): Flow<Medicine?>
    suspend fun getAll(): List<Medicine>
    fun getAllFlow(): Flow<List<Medicine>>
    suspend fun create(medicine: Medicine): Int
    suspend fun delete(medicineId: Int)
    suspend fun update(medicine: Medicine)
    suspend fun updateAll(medicines: List<Medicine>)
    suspend fun decreaseStock(medicineId: Int, decreaseAmount: Double): Medicine?
    suspend fun increaseStock(medicineId: Int, increaseAmount: Double): Medicine?
    suspend fun getHighestSortOrder(): Double
    suspend fun move(id: Int, toPosition: Int)
    suspend fun deleteAll()
}

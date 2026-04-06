package com.futsch1.medtimer.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MedicineDao {

    @Transaction
    open suspend fun decreaseStock(medicineId: Int, decreaseAmount: Double): FullMedicineEntity? {
        val medicine = getOnlyMedicine(medicineId) ?: return null
        medicine.amount = maxOf(0.0, medicine.amount - decreaseAmount)
        updateMedicine(medicine)
        return getMedicine(medicineId)
    }

    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    abstract fun getMedicinesFlow(): Flow<List<FullMedicineEntity>>

    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    abstract suspend fun getMedicines(): List<FullMedicineEntity>

    @Query("SELECT * FROM Medicine WHERE medicineId = :medicineId")
    abstract suspend fun getOnlyMedicine(medicineId: Int): MedicineEntity?

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId = :medicineId")
    abstract suspend fun getMedicine(medicineId: Int): FullMedicineEntity?

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId = :medicineId")
    abstract fun getMedicineFlow(medicineId: Int): Flow<FullMedicineEntity?>

    @Insert
    abstract suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    abstract suspend fun updateMedicine(medicine: MedicineEntity)

    @Update
    abstract suspend fun updateMedicines(medicines: List<MedicineEntity>)

    @Delete
    abstract suspend fun deleteMedicine(medicine: MedicineEntity)

    @Query("DELETE FROM Medicine")
    abstract suspend fun deleteAll()

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM Medicine")
    abstract suspend fun getHighestSortOrder(): Double
}

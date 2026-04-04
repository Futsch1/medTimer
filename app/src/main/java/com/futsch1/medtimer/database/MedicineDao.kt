package com.futsch1.medtimer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MedicineDao {

    @Transaction
    open suspend fun decreaseStock(medicineId: Int, decreaseAmount: Double): FullMedicine? {
        val fullMedicine = getFull(medicineId) ?: return null
        fullMedicine.medicine.amount = maxOf(0.0, fullMedicine.medicine.amount - decreaseAmount)
        update(fullMedicine.medicine)
        return fullMedicine
    }

    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    abstract fun getFullAllFlow(): Flow<List<FullMedicine>>

    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    abstract suspend fun getFullAll(): List<FullMedicine>

    @Query("SELECT * FROM Medicine WHERE medicineId = :medicineId")
    abstract suspend fun get(medicineId: Int): Medicine?

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId = :medicineId")
    abstract suspend fun getFull(medicineId: Int): FullMedicine?

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId = :medicineId")
    abstract fun getFullFlow(medicineId: Int): Flow<FullMedicine?>

    @Insert
    abstract suspend fun create(medicine: Medicine): Long

    @Update
    abstract suspend fun update(medicine: Medicine)

    @Update
    abstract suspend fun updateAll(medicines: List<Medicine>)

    @Delete
    abstract suspend fun delete(medicine: Medicine)

    @Query("DELETE FROM Medicine")
    abstract suspend fun deleteAll()

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM Medicine")
    abstract suspend fun getHighestSortOrder(): Double
}

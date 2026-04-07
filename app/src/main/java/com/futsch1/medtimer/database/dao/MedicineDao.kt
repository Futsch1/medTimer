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
        val medicine = get(medicineId) ?: return null
        medicine.amount = maxOf(0.0, medicine.amount - decreaseAmount)
        update(medicine)
        return getFull(medicineId)
    }

    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    abstract fun getAllFlow(): Flow<List<FullMedicineEntity>>

    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    abstract suspend fun getAll(): List<FullMedicineEntity>

    @Query("SELECT * FROM Medicine WHERE medicineId = :medicineId")
    abstract suspend fun get(medicineId: Int): MedicineEntity?

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId = :medicineId")
    abstract suspend fun getFull(medicineId: Int): FullMedicineEntity?

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId = :medicineId")
    abstract fun getFlow(medicineId: Int): Flow<FullMedicineEntity?>

    @Insert
    abstract suspend fun create(medicine: MedicineEntity): Long

    @Update
    abstract suspend fun update(medicine: MedicineEntity)

    @Update
    abstract suspend fun updateAll(medicines: List<MedicineEntity>)

    @Delete
    abstract suspend fun delete(medicine: MedicineEntity)

    @Query("DELETE FROM Medicine")
    abstract suspend fun deleteAll()

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM Medicine")
    abstract suspend fun getHighestSortOrder(): Double
}

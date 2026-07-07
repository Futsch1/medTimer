package com.futsch1.medtimer.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.futsch1.medtimer.database.BarcodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeDao {

    @Query("SELECT medicineId FROM Barcode WHERE barcode = :barcode")
    suspend fun findMedicineId(barcode: String): Int?

    @Query("SELECT * FROM Barcode WHERE medicineId = :medicineId")
    suspend fun getForMedicine(medicineId: Int): List<BarcodeEntity>

    @Query("SELECT * FROM Barcode")
    fun getAllFlow(): Flow<List<BarcodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun link(barcode: BarcodeEntity)

    @Query("DELETE FROM Barcode WHERE barcode = :barcode")
    suspend fun unlink(barcode: String)

    @Query("DELETE FROM Barcode")
    suspend fun deleteAll()
}

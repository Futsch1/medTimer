package com.futsch1.medtimer.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.futsch1.medtimer.database.MedicineLabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineLabelDao {

    @Query("SELECT * FROM MedicineLabel")
    suspend fun getAll(): List<MedicineLabelEntity>

    @Query("SELECT * FROM MedicineLabel")
    fun getAllFlow(): Flow<List<MedicineLabelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun remember(label: MedicineLabelEntity)

    @Query("DELETE FROM MedicineLabel WHERE text = :text")
    suspend fun forget(text: String)

    @Query("DELETE FROM MedicineLabel")
    suspend fun deleteAll()
}

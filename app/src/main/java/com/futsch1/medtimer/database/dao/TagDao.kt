package com.futsch1.medtimer.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.futsch1.medtimer.database.MedicineToTagEntity
import com.futsch1.medtimer.database.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM Tag")
    fun getAllFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM Tag WHERE name = :name")
    suspend fun getByName(name: String): TagEntity?

    @Query("SELECT COUNT(*) FROM Tag")
    suspend fun count(): Int

    @Insert
    suspend fun create(tag: TagEntity): Long

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM Tag")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createMedicineToTag(medicineToTag: MedicineToTagEntity)

    @Delete
    suspend fun deleteMedicineToTag(medicineToTag: MedicineToTagEntity)

    @Query("DELETE FROM MedicineToTag WHERE tagId = :tagId")
    suspend fun deleteMedicineToTagForTag(tagId: Int)

    @Query("DELETE FROM MedicineToTag WHERE medicineId = :medicineId")
    suspend fun deleteMedicineToTagForMedicine(medicineId: Int)

    @Query("DELETE FROM MedicineToTag")
    suspend fun deleteAllMedicineToTags()

    @Query("SELECT * FROM MedicineToTag")
    fun getMedicineTagsFlow(): Flow<List<MedicineToTagEntity>>
}

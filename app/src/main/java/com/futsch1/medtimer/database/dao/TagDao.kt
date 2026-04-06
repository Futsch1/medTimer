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
abstract class TagDao {

    @Query("SELECT * FROM Tag")
    abstract fun getAllFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM Tag WHERE name = :name")
    abstract suspend fun getByName(name: String): TagEntity?

    @Query("SELECT COUNT(*) FROM Tag")
    abstract suspend fun count(): Int

    @Insert
    abstract suspend fun create(tag: TagEntity): Long

    @Delete
    abstract suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM Tag")
    abstract suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun createMedicineToTag(medicineToTag: MedicineToTagEntity)

    @Delete
    abstract suspend fun deleteMedicineToTag(medicineToTag: MedicineToTagEntity)

    @Query("DELETE FROM MedicineToTag WHERE tagId = :tagId")
    abstract suspend fun deleteMedicineToTagForTag(tagId: Int)

    @Query("DELETE FROM MedicineToTag WHERE medicineId = :medicineId")
    abstract suspend fun deleteMedicineToTagForMedicine(medicineId: Int)

    @Query("DELETE FROM MedicineToTag")
    abstract suspend fun deleteAllMedicineToTags()

    @Query("SELECT * FROM MedicineToTag")
    abstract fun getMedicineTagsFlow(): Flow<List<MedicineToTagEntity>>
}

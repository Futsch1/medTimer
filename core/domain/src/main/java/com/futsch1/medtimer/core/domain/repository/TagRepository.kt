package com.futsch1.medtimer.core.domain.repository

import com.futsch1.medtimer.core.domain.model.MedicineToTag
import com.futsch1.medtimer.core.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllFlow(): Flow<List<Tag>>
    fun getMedicineTagsFlow(): Flow<List<MedicineToTag>>
    suspend fun create(tag: Tag): Int
    suspend fun getByName(name: String): Tag?
    suspend fun delete(tag: Tag)
    suspend fun addMedicineTag(medicineId: Int, tagId: Int)
    suspend fun removeMedicineTag(medicineId: Int, tagId: Int)
    fun hasAny(): Flow<Boolean>
    suspend fun deleteAll()
}

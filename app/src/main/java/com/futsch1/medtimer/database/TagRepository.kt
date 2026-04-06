package com.futsch1.medtimer.database

import com.futsch1.medtimer.database.dao.TagDao
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.database.toModel.toModel
import com.futsch1.medtimer.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

open class TagRepository(
    private val tagDao: TagDao
) {
    fun getAllFlow(): Flow<List<Tag>> = tagDao.getAllFlow().map { list ->
        list.map { it.toModel() }
    }

    fun getMedicineTagsFlow(): Flow<List<MedicineToTagEntity>> {
        return tagDao.getMedicineTagsFlow()
    }

    suspend fun create(tag: Tag): Long {
        val existingTagId = getByName(tag.name)?.id?.toLong()
        return existingTagId ?: tagDao.create(tag.toEntity())
    }

    suspend fun getByName(name: String): Tag? {
        return tagDao.getByName(name)?.toModel()
    }

    suspend fun delete(tag: Tag) {
        tagDao.deleteMedicineToTagForTag(tag.id)
        tagDao.delete(tag.toEntity())
    }

    suspend fun addMedicineTag(medicineId: Int, tagId: Int) {
        tagDao.createMedicineToTag(MedicineToTagEntity(medicineId, tagId))
    }

    suspend fun removeMedicineTag(medicineId: Int, tagId: Int) {
        tagDao.deleteMedicineToTag(MedicineToTagEntity(medicineId, tagId))
    }

    suspend fun hasAny(): Boolean {
        return tagDao.count() > 0
    }

    suspend fun deleteAll() {
        tagDao.deleteAll()
        tagDao.deleteAllMedicineToTags()
    }
}

package com.futsch1.medtimer.database

import kotlinx.coroutines.flow.Flow

open class TagRepository(
    private val tagDao: TagDao
) {
    fun getAllFlow(): Flow<List<TagEntity>> = tagDao.getAllFlow()

    fun getMedicineTagsFlow(): Flow<List<MedicineToTagEntity>> {
        return tagDao.getMedicineTagsFlow()
    }

    suspend fun create(tag: TagEntity): Long {
        val existingTagId = getByName(tag.name)?.tagId?.toLong()
        return existingTagId ?: tagDao.create(tag)
    }

    suspend fun getByName(name: String): TagEntity? {
        return tagDao.getByName(name)
    }

    suspend fun delete(tag: TagEntity) {
        tagDao.deleteMedicineToTagForTag(tag.tagId)
        tagDao.delete(tag)
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

package com.futsch1.medtimer.database

import kotlinx.coroutines.flow.Flow

open class TagRepository(
    private val tagDao: TagDao
) {
    fun getAllFlow(): Flow<List<Tag>> = tagDao.getAllFlow()

    suspend fun create(tag: Tag): Long {
        val existingTagId = getByName(tag.name)?.tagId?.toLong()
        return existingTagId ?: tagDao.create(tag)
    }

    suspend fun getByName(name: String): Tag? {
        return tagDao.getByName(name)
    }

    suspend fun delete(tag: Tag) {
        tagDao.deleteMedicineToTagForTag(tag.tagId)
        tagDao.delete(tag)
    }

    suspend fun addMedicineTag(medicineId: Int, tagId: Int) {
        tagDao.createMedicineToTag(MedicineToTag(medicineId, tagId))
    }

    suspend fun removeMedicineTag(medicineId: Int, tagId: Int) {
        tagDao.deleteMedicineToTag(MedicineToTag(medicineId, tagId))
    }

    fun getMedicineTagsFlow(): Flow<List<MedicineToTag>> {
        return tagDao.getMedicineTagsFlow()
    }

    suspend fun hasAny(): Boolean {
        return tagDao.count() > 0
    }

    suspend fun deleteAll() {
        tagDao.deleteAll()
        tagDao.deleteAllMedicineToTags()
    }
}

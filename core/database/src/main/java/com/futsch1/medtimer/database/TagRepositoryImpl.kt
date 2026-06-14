package com.futsch1.medtimer.database

import com.futsch1.medtimer.core.domain.model.MedicineToTag
import com.futsch1.medtimer.core.domain.model.Tag
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.database.dao.TagDao
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.database.toModel.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepositoryImpl(
    private val tagDao: TagDao
) : TagRepository {
    override fun getAllFlow(): Flow<List<Tag>> = tagDao.getAllFlow().map { list ->
        list.map { it.toModel() }
    }

    override fun getMedicineTagsFlow(): Flow<List<MedicineToTag>> {
        return tagDao.getMedicineTagsFlow().map { list -> list.map { it.toModel() } }
    }

    override suspend fun create(tag: Tag): Int {
        val existingTagId = getByName(tag.name)?.id
        return existingTagId ?: tagDao.create(tag.toEntity()).toInt()
    }

    override suspend fun getByName(name: String): Tag? {
        return tagDao.getByName(name)?.toModel()
    }

    override suspend fun delete(tag: Tag) {
        tagDao.deleteMedicineToTagForTag(tag.id)
        tagDao.delete(tag.toEntity())
    }

    override suspend fun addMedicineTag(medicineId: Int, tagId: Int) {
        tagDao.createMedicineToTag(MedicineToTagEntity(medicineId, tagId))
    }

    override suspend fun removeMedicineTag(medicineId: Int, tagId: Int) {
        tagDao.deleteMedicineToTag(MedicineToTagEntity(medicineId, tagId))
    }

    override fun hasAny(): Flow<Boolean> {
        return tagDao.countFlow().map { it > 0 }
    }

    override suspend fun deleteAll() {
        tagDao.deleteAll()
        tagDao.deleteAllMedicineToTags()
    }
}

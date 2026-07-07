package com.futsch1.medtimer.database

import com.futsch1.medtimer.core.domain.model.MedicineLabel
import com.futsch1.medtimer.core.domain.repository.MedicineLabelRepository
import com.futsch1.medtimer.database.dao.MedicineLabelDao
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.database.toModel.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MedicineLabelRepositoryImpl(
    private val medicineLabelDao: MedicineLabelDao
) : MedicineLabelRepository {
    override suspend fun getAll(): List<MedicineLabel> =
        medicineLabelDao.getAll().map { it.toModel() }

    override fun getAllFlow(): Flow<List<MedicineLabel>> =
        medicineLabelDao.getAllFlow().map { list -> list.map { it.toModel() } }

    override suspend fun remember(label: MedicineLabel) =
        medicineLabelDao.remember(label.toEntity())

    override suspend fun forget(text: String) =
        medicineLabelDao.forget(text)

    override suspend fun deleteAll() =
        medicineLabelDao.deleteAll()
}

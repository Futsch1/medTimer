package com.futsch1.medtimer.database

import com.futsch1.medtimer.core.domain.model.Barcode
import com.futsch1.medtimer.core.domain.repository.BarcodeRepository
import com.futsch1.medtimer.database.dao.BarcodeDao
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.database.toModel.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BarcodeRepositoryImpl(
    private val barcodeDao: BarcodeDao
) : BarcodeRepository {
    override suspend fun findMedicineId(barcode: String): Int? =
        barcodeDao.findMedicineId(barcode)

    override suspend fun getForMedicine(medicineId: Int): List<Barcode> =
        barcodeDao.getForMedicine(medicineId).map { it.toModel() }

    override fun getAllFlow(): Flow<List<Barcode>> =
        barcodeDao.getAllFlow().map { list -> list.map { it.toModel() } }

    override suspend fun link(barcode: Barcode) =
        barcodeDao.link(barcode.toEntity())

    override suspend fun unlink(barcode: String) =
        barcodeDao.unlink(barcode)

    override suspend fun deleteAll() =
        barcodeDao.deleteAll()
}

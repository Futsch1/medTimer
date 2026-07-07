package com.futsch1.medtimer.core.domain.repository

import com.futsch1.medtimer.core.domain.model.Barcode
import kotlinx.coroutines.flow.Flow

interface BarcodeRepository {
    /** Returns the medicineId linked to [barcode], or null if this code was never scanned before. */
    suspend fun findMedicineId(barcode: String): Int?

    /** All barcodes linked to a given medicine. */
    suspend fun getForMedicine(medicineId: Int): List<Barcode>

    fun getAllFlow(): Flow<List<Barcode>>

    /** Links a barcode to a medicine. Idempotent: re-linking the same barcode overwrites the target. */
    suspend fun link(barcode: Barcode)

    suspend fun unlink(barcode: String)

    suspend fun deleteAll()
}

package com.futsch1.medtimer.core.domain.repository

import com.futsch1.medtimer.core.domain.model.MedicineLabel
import kotlinx.coroutines.flow.Flow

interface MedicineLabelRepository {
    /** All remembered text snippets, used to fuzzy-match against freshly OCR'd package text. */
    suspend fun getAll(): List<MedicineLabel>

    fun getAllFlow(): Flow<List<MedicineLabel>>

    /** Remembers that [text] identifies this medicine. Idempotent: re-adding overwrites the target. */
    suspend fun remember(label: MedicineLabel)

    suspend fun forget(text: String)

    suspend fun deleteAll()
}

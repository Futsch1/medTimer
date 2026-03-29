package com.futsch1.medtimer.medicine.tags

import androidx.lifecycle.ViewModel
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MedicineWithTagsViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository
) : ViewModel() {
    fun getMedicineWithTags(medicineId: Int): Flow<FullMedicine?> =
        medicineRepository.getMedicineFlow(medicineId)

    val tags: Flow<List<Tag>> = medicineRepository.tagsFlow

    suspend fun associateTag(medicineId: Int, tagId: Int) {
        medicineRepository.insertMedicineToTag(medicineId, tagId)
    }

    suspend fun disassociateTag(medicineId: Int, tagId: Int) {
        medicineRepository.deleteMedicineToTag(medicineId, tagId)
    }
}
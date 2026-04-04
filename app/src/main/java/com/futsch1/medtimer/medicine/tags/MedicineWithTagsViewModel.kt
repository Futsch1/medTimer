package com.futsch1.medtimer.medicine.tags

import androidx.lifecycle.ViewModel
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.TagEntity
import com.futsch1.medtimer.database.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MedicineWithTagsViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val tagRepository: TagRepository
) : ViewModel() {
    fun getMedicineWithTags(medicineId: Int): Flow<FullMedicineEntity?> =
        medicineRepository.getFullFlow(medicineId)

    val tags: Flow<List<TagEntity>> = tagRepository.getAllFlow()

    suspend fun associateTag(medicineId: Int, tagId: Int) {
        tagRepository.addMedicineTag(medicineId, tagId)
    }

    suspend fun disassociateTag(medicineId: Int, tagId: Int) {
        tagRepository.removeMedicineTag(medicineId, tagId)
    }
}
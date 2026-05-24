package com.futsch1.medtimer.feature.ui.medicine.tags

import androidx.lifecycle.ViewModel
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Tag
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicineWithTagsViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val tagRepository: TagRepository
) : ViewModel() {
    fun getMedicineWithTags(medicineId: Int): Flow<Medicine?> =
        medicineRepository.getFlow(medicineId)

    val tags: Flow<List<Tag>> = tagRepository.getAllFlow()

    fun associateTag(medicineId: Int, tagId: Int) {
        viewModelScope.launch {
            tagRepository.addMedicineTag(medicineId, tagId)
        }
    }

    fun disassociateTag(medicineId: Int, tagId: Int) {
        viewModelScope.launch {
            tagRepository.removeMedicineTag(medicineId, tagId)
        }
    }
}
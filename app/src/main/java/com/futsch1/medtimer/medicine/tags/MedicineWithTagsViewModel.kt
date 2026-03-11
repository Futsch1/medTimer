package com.futsch1.medtimer.medicine.tags

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MedicineWithTagsViewModel @Inject constructor(
    application: Application,
    val medicineRepository: MedicineRepository
) : AndroidViewModel(application) {
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
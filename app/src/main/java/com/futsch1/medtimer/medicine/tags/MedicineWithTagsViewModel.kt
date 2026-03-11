package com.futsch1.medtimer.medicine.tags

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Tag
import kotlinx.coroutines.flow.Flow

class MedicineWithTagsViewModel(application: Application) :
    AndroidViewModel(application) {
    val medicineRepository = MedicineRepository(application)
    fun getMedicineWithTags(medicineId: Int): Flow<FullMedicine?> =
        medicineRepository.getMedicineFlow(medicineId)

    val tags: Flow<List<Tag>> = medicineRepository.tagsFlow

    fun associateTag(medicineId: Int, tagId: Int) {
        medicineRepository.insertMedicineToTag(medicineId, tagId)
    }

    fun disassociateTag(medicineId: Int, tagId: Int) {
        medicineRepository.deleteMedicineToTag(medicineId, tagId)
    }
}
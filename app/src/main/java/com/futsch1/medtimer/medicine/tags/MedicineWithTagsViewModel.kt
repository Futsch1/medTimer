package com.futsch1.medtimer.medicine.tags

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineWithTags
import com.futsch1.medtimer.database.Tag

class MedicineWithTagsViewModel(application: Application) :
    AndroidViewModel(application) {
    val medicineRepository = MedicineRepository(application)
    fun getMedicineWithTags(medicineId: Int): LiveData<MedicineWithTags> =
        medicineRepository.getLiveMedicineWithTags(medicineId)

    val tags: LiveData<List<Tag>> = medicineRepository.liveTags

    fun associateTag(medicineId: Int, tagId: Int) {
        medicineRepository.insertMedicineToTag(medicineId, tagId)
    }

    fun disassociateTag(medicineId: Int, tagId: Int) {
        medicineRepository.deleteMedicineToTag(medicineId, tagId)
    }
}
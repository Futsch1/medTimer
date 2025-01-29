package com.futsch1.medtimer.medicine.tags

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineWithTags
import com.futsch1.medtimer.database.Tag

class MedicineWithTagsViewModel(application: Application, val medicineId: Int) :
    AndroidViewModel(application) {
    val medicineRepository = MedicineRepository(application)
    val medicineWithTags: LiveData<MedicineWithTags> =
        medicineRepository.getLiveMedicineWithTags(medicineId)
    val tags: LiveData<List<Tag>> = medicineRepository.liveTags

    fun associateTag(tagId: Int) {
        medicineRepository.insertMedicineToTag(medicineId, tagId)
    }

    fun disassociateTag(tagId: Int) {
        medicineRepository.deleteMedicineToTag(medicineId, tagId)
    }

    class Factory(private val application: Application, private val factoryMedicineId: Int) :
        ViewModelProvider.Factory {
        @Suppress("kotlin:S6530", "UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MedicineWithTagsViewModel::class.java)) {
                return MedicineWithTagsViewModel(application, factoryMedicineId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
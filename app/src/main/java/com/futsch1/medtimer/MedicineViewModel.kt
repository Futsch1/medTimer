package com.futsch1.medtimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineToTag
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.medicine.tags.TagFilterStore
import java.util.stream.Collectors

class MedicineViewModel(application: Application) : AndroidViewModel(application) {
    @JvmField
    val medicineRepository: MedicineRepository = MedicineRepository(application)
    private val liveMedicines: LiveData<List<FullMedicine>> =
        medicineRepository.liveMedicines

    val validTagIds: MutableLiveData<Set<Int>> = MutableLiveData()
    val tagFilterStore = TagFilterStore(application, validTagIds)
    private lateinit var medicineToTags: List<MedicineToTag>

    private val filteredMedicine = MediatorLiveData<List<FullMedicine>>()
    val medicines: LiveData<List<FullMedicine>> = filteredMedicine

    private val liveScheduledReminders: MutableLiveData<List<ScheduledReminder>> = MutableLiveData()
    private val filteredScheduledReminders = MediatorLiveData<List<ScheduledReminder>>()
    val scheduledReminders = filteredScheduledReminders

    init {
        medicineRepository.liveMedicineToTags.observeForever {
            medicineToTags = it
        }

        filteredMedicine.addSource(liveMedicines) {
            filteredMedicine.value = getFiltered(liveMedicines, validTagIds)
        }
        filteredMedicine.addSource(validTagIds) {
            filteredMedicine.value = getFiltered(liveMedicines, validTagIds)
        }

        filteredScheduledReminders.addSource(liveScheduledReminders) {
            filteredScheduledReminders.value =
                getFiltered(liveScheduledReminders, validTagIds)
        }
        filteredScheduledReminders.addSource(validTagIds) {
            filteredScheduledReminders.value =
                getFiltered(liveScheduledReminders, validTagIds)
        }
    }

    private fun <T : Any> getFiltered(
        liveData: LiveData<List<T>>,
        validTagIds: MutableLiveData<Set<Int>>
    ): List<T> {
        if (liveData.value.isNullOrEmpty()) {
            return emptyList()
        }
        if (validTagIds.value.isNullOrEmpty()) {
            return liveData.value!!
        }
        return liveData.value!!.stream()
            .filter { medicine ->
                filterMedicineId(
                    getId(medicine),
                    validTagIds.value
                )
            }
            .collect(Collectors.toList())
    }

    private fun getId(medicineWithReminder: Any): Int {
        return if (medicineWithReminder is FullMedicine) {
            medicineWithReminder.medicine.medicineId
        } else {
            (medicineWithReminder as ScheduledReminder).medicine.medicine.medicineId
        }
    }

    private fun filterMedicineId(medicineId: Int, validTagIds: Set<Int>? = null): Boolean {
        if (validTagIds.isNullOrEmpty()) {
            return true
        }
        val medicineTags: List<Int> = medicineToTags.stream()
            .filter { medicineToTag -> medicineToTag.medicineId == medicineId }
            .map { medicineToTag -> medicineToTag.tagId }
            .collect(Collectors.toList())
        for (tagId in validTagIds) {
            if (medicineTags.contains(tagId)) {
                return true
            }
        }
        return false
    }

    fun getLiveReminderEvents(
        limit: Int,
        timeStamp: Long,
        withDeleted: Boolean
    ): LiveData<List<ReminderEvent>> {
        return medicineRepository.getLiveReminderEvents(limit, timeStamp, withDeleted)
    }

    fun setScheduledReminders(scheduledReminders: List<ScheduledReminder>) {
        this.liveScheduledReminders.value = scheduledReminders
    }
}
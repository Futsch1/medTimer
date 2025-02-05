package com.futsch1.medtimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineToTag
import com.futsch1.medtimer.database.MedicineWithReminders
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.medicine.tags.TagFilterStore
import java.util.stream.Collectors

class MedicineViewModel(application: Application) : AndroidViewModel(application) {
    @JvmField
    val medicineRepository: MedicineRepository = MedicineRepository(application)
    private val liveMedicines: LiveData<List<MedicineWithReminders>> =
        medicineRepository.liveMedicines

    val validTagIds: MutableLiveData<Set<Int>> = MutableLiveData()
    val tagFilterStore = TagFilterStore(application, validTagIds)
    private lateinit var medicineToTags: List<MedicineToTag>

    private val filteredMedicine = MediatorLiveData<List<MedicineWithReminders>>()
    val medicines: LiveData<List<MedicineWithReminders>> = filteredMedicine

    private val liveScheduledReminders: MutableLiveData<List<ScheduledReminder>> = MutableLiveData()
    private val filteredScheduledReminders = MediatorLiveData<List<ScheduledReminder>>()
    val scheduledReminders = filteredScheduledReminders

    init {
        medicineRepository.liveMedicineToTags.observeForever {
            medicineToTags = it
        }

        filteredMedicine.addSource(liveMedicines) {
            filteredMedicine.value = getFilteredMedicine(liveMedicines, validTagIds)
        }
        filteredMedicine.addSource(validTagIds) {
            filteredMedicine.value = getFilteredMedicine(liveMedicines, validTagIds)
        }

        filteredScheduledReminders.addSource(liveScheduledReminders) {
            filteredScheduledReminders.value =
                getFilteredScheduledReminders(liveScheduledReminders, validTagIds)
        }
        filteredScheduledReminders.addSource(validTagIds) {
            filteredScheduledReminders.value =
                getFilteredScheduledReminders(liveScheduledReminders, validTagIds)
        }
    }

    private fun getFilteredScheduledReminders(
        liveData: MutableLiveData<List<ScheduledReminder>>,
        validTagIds: MutableLiveData<Set<Int>>
    ): List<ScheduledReminder> {
        if (liveData.value.isNullOrEmpty()) {
            return emptyList()
        }
        if (validTagIds.value.isNullOrEmpty()) {
            return liveData.value!!
        }
        return liveData.value!!.stream()
            .filter { medicine ->
                filterMedicineId(
                    medicine.medicine.medicineId,
                    validTagIds.value
                )
            }
            .collect(Collectors.toList())
    }

    private fun getFilteredMedicine(
        liveData: LiveData<List<MedicineWithReminders>>,
        validTagIds: MutableLiveData<Set<Int>>
    ): List<MedicineWithReminders> {
        if (liveData.value.isNullOrEmpty()) {
            return emptyList()
        }
        if (validTagIds.value.isNullOrEmpty()) {
            return liveData.value!!
        }
        return liveData.value!!.stream()
            .filter { medicine ->
                filterMedicineId(
                    medicine.medicine.medicineId,
                    validTagIds.value
                )
            }
            .collect(Collectors.toList())
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
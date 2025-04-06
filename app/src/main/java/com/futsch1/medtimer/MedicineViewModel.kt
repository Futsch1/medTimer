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
import com.futsch1.medtimer.database.Tag
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
    private val liveTags = medicineRepository.liveTags

    private val filteredMedicine = MediatorLiveData<List<FullMedicine>>()
    val medicines: LiveData<List<FullMedicine>> = filteredMedicine

    private val liveScheduledReminders: MutableLiveData<List<ScheduledReminder>> = MutableLiveData()
    private val filteredScheduledReminders = MediatorLiveData<List<ScheduledReminder>>()
    val scheduledReminders = filteredScheduledReminders

    private val filteredReminderEvents = MediatorLiveData<List<ReminderEvent>>()
    private lateinit var liveReminderEvents: LiveData<List<ReminderEvent>>


    init {
        medicineRepository.liveMedicineToTags.observeForever {
            medicineToTags = it
        }

        filteredMedicine.addSource(liveMedicines) {
            if (validTagIds.value != null) {
                filteredMedicine.value = getFiltered(it, validTagIds.value!!)
            }
        }
        filteredMedicine.addSource(validTagIds) {
            if (liveMedicines.value != null) {
                filteredMedicine.value = getFiltered(liveMedicines.value!!, it)
            }
        }

        filteredScheduledReminders.addSource(liveScheduledReminders) {
            if (validTagIds.value != null) {
                filteredScheduledReminders.value =
                    getFiltered(it, validTagIds.value!!)
            }
        }
        filteredScheduledReminders.addSource(validTagIds) {
            if (liveScheduledReminders.value != null) {
                filteredScheduledReminders.value =
                    getFiltered(liveScheduledReminders.value!!, it)
            }
        }

        filteredReminderEvents.addSource(validTagIds) {
            if (liveReminderEvents.isInitialized && liveReminderEvents.value != null && liveTags.value != null) {
                filteredReminderEvents.value =
                    getFilteredEvents(liveReminderEvents.value!!, it, liveTags.value!!)
            }
        }
        filteredReminderEvents.addSource(liveTags) {
            if (liveReminderEvents.isInitialized && liveReminderEvents.value != null && validTagIds.value != null) {
                filteredReminderEvents.value =
                    getFilteredEvents(liveReminderEvents.value!!, validTagIds.value!!, it)
            }
        }
    }

    private fun <T : Any> getFiltered(
        liveData: List<T>,
        validTagIds: Set<Int>
    ): List<T> {
        if (validTagIds.isEmpty()) {
            return liveData
        }
        return liveData.stream()
            .filter { medicine ->
                filterMedicineId(
                    getId(medicine),
                    validTagIds
                )
            }
            .collect(Collectors.toList())
    }

    private fun getFilteredEvents(
        liveData: List<ReminderEvent>,
        validTagIds: Set<Int>,
        liveTags: MutableList<Tag>
    ): List<ReminderEvent> {
        if (validTagIds.isEmpty()) {
            return liveData
        }
        // Get all valid tag names from the list of valid IDs
        val validTagNames =
            liveTags.stream().filter { tag -> validTagIds.contains(tag.tagId) }
                .map { tag -> tag.name }.collect(Collectors.toSet())
        // Now filter all reminder events and check if they contain any of the valid tags
        return liveData.stream().filter { reminderEvent ->
            validTagNames.stream().filter { reminderEvent.tags.contains(it) }.count() > 0
        }.collect(Collectors.toList())

    }

    private fun getId(medicineWithReminder: Any): Int {
        return if (medicineWithReminder is FullMedicine) {
            medicineWithReminder.medicine.medicineId
        } else {
            (medicineWithReminder as ScheduledReminder).medicine.medicine.medicineId
        }
    }

    private fun filterMedicineId(medicineId: Int, validTagIds: Set<Int>? = null): Boolean {
        if (validTagIds.isNullOrEmpty() || !this::medicineToTags.isInitialized) {
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
        timeStamp: Long,
        withDeleted: Boolean
    ): LiveData<List<ReminderEvent>> {
        liveReminderEvents = medicineRepository.getLiveReminderEvents(timeStamp, withDeleted)
        filteredReminderEvents.addSource(liveReminderEvents) {
            if (validTagIds.value != null && liveTags.value != null) {
                filteredReminderEvents.value =
                    getFilteredEvents(it, validTagIds.value!!, liveTags.value!!)
            }
        }
        return filteredReminderEvents
    }

    fun setScheduledReminders(scheduledReminders: List<ScheduledReminder>) {
        this.liveScheduledReminders.postValue(scheduledReminders)
    }
}
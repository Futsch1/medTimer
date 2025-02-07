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

        filteredReminderEvents.addSource(validTagIds) {
            filteredReminderEvents.value =
                getFilteredEvents(liveReminderEvents, validTagIds, liveTags)
        }
        filteredReminderEvents.addSource(liveTags) {
            filteredReminderEvents.value =
                getFilteredEvents(liveReminderEvents, validTagIds, liveTags)
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

    private fun getFilteredEvents(
        liveData: LiveData<List<ReminderEvent>>,
        validTagIds: MutableLiveData<Set<Int>>,
        liveTags: LiveData<MutableList<Tag>>
    ): List<ReminderEvent> {
        if (!liveData.isInitialized || liveData.value.isNullOrEmpty()) {
            return emptyList()
        }
        if (validTagIds.value.isNullOrEmpty() || liveTags.value.isNullOrEmpty()) {
            return liveData.value!!
        }
        // Get all valid tag names from the list of valid IDs
        val validTagNames =
            liveTags.value!!.stream().filter { tag -> validTagIds.value!!.contains(tag.tagId) }
                .map { tag -> tag.name }.collect(Collectors.toSet())
        // Now filter all reminder events and check if they contain any of the valid tags
        return liveData.value!!.stream().filter { reminderEvent ->
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
        liveReminderEvents = medicineRepository.getLiveReminderEvents(limit, timeStamp, withDeleted)
        filteredReminderEvents.addSource(liveReminderEvents) {
            filteredReminderEvents.value =
                getFilteredEvents(liveReminderEvents, validTagIds, liveTags)
        }
        return filteredReminderEvents
    }

    fun setScheduledReminders(scheduledReminders: List<ScheduledReminder>) {
        this.liveScheduledReminders.value = scheduledReminders
    }
}
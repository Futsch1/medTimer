package com.futsch1.medtimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineToTag
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.database.allStatusValues
import com.futsch1.medtimer.medicine.tags.TagFilterStore
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.stream.Collectors

class MedicineViewModel(application: Application) : AndroidViewModel(application) {
    val medicineRepository: MedicineRepository = MedicineRepository(application)
    private val liveMedicines = medicineRepository.medicinesFlow

    val validTagIds = MutableStateFlow<Set<Int>?>(null)
    val tagFilterStore = TagFilterStore(application, validTagIds)
    private var medicineToTags: List<MedicineToTag> = emptyList()
    private val liveTags: StateFlow<List<Tag>> = medicineRepository.tagsFlow.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    private val _scheduledReminders = MutableStateFlow<List<ScheduledReminder>>(emptyList())

    val medicines: StateFlow<List<FullMedicine>> = combine(liveMedicines, validTagIds) { medicines, tagIds ->
        getFiltered(medicines, tagIds ?: emptySet())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val scheduledReminders: SharedFlow<List<ScheduledReminder>> = combine(_scheduledReminders, validTagIds) { reminders, tagIds ->
        getFiltered(reminders, tagIds ?: emptySet())
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        viewModelScope.launch {
            medicineRepository.medicineToTagsFlow.collect {
                medicineToTags = it
            }
        }

        viewModelScope.launch {
            medicineRepository.tagsFlow.collect {
                tagFilterStore.filterForDeletedTags(it)
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

    fun filterEvents(events: List<ReminderEvent>): List<ReminderEvent> {
        return getFilteredEvents(events, validTagIds.value ?: setOf(), liveTags.value)
    }

    fun filterMedicines(medicines: List<FullMedicine>): List<FullMedicine> {
        return getFiltered(medicines, validTagIds.value ?: emptySet())
    }

    fun tagFilterActive(): Boolean {
        return validTagIds.value?.isNotEmpty() ?: return false
    }

    private fun getFilteredEvents(
        liveData: List<ReminderEvent>,
        validTagIds: Set<Int>,
        liveTags: List<Tag>
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
        if (validTagIds.isNullOrEmpty() || medicineToTags.isEmpty()) {
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
        statusValues: List<ReminderStatus> = allStatusValues
    ): Flow<List<ReminderEvent>> {
        return combine(
            medicineRepository.getReminderEventsFlow(timeStamp, statusValues),
            validTagIds,
            liveTags
        ) { events, tagIds, tags ->
            if (tagIds.isNullOrEmpty()) events else getFilteredEvents(events, tagIds, tags)
        }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    }

    fun setScheduledReminders(scheduledReminders: List<ScheduledReminder>) {
        _scheduledReminders.value = scheduledReminders
    }
}

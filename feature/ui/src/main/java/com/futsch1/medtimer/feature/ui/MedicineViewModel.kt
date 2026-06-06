package com.futsch1.medtimer.feature.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.MedicineToTag
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.model.Tag
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.feature.reminders.FutureRemindersRepository
import com.futsch1.medtimer.feature.ui.medicine.tags.TagFilterStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.stream.Collectors
import javax.inject.Inject

@HiltViewModel
class MedicineViewModel @Inject constructor(
    persistentDataDataSource: PersistentDataDataSource,
    medicineRepository: MedicineRepository,
    private val tagRepository: TagRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val futureRemindersRepository: FutureRemindersRepository,
) : ViewModel() {
    private val liveMedicines = medicineRepository.getAllFlow()

    private val _validTagIds = MutableStateFlow<Set<Int>?>(null)
    val validTagIds: StateFlow<Set<Int>?> = _validTagIds.asStateFlow()
    val tagFilterStore = TagFilterStore(persistentDataDataSource, _validTagIds)

    fun clearTagFilter() {
        _validTagIds.value = setOf()
    }

    private var medicineToTags: List<MedicineToTag> = emptyList()
    private val liveTags: StateFlow<List<Tag>> = tagRepository.getAllFlow().stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    private val _scheduledReminders = MutableStateFlow<List<ScheduledReminder>>(emptyList())

    val medicines: StateFlow<List<Medicine>> =
        combine(liveMedicines, validTagIds) { medicines, tagIds ->
            getFiltered(medicines, tagIds ?: emptySet())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val scheduledReminders: SharedFlow<List<ScheduledReminder>> =
        combine(_scheduledReminders, validTagIds) { reminders, tagIds ->
            getFiltered(reminders, tagIds ?: emptySet())
        }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        viewModelScope.launch {
            tagRepository.getMedicineTagsFlow().collect {
                medicineToTags = it
            }
        }

        viewModelScope.launch {
            tagRepository.getAllFlow().collect {
                tagFilterStore.filterForDeletedTags(it)
            }
        }

        viewModelScope.launch {
            futureRemindersRepository.simulatedReminders.collect { reminders ->
                _scheduledReminders.value = reminders
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

    fun filterMedicines(medicines: List<Medicine>): List<Medicine> {
        return getFiltered(medicines, validTagIds.value ?: emptySet())
    }

    fun tagFilterActive(): Boolean {
        return validTagIds.value?.isNotEmpty() ?: false
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
            liveTags.stream().filter { tag -> validTagIds.contains(tag.id) }
                .map { tag -> tag.name }.collect(Collectors.toSet())
        // Now filter all reminder events and check if they contain any of the valid tags
        return liveData.stream().filter { reminderEvent ->
            validTagNames.stream().filter { reminderEvent.tags.contains(it) }.count() > 0
        }.collect(Collectors.toList())
    }

    private fun getId(medicineWithReminder: Any): Int {
        return if (medicineWithReminder is Medicine) {
            medicineWithReminder.id
        } else {
            (medicineWithReminder as ScheduledReminder).medicine.id
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
        startInstant: Instant,
        statusValues: List<ReminderEvent.ReminderStatus> = ReminderEvent.allStatusValues
    ): Flow<List<ReminderEvent>> {
        return combine(
            reminderEventRepository.getAllFlow(startInstant, statusValues),
            validTagIds,
            liveTags
        ) { events, tagIds, tags ->
            if (tagIds.isNullOrEmpty()) events else getFilteredEvents(events, tagIds, tags)
        }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    }


}

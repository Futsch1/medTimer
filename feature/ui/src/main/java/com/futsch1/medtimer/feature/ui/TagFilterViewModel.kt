package com.futsch1.medtimer.feature.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.MedicineToTag
import com.futsch1.medtimer.core.domain.model.SimulatedReminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.Tag
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.feature.ui.medicine.tags.TagFilterStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.stream.Collectors
import javax.inject.Inject

@HiltViewModel
class TagFilterViewModel @Inject constructor(
    persistentDataDataSource: PersistentDataDataSource,
    private val tagRepository: TagRepository,
) : ViewModel() {
    private val _validTagIds = MutableStateFlow<Set<Int>?>(null)
    val validTagIds: StateFlow<Set<Int>?> = _validTagIds.asStateFlow()
    val tagFilterStore = TagFilterStore(persistentDataDataSource, _validTagIds)

    fun clearTagFilter() {
        _validTagIds.value = setOf()
    }

    val tagsSelected: StateFlow<Boolean> =
        validTagIds.map { it?.isNotEmpty() ?: false }.stateIn(
            viewModelScope, SharingStarted.Eagerly, false
        )

    val hasAnyTags: StateFlow<Boolean> = tagRepository.hasAny()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var medicineToTags: List<MedicineToTag> = emptyList()
    val liveTags: StateFlow<List<Tag>> = tagRepository.getAllFlow().stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )


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
    }

    fun <T : Any> getFiltered(
        liveData: List<T>,
        validTagIds: Set<Int>
    ): List<T> {
        if (validTagIds.isEmpty()) {
            return liveData
        }
        return liveData.filter { medicine ->
            filterMedicineId(
                getId(medicine),
                validTagIds
            )
        }
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

    fun getFilteredEvents(
        liveData: List<ReminderEvent>,
        validTagIds: Set<Int>?,
        liveTags: List<Tag>
    ): List<ReminderEvent> {
        if (validTagIds.isNullOrEmpty()) {
            return liveData
        }
        // Get all valid tag names from the list of valid IDs
        val validTagNames =
            liveTags.filter { tag -> validTagIds.contains(tag.id) }
                .map { tag -> tag.name }.toSet()
        // Now filter all reminder events and check if they contain any of the valid tags
        return liveData.filter { reminderEvent ->
            validTagNames.any { reminderEvent.tags.contains(it) }
        }
    }

    private fun getId(medicineWithReminder: Any): Int {
        return if (medicineWithReminder is Medicine) {
            medicineWithReminder.id
        } else {
            (medicineWithReminder as SimulatedReminder).scheduledReminder.medicine.id
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

}

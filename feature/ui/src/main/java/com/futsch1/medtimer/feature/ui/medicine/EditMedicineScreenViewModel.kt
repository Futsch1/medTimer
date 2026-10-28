package com.futsch1.medtimer.feature.ui.medicine

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.setRemindersActive
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.ReminderSummaryFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import javax.inject.Inject

@HiltViewModel
class EditMedicineScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val tagRepository: TagRepository,
    private val reminderSummaryFormatter: ReminderSummaryFormatter,
    private val linkedReminderAlgorithms: LinkedReminderAlgorithms,
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    val medicineId: Int = checkNotNull(savedStateHandle["medicineId"])
    val medicine = medicineRepository.getFlow(medicineId)

    private val editMutex = Mutex()

    val state: EditMedicineScreenState
        field = MutableEditMedicineScreenState()

    val fragmentState: EditMedicineFragmentState
        field = MutableEditMedicineFragmentState()

    init {
        viewModelScope.launch {
            medicine.collect { medicine ->
                fragmentState.medicine = medicine
                if (medicine != null) {
                    val summaries = medicine.reminders.associate { reminder ->
                        reminder.id to reminderSummaryFormatter.formatReminderSummary(reminder)
                    }
                    setupState(medicine, summaries)
                }
            }
        }
    }

    private fun setupState(medicine: Medicine, summaries: Map<Int, String>) {
        state.id = medicine.id
        state.icon = medicine.iconId
        state.name = medicine.name
        state.color = if (medicine.useColor) medicine.color else null

        state.reminders = linkedReminderAlgorithms.sortRemindersList(medicine.reminders).map { reminder ->
            ReminderItem(
                id = reminder.id,
                timeVisible = reminder.usesTimeInMinutes,
                timeLabel = if (reminder.reminderType != ReminderType.LINKED) R.string.time else R.string.delay,
                timeHint = if (reminder.reminderType == ReminderType.LINKED) R.string.linked_reminder_delay else null,
                timeMinutesOfDay = reminder.time.minutes,
                amount = reminder.amount,
                type = reminder.reminderType,
                summary = summaries[reminder.id] ?: "?"
            )
        }.toImmutableList()
    }

    fun setRemindersActive(active: Boolean) = enqueueEdit {
        setRemindersActive(reminderRepository.getAll(medicineId), reminderRepository, active)
    }

    fun duplicate(finishCallback: () -> Unit = {}) = enqueueEdit(finishCallback) {
        val medicine = medicineRepository.fetch(medicineId) ?: return@enqueueEdit
        val newMedicineId = medicineRepository.create(duplicateOf(medicine, ::copyName))
        assignTags(medicineId, newMedicineId)
    }

    fun duplicateIncludingReminders(finishCallback: () -> Unit = {}) = enqueueEdit(finishCallback) {
        val medicine = medicineRepository.fetch(medicineId) ?: return@enqueueEdit
        val newMedicineId = medicineRepository.create(duplicateOf(medicine, ::copyName))
        reminderRepository.createMany(medicine.reminders.map { it.copy(id = 0, medicineRelId = newMedicineId) })
        assignTags(medicineId, newMedicineId)
    }

    fun delete(finishCallback: () -> Unit = {}) = enqueueEdit(finishCallback) {
        medicineRepository.delete(medicineId)
    }

    fun deleteReminder(id: Int) = enqueueEdit { reminderRepository.delete(id) }

    fun editName(name: String) = enqueueEdit {
        val medicine = medicineRepository.fetch(medicineId) ?: return@enqueueEdit
        medicineRepository.update(medicine.copy(name = name))
    }

    fun editReminderAmount(id: Int, amount: String) = enqueueEdit {
        val reminder = reminderRepository.fetch(id) ?: return@enqueueEdit
        reminderRepository.update(reminder.copy(amount = amount))
    }

    fun editReminderTime(id: Int, timeMinutesOfDay: Int) = enqueueEdit {
        val reminder = reminderRepository.fetch(id) ?: return@enqueueEdit
        if (timeMinutesOfDay >= 0 && reminder.usesTimeInMinutes) {
            reminderRepository.update(reminder.copy(time = reminder.time.copy(minutes = timeMinutesOfDay)))
        }
    }

    fun editIcon(iconId: Int) = enqueueEdit {
        val medicine = medicineRepository.fetch(medicineId) ?: return@enqueueEdit
        medicineRepository.update(medicine.copy(iconId = iconId))
    }

    fun editNotes(notes: String) = enqueueEdit {
        val medicine = medicineRepository.fetch(medicineId) ?: return@enqueueEdit
        medicineRepository.update(medicine.copy(notes = notes))
    }

    private fun enqueueEdit(onComplete: () -> Unit = {}, block: suspend () -> Unit) {
        applicationScope.launch(start = CoroutineStart.UNDISPATCHED) {
            editMutex.withLock {
                // Acquire in call order, then leave the caller thread before running the edit.
                yield()
                block()
            }
            viewModelScope.launch { onComplete() }
        }
    }

    private fun copyName(name: String) = context.getString(R.string.duplicate_name, name)

    private suspend fun assignTags(oldMedicineId: Int, newMedicineId: Int) {
        val oldFullMedicine = medicineRepository.fetch(oldMedicineId) ?: return
        for ((_, id) in oldFullMedicine.tags) {
            tagRepository.addMedicineTag(newMedicineId, id)
        }
    }
}
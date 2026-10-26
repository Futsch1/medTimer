package com.futsch1.medtimer.feature.ui.medicine

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.setRemindersActive
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.ReminderSummaryFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditMedicineScreenViewModel @AssistedInject constructor(
    @Assisted val medicineId: Int,
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val tagRepository: TagRepository,
    private val reminderSummaryFormatter: ReminderSummaryFormatter,
    @param:ApplicationContext private val context: Context,
    @Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    @AssistedFactory
    fun interface Factory {
        fun create(medicineId: Int): EditMedicineScreenViewModel
    }

    val medicine = medicineRepository.getFlow(medicineId)
    private val reminderSummaries: MutableSharedFlow<Map<Int, String>> = MutableSharedFlow()

    val state: EditMedicineScreenState
        field = MutableEditMedicineScreenState()

    init {
        viewModelScope.launch {
            medicine.collect {
                if (it != null) {
                    val summaries: MutableMap<Int, String> = mutableMapOf()
                    for (reminder in it.reminders) {
                        summaries[reminder.id] = reminderSummaryFormatter.formatReminderSummary(reminder)
                    }
                    reminderSummaries.emit(summaries)
                }
            }
        }

        combine(medicine, reminderSummaries) { medicine, summaries ->
            if (medicine == null) return@combine
            setupState(medicine, summaries)
        }.launchIn(viewModelScope)
    }

    private fun setupState(medicine: Medicine, summaries: Map<Int, String>) {
        state.id = medicine.id
        state.icon = medicine.iconId
        state.name = medicine.name
        state.color = medicine.color
        state.reminders = medicine.reminders.map { reminder ->
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

    fun setRemindersActive(active: Boolean) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val reminders = reminderRepository.getAll(medicineId)
                setRemindersActive(
                    reminders,
                    reminderRepository,
                    active
                )
            }
        }
    }

    fun duplicate(finishCallback: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val medicine = medicineRepository.fetch(medicineId) ?: return@withContext
                val newMedicineId = medicineRepository.create(duplicateOf(medicine, ::copyName))
                assignTags(medicineId, newMedicineId)
            }
            finishCallback()
        }
    }

    fun duplicateIncludingReminders(finishCallback: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val medicine = medicineRepository.fetch(medicineId) ?: return@withContext
                val newMedicineId = medicineRepository.create(duplicateOf(medicine, ::copyName))
                reminderRepository.createMany(medicine.reminders.map {
                    it.copy(
                        id = 0,
                        medicineRelId = newMedicineId
                    )
                })
                assignTags(medicineId, newMedicineId)
            }
            finishCallback()
        }
    }

    fun delete(finishCallback: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                medicineRepository.delete(medicineId)
            }
            finishCallback()
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
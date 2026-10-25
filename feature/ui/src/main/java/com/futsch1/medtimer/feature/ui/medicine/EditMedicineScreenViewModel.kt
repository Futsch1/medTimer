package com.futsch1.medtimer.feature.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.ReminderSummaryFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class EditMedicineScreenViewModel @AssistedInject constructor(
    @Assisted val medicineId: Int,
    medicineRepository: MedicineRepository,
    private val reminderSummaryFormatter: ReminderSummaryFormatter
) : ViewModel() {

    @AssistedFactory
    fun interface Factory {
        fun create(medicineId: Int): EditMedicineScreenViewModel
    }

    private val medicine = medicineRepository.getFlow(medicineId)
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
}
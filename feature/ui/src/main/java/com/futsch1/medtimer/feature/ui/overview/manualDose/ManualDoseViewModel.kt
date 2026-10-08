package com.futsch1.medtimer.feature.ui.overview.manualDose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class ManualDoseMedicineEntry(val label: String, val medicine: Medicine?)

@HiltViewModel
class ManualDoseViewModel @Inject constructor(
    medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val persistentDataDataSource: PersistentDataDataSource,
    private val commandBus: ReminderCommandBus,
    private val timeFormatter: TimeFormatter
) : ViewModel() {
    private val medicineList: Flow<List<Medicine>> = medicineRepository.getAllFlow()

    /** Snapshot state, so the search field reads every keystroke back immediately. */
    var searchQuery by mutableStateOf("")
        private set

    fun search(query: String) {
        searchQuery = query
    }

    private data class MedicineChoices(val entries: List<ManualDoseMedicineEntry>, val showSearch: Boolean)

    private val manualDoseMedicines: Flow<MedicineChoices> =
        combine(medicineList, snapshotFlow { searchQuery.trim() }) { medicines, query ->
            MedicineChoices(
                entries = medicines.filter { it.name.contains(query, ignoreCase = true) }.map { ManualDoseMedicineEntry(it.name, it) },
                showSearch = medicines.size > SEARCH_MIN_MEDICINES,
            )
        }

    private val lastCustomMedicineEntry: Flow<ManualDoseMedicineEntry> =
        persistentDataDataSource.data.map { data -> ManualDoseMedicineEntry(data.lastCustomDose, null) }

    private val selectedManualDoseEntry: MutableStateFlow<ManualDoseMedicineEntry> =
        MutableStateFlow(ManualDoseMedicineEntry("", null))

    fun selectMedicineEntry(manualDoseMedicineEntry: ManualDoseMedicineEntry) {
        selectedManualDoseEntry.value = manualDoseMedicineEntry
        step.value = ManualDoseStep.AMOUNT
    }

    private val _reminderList: Flow<List<Reminder>> = selectedManualDoseEntry.map {
        val medicine = it.medicine
        if (medicine == null) {
            emptyList()
        } else {
            reminderRepository.getAll(medicine.id)
        }
    }

    private val manualDoseAmounts: Flow<List<String>> = combine(
        _reminderList,
        selectedManualDoseEntry
    ) { reminders, manualDoseEntry ->
        val medicine = manualDoseEntry.medicine
        if (medicine == null) {
            emptyList()
        } else {
            reminders.map { it.amount }.distinct()
        }
    }

    private val lastCustomDoseAmount: Flow<String> =
        persistentDataDataSource.data.map { data -> data.lastCustomDoseAmount }

    fun selectAmount(amount: String) {
        selectedManualDoseAmount.value = amount
        step.value = ManualDoseStep.TIME
    }

    private val selectedManualDoseAmount: MutableStateFlow<String> = MutableStateFlow("")

    fun goOneStepBack() {
        when (step.value) {
            ManualDoseStep.MEDICINE -> {
                // Nothing to do
            }

            ManualDoseStep.AMOUNT -> {
                step.value = ManualDoseStep.MEDICINE
            }

            ManualDoseStep.TIME -> {
                step.value = ManualDoseStep.AMOUNT
            }
        }
    }

    private val step: MutableStateFlow<ManualDoseStep> = MutableStateFlow(ManualDoseStep.MEDICINE)

    private val availableState: Flow<ManualDoseState> = combine(
        manualDoseMedicines,
        lastCustomMedicineEntry,
        manualDoseAmounts,
        lastCustomDoseAmount
    ) { medicines, lastCustomMedicineEntry, amounts, lastCustomDoseAmount ->
        ManualDoseState(
            medicineEntries = medicines.entries.toImmutableList(),
            showSearch = medicines.showSearch,
            lastCustomMedicineEntry = lastCustomMedicineEntry,
            amounts = amounts.toImmutableList(),
            lastCustomDoseAmount = lastCustomDoseAmount,
        )
    }

    private val selectedDay: MutableStateFlow<LocalDate> = MutableStateFlow(LocalDate.now())

    fun selectDay(day: LocalDate) {
        selectedDay.value = day
    }

    val state: Flow<ManualDoseState> = combine(
        availableState,
        selectedManualDoseEntry,
        selectedManualDoseAmount,
        selectedDay,
        step
    ) { available, selectedEntry, selectedAmount, selectedDay, step ->
        available.copy(
            selectedMedicine = selectedEntry,
            selectedAmount = selectedAmount,
            selectedDay = if (selectedDay != LocalDate.now()) timeFormatter.localDateToString(selectedDay) else null,
            step = step,
        )
    }

    fun selectTimeAndLog(doseLocalTime: LocalTime) {
        val selectedEntry = selectedManualDoseEntry.value
        val amount = selectedManualDoseAmount.value
        val doseDateTime = selectedDay.value.atTime(doseLocalTime)
        val doseInstant = doseDateTime.toInstant(ZoneId.systemDefault().rules.getOffset(doseDateTime))

        var reminderEvent = ReminderEvent.default().copy(
            reminderId = -1,
            status = ReminderEvent.ReminderStatus.TAKEN,
            medicineName = selectedEntry.label,
            amount = amount,
            remindedTimestamp = doseInstant,
            processedTimestamp = doseInstant
        )
        selectedEntry.medicine?.let { medicine ->
            val stockAmount = medicine.amount.takeIf { medicine.isStockManagementActive() } ?: -1.0
            reminderEvent = reminderEvent.copy(
                color = medicine.color,
                useColor = medicine.useColor,
                iconId = medicine.iconId,
                tags = medicine.tags.map { tag -> tag.name },
                stockBefore = stockAmount,
                stockAfter = stockAmount,
                stockUnit = medicine.unit
            )
        }

        viewModelScope.launch {
            val event = reminderEventRepository.create(reminderEvent)

            selectedEntry.medicine?.let {
                val amount = MedicineHelper.parseAmount(reminderEvent.amount)
                if (amount != null && it.isStockManagementActive()) {
                    commandBus.processStockHandling(amount, it.id, doseInstant.epochSecond)
                        ?.let { stockAfter ->
                            reminderEventRepository.update(
                                event.copy(stockAfter = stockAfter, stockHandled = true)
                            )
                        }
                }
            }
        }

        if (selectedEntry.medicine == null) {
            persistentDataDataSource.setLastCustomDoseAmount(amount)
            persistentDataDataSource.setLastCustomDose(selectedEntry.label)
        }
    }

    fun reset() {
        searchQuery = ""
        selectedManualDoseEntry.value = ManualDoseMedicineEntry("", null)
        selectedManualDoseAmount.value = ""
        step.value = ManualDoseStep.MEDICINE
    }

    private companion object {
        /** About as many medicines as the list shows before it scrolls; below that a search field is just clutter. */
        const val SEARCH_MIN_MEDICINES = 4
    }
}

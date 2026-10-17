package com.futsch1.medtimer.feature.ui.overview.manualDose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.ManualDose
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class ManualDoseMedicineEntry(val label: String, val medicine: Medicine?)
data class ManualDoseMedicineWithAmountEntry(val label: String, val medicine: Medicine?, val amount: String)

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
                entries = medicines.filter { it.name.contains(query, ignoreCase = true) }
                    .map { ManualDoseMedicineEntry(it.name, it) },
                showSearch = medicines.size > SEARCH_MIN_MEDICINES,
            )
        }

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
        manualDoseAmounts,
        medicineList,
        persistentDataDataSource.data
    ) { medicines, amounts, medicineList, persistentData ->
        ManualDoseState(
            medicineEntries = medicines.entries.toImmutableList(),
            showSearch = medicines.showSearch,
            amounts = amounts.toImmutableList(),
            lastCustomDoses = persistentData.lastManualDoses.doses.map {
                ManualDoseMedicineWithAmountEntry(
                    it.medicineLabel ?: "",
                    it.medicineId?.let { id -> medicineList.find { medicine -> medicine.id == id } },
                    it.amount ?: ""
                )
            }.toImmutableList(),
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
        val doseDateTime = selectedDay.value.atTime(doseLocalTime)
        val doseInstant = doseDateTime.toInstant(ZoneId.systemDefault().rules.getOffset(doseDateTime))
        logDose(selectedManualDoseEntry.value, selectedManualDoseAmount.value, doseInstant)
    }

    private fun logDose(selectedEntry: ManualDoseMedicineEntry, amount: String, doseInstant: Instant) {
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

        persistentDataDataSource.addManualDose(ManualDose(selectedEntry.label, selectedEntry.medicine?.id, amount))
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

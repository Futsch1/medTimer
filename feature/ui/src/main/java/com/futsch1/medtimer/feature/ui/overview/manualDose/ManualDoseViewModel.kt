package com.futsch1.medtimer.feature.ui.overview.manualDose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.helpers.DoseAmountParser
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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class ManualDoseMedicineEntry(val label: String, val medicine: Medicine?)
data class ManualDoseMedicineWithAmountEntry(
    val label: String,
    val medicine: Medicine?,
    val amount: String,
    val medicineId: Int? = medicine?.id,
)

data class ManualDoseSaveResult(val event: ReminderEvent?, val day: LocalDate)

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
        combine(medicineList, snapshotFlow { searchQuery.trim() }, persistentDataDataSource.data) { medicines, query, persistentData ->
            val entries = medicines.map { ManualDoseMedicineEntry(it.name, it) }.toMutableList()
            persistentData.lastManualDoses.doses.forEach { dose ->
                val label = dose.medicineLabel?.takeIf(String::isNotBlank) ?: return@forEach
                if (entries.any { it.label.equals(label, ignoreCase = true) }) return@forEach
                val medicine = dose.medicineId?.let { id -> medicines.find { it.id == id } }
                entries += ManualDoseMedicineEntry(label, medicine)
            }
            MedicineChoices(
                entries = entries.filter { it.label.contains(query, ignoreCase = true) },
                showSearch = entries.size > SEARCH_MIN_MEDICINES,
            )
        }

    private val selectedManualDoseEntry: MutableStateFlow<ManualDoseMedicineEntry> =
        MutableStateFlow(ManualDoseMedicineEntry("", null))

    fun selectMedicineEntry(manualDoseMedicineEntry: ManualDoseMedicineEntry): Boolean {
        val label = manualDoseMedicineEntry.label.trim()
        if (label.isEmpty()) return false
        selectedManualDoseEntry.value = manualDoseMedicineEntry.copy(label = label)
        step.value = ManualDoseStep.AMOUNT
        return true
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
        selectedManualDoseEntry,
        persistentDataDataSource.data,
    ) { reminders, manualDoseEntry, persistentData ->
        val medicine = manualDoseEntry.medicine
        val reminderAmounts = if (medicine == null) {
            emptyList()
        } else {
            reminders.filter { !it.isOutOfStockOrExpirationReminder }
                .map { it.amount.trim() }
                .filter(String::isNotEmpty)
        }
        (reminderAmounts + persistentData.lastManualDoses.doses
            .filter { dose ->
                dose.medicineLabel.equals(manualDoseEntry.label, ignoreCase = true) ||
                    (medicine != null && dose.medicineId == medicine.id)
            }
            .mapNotNull { it.amount?.trim()?.takeIf(String::isNotEmpty) })
            .distinct()
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
            lastCustomDoses = persistentData.lastManualDoses.doses.mapNotNull { dose ->
                dose.medicineLabel?.takeIf(String::isNotBlank)?.let { label ->
                    val medicine = dose.medicineId?.let { id -> medicineList.find { it.id == id } }
                    ManualDoseMedicineWithAmountEntry(label, medicine, dose.amount ?: "", medicine?.id)
                }
            }.toImmutableList(),
        )
    }

    private val selectedDateTime: MutableStateFlow<LocalDateTime> = MutableStateFlow(LocalDateTime.now())

    fun setSelectedDateTime(dateTime: LocalDateTime) {
        selectedDateTime.value = dateTime
    }

    val state: Flow<ManualDoseState> = combine(
        availableState,
        selectedManualDoseEntry,
        selectedManualDoseAmount,
        selectedDateTime,
        step,
    ) { available, selectedEntry, selectedAmount, selectedDateTime, step ->
        available.copy(
            selectedMedicine = selectedEntry,
            selectedAmount = selectedAmount,
            selectedDateTime = timeFormatter.toDateTimeString(selectedDateTime),
            step = step,
        )
    }

    fun selectTimeAndLog(doseLocalTime: LocalTime?, onResult: (ManualDoseSaveResult) -> Unit) {
        val selectedDoseDateTime = selectedDateTime.value
        submitDose(
            selectedManualDoseEntry.value,
            selectedManualDoseAmount.value,
            doseLocalTime?.let { selectedDoseDateTime.with(it) } ?: selectedDoseDateTime,
            onResult,
        )
    }

    fun logRecentDose(dose: ManualDoseMedicineWithAmountEntry, onResult: (ManualDoseSaveResult) -> Unit) {
        submitDose(
            ManualDoseMedicineEntry(dose.label, dose.medicine),
            dose.amount,
            selectedDateTime.value,
            onResult,
            dose.medicineId,
        )
    }

    private fun submitDose(
        selectedEntry: ManualDoseMedicineEntry,
        amount: String,
        doseDateTime: LocalDateTime,
        onResult: (ManualDoseSaveResult) -> Unit,
        medicineId: Int? = selectedEntry.medicine?.id,
    ) {
        val label = selectedEntry.label.trim()
        val entry = selectedEntry.copy(label = label)
        val trimmedAmount = amount.trim()
        val doseInstant = doseDateTime.toInstant(ZoneId.systemDefault().rules.getOffset(doseDateTime))
        val reminderEvent = createReminderEvent(entry, trimmedAmount, doseInstant)

        viewModelScope.launch {
            val event = reminderEventRepository.create(reminderEvent)
            persistentDataDataSource.addManualDose(ManualDose(entry.label, medicineId, trimmedAmount))
            onResult(ManualDoseSaveResult(event, doseDateTime.toLocalDate()))
            entry.medicine?.let { medicine ->
                val parsedAmount = DoseAmountParser.parseAmount(trimmedAmount)
                if (parsedAmount != null && medicine.isStockManagementActive()) {
                    val stockAfter = commandBus.processStockHandling(parsedAmount, medicine.id, doseInstant.epochSecond)
                    if (stockAfter != null) {
                        reminderEventRepository.update(event.copy(stockAfter = stockAfter, stockHandled = true))
                    }
                }
            }
        }
    }

    private fun createReminderEvent(
        selectedEntry: ManualDoseMedicineEntry,
        amount: String,
        doseInstant: Instant,
    ): ReminderEvent {
        var event = ReminderEvent.default().copy(
            reminderId = -1,
            status = ReminderEvent.ReminderStatus.TAKEN,
            medicineName = selectedEntry.label,
            amount = amount,
            remindedTimestamp = doseInstant,
            processedTimestamp = doseInstant,
        )
        selectedEntry.medicine?.let { medicine ->
            val stockAmount = medicine.amount.takeIf { medicine.isStockManagementActive() } ?: -1.0
            event = event.copy(
                color = medicine.color,
                useColor = medicine.useColor,
                iconId = medicine.iconId,
                tags = medicine.tags.map { tag -> tag.name },
                stockBefore = stockAmount,
                stockAfter = stockAmount,
                stockUnit = MedicineHelper.getUnit(medicine),
            )
        }
        return event
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

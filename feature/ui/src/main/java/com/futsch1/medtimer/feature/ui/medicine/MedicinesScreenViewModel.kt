package com.futsch1.medtimer.feature.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.ui.MedicineIcons
import com.futsch1.medtimer.core.ui.MedicineStringFormatter
import com.futsch1.medtimer.feature.reminders.FutureRemindersRepository
import com.futsch1.medtimer.feature.ui.TagFilterViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn


@HiltViewModel(assistedFactory = MedicinesScreenViewModel.Factory::class)
class MedicinesScreenViewModel @AssistedInject constructor(
    medicineRepository: MedicineRepository,
    @Assisted val tagFilterViewModel: TagFilterViewModel,
    private val medicineIcons: MedicineIcons,
    private val medicineStringFormatter: MedicineStringFormatter,
    private val futureRemindersRepository: FutureRemindersRepository,
) : ViewModel() {

    @AssistedFactory
    fun interface Factory {
        fun create(tagFilterViewModel: TagFilterViewModel): MedicinesScreenViewModel
    }

    private val liveMedicines = medicineRepository.getAllFlow()

    val medicines: StateFlow<List<Medicine>> =
        combine(liveMedicines, tagFilterViewModel.validTagIds) { medicines, tagIds ->
            tagFilterViewModel.getFiltered(medicines, tagIds ?: emptySet())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val medicineUiState: StateFlow<MedicineUiState> =
        combine(
            medicines,
            futureRemindersRepository.stockRunOutDates
        ) { medicines, stockRunOutDates ->
            MedicineUiState(medicines.map { medicine ->
                MedicineScreenItem(
                    medicine.id,
                    medicine.name,
                    medicineStringFormatter.getReminderTimes(medicine).toImmutableList(),
                    StockState(
                        if (medicine.isStockManagementActive()) medicineStringFormatter.getStockText(
                            medicine
                        ) else null,
                        medicine.isOutOfStock(),
                        medicineStringFormatter.getStockRunOutText
                            (
                            stockRunOutDates.getOrDefault(medicine.id, null),
                            futureRemindersRepository.simulatedThrough.value
                        )
                    ),
                    if (medicine.iconId != 0) medicineIcons.getIconBitmapUntinted(medicine.iconId) else null,
                    if (medicine.useColor) medicine.color else null,
                    medicine.tags.map { tag -> tag.name }.toImmutableList(),
                    medicine.reminders.isNotEmpty() && medicine.reminders.none { it.active }
                )
            }.toImmutableList())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MedicineUiState(persistentListOf()))
}

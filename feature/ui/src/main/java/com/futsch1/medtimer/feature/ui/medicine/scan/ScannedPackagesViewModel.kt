package com.futsch1.medtimer.feature.ui.medicine.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.domain.model.MedicineLabel
import com.futsch1.medtimer.core.domain.repository.MedicineLabelRepository
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicineLabelItem(
    val text: String,
    val medicineId: Int,
    val medicineName: String,
    val quantity: Double?
)

@HiltViewModel
class ScannedPackagesViewModel @Inject constructor(
    private val medicineLabelRepository: MedicineLabelRepository,
    medicineRepository: MedicineRepository
) : ViewModel() {

    val items: StateFlow<ImmutableList<MedicineLabelItem>> = combine(
        medicineLabelRepository.getAllFlow(),
        medicineRepository.getAllFlow()
    ) { labels, medicines ->
        val nameById = medicines.associate { it.id to it.name }
        labels.mapNotNull { label ->
            val name = nameById[label.medicineId] ?: return@mapNotNull null
            MedicineLabelItem(label.text, label.medicineId, name, label.quantity)
        }.sortedBy { it.medicineName.lowercase() }.toImmutableList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    fun updateQuantity(text: String, medicineId: Int, quantity: Double) {
        viewModelScope.launch {
            medicineLabelRepository.remember(MedicineLabel(text, medicineId, quantity))
        }
    }

    fun forget(text: String) {
        viewModelScope.launch {
            medicineLabelRepository.forget(text)
        }
    }

    fun forgetAll() {
        viewModelScope.launch {
            medicineLabelRepository.deleteAll()
        }
    }
}

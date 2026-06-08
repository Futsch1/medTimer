package com.futsch1.medtimer.feature.ui.medicine.medicineSettings

import com.futsch1.medtimer.core.common.helpers.ModelDataViewModel
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository
) : ModelDataViewModel<Medicine>() {

    override fun getFlow(id: Int): Flow<Medicine?> = medicineRepository.getFlow(id)
}

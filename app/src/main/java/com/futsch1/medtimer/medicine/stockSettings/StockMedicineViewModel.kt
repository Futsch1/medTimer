package com.futsch1.medtimer.medicine.stockSettings

import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.ModelDataViewModel
import com.futsch1.medtimer.model.Medicine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class StockMedicineViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository
) : ModelDataViewModel<Medicine>() {

    override fun getFlow(id: Int): Flow<Medicine?> = medicineRepository.getFlow(id)
}

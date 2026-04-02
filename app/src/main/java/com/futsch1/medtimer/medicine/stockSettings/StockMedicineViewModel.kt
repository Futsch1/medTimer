package com.futsch1.medtimer.medicine.stockSettings

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.EntityViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class StockMedicineViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository
) : EntityViewModel<FullMedicineEntity>() {

    override fun getFlow(id: Int): Flow<FullMedicineEntity?> = medicineRepository.getMedicineFlow(id)
}

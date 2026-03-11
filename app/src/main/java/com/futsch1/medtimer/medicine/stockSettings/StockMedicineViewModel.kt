package com.futsch1.medtimer.medicine.stockSettings

import android.app.Application
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.EntityViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class StockMedicineViewModel @Inject constructor(
    application: Application,
    override val medicineRepository: MedicineRepository
) : EntityViewModel<FullMedicine>(application) {

    override fun getFlow(id: Int): Flow<FullMedicine?> = medicineRepository.getMedicineFlow(id)
}

package com.futsch1.medtimer.medicine.stockSettings

import android.app.Application
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.EntityViewModel
import kotlinx.coroutines.flow.Flow

class StockMedicineViewModel(application: Application) : EntityViewModel<FullMedicine>(application) {
    override val medicineRepository = MedicineRepository(application)

    override fun getFlow(id: Int): Flow<FullMedicine> = medicineRepository.getMedicineFlow(id)
}

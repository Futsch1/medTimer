package com.futsch1.medtimer.feature.ui.medicine

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf


interface MedicineScreenState {
    val medicines: ImmutableList<MedicineScreenItem>
}

class MutableMedicineScreenState : MedicineScreenState {
    override var medicines by mutableStateOf<ImmutableList<MedicineScreenItem>>(persistentListOf())
}

data class StockState(
    val stockString: String?,
    val stockWarning: Boolean,
    val stockRunOutDate: String?
)

data class MedicineScreenItem(
    val id: Int,
    val name: String,
    val reminderTimes: ImmutableList<String>,
    val stockState: StockState,
    val icon: Bitmap?,
    val color: Int?,
    val tags: ImmutableList<String>,
    val inactive: Boolean = false
)

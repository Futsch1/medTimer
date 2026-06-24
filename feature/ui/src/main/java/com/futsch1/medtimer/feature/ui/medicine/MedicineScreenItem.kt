package com.futsch1.medtimer.feature.ui.medicine

import android.graphics.Bitmap
import kotlinx.collections.immutable.ImmutableList


data class MedicineUiState(
    val medicines: List<MedicineScreenItem>
)

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

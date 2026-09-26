package com.futsch1.medtimer.feature.ui.overview.manualDose

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ManualDoseState(
    val medicineEntries: ImmutableList<ManualDoseMedicineEntry> = persistentListOf(),
    val amounts: ImmutableList<String> = persistentListOf(),
    val selectedMedicine: ManualDoseMedicineEntry? = null,
    val selectedAmount: String? = null
)


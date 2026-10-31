package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.futsch1.medtimer.core.domain.model.ReminderType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

interface EditMedicineScreenState {
    val isLoaded: Boolean
    val id: Int?
    val amount: Double
    val stockTrackingActive: Boolean
    val showDoseUnitWarning: Boolean
    val doseUnits: ImmutableList<String>
    val unit: String
    val name: String
    val notes: String?
    val color: Int?
    val icon: Int
    val reminders: ImmutableList<ReminderItem>
}

class MutableEditMedicineScreenState : EditMedicineScreenState {
    override var isLoaded by mutableStateOf(false)
    override var id by mutableStateOf<Int?>(null)
    override var amount by mutableDoubleStateOf(0.0)
    override var stockTrackingActive by mutableStateOf(false)
    override var showDoseUnitWarning by mutableStateOf(false)
    override var doseUnits by mutableStateOf<ImmutableList<String>>(persistentListOf())
    override var unit by mutableStateOf("")
    override var name by mutableStateOf("")
    override var notes by mutableStateOf<String?>(null)
    override var color by mutableStateOf<Int?>(null)
    override var icon by mutableIntStateOf(0)
    override var reminders by mutableStateOf<ImmutableList<ReminderItem>>(persistentListOf())
}

data class ReminderItem(
    val id: Int,
    val timeVisible: Boolean,
    val timeLabel: Int,
    val timeHint: Int?,
    val timeMinutesOfDay: Int,
    val timeText: String,
    val amount: String,
    val type: ReminderType,
    val summary: String
)

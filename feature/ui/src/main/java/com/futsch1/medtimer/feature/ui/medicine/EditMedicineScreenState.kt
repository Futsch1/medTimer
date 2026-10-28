package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

interface EditMedicineFragmentState {
    val medicine: Medicine?
}

class MutableEditMedicineFragmentState : EditMedicineFragmentState {
    override var medicine by mutableStateOf<Medicine?>(null)
}

interface EditMedicineScreenState {
    val id: Int
    val name: String
    val color: Int?
    val icon: Int
    val reminders: ImmutableList<ReminderItem>
}

class MutableEditMedicineScreenState : EditMedicineScreenState {
    override var id by mutableIntStateOf(0)
    override var name by mutableStateOf("")
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
    val amount: String,
    val type: ReminderType,
    val summary: String
)
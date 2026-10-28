package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.futsch1.medtimer.core.domain.model.ReminderType

@Composable
fun ReminderCard(
    reminder: ReminderItem,
    onReminderEdit: (id: Int, type: ReminderType) -> Unit,
    onEditAmount: (id: Int, amount: String) -> Unit,
    onEditTime: (id: Int, timeMinutesOfDay: Int) -> Unit
) {
    Text(text = reminder.summary)
}
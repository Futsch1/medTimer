package com.futsch1.medtimer.remindertable

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.core.ui.ReminderTable
import com.futsch1.medtimer.core.ui.ReminderTableData

object ReminderTableBridge {
    @JvmStatic
    fun setTableContent(
        composeView: ComposeView,
        data: ReminderTableData,
        onEditEvent: (Int) -> Unit
    ) {
        composeView.setContent {
            MedTimerTheme {
                ReminderTable(data = data, onEditEvent = onEditEvent, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

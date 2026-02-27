package com.futsch1.medtimer.statistics

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.core.ui.TakenSkippedData
import com.futsch1.medtimer.core.ui.TakenSkippedPieChart

object TakenSkippedChartBridge {
    @JvmStatic
    fun setChartContent(composeView: ComposeView, data: TakenSkippedData) {
        composeView.setContent {
            MedTimerTheme {
                TakenSkippedPieChart(data = data, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

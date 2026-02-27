package com.futsch1.medtimer.statistics

import androidx.compose.ui.platform.ComposeView
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.core.ui.MedicinePerDayBarChart
import com.futsch1.medtimer.core.ui.MedicinePerDayData

object MedicinePerDayChartBridge {
    @JvmStatic
    fun setChartContent(
        composeView: ComposeView,
        data: MedicinePerDayData,
    ) {
        composeView.setContent {
            MedTimerTheme {
                MedicinePerDayBarChart(data = data)
            }
        }
    }
}

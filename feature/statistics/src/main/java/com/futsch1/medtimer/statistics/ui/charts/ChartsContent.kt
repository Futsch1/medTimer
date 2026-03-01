package com.futsch1.medtimer.statistics.ui.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.statistics.model.MedicinePerDayData
import com.futsch1.medtimer.statistics.model.TakenSkippedData
import com.futsch1.medtimer.statistics.ui.preview.PreviewData
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ChartsContent(
    medicinePerDayData: MedicinePerDayData?,
    takenSkippedData: TakenSkippedData?,
    takenSkippedTotalData: TakenSkippedData?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MedicinePerDayBarChart(
            data = medicinePerDayData,
            modifier = Modifier.weight(2f),
        )

        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TakenSkippedPieChart(
                data = takenSkippedData,
                modifier = Modifier
                    .weight(1f),
            )

            TakenSkippedPieChart(
                data = takenSkippedTotalData,
                modifier = Modifier
                    .weight(1f),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ChartsContentPreview() {
    MedTimerTheme {
        Surface {
            ChartsContent(
                medicinePerDayData = PreviewData.sampleMedicinePerDayData,
                takenSkippedData = PreviewData.sampleTakenSkippedData,
                takenSkippedTotalData = PreviewData.sampleTakenSkippedTotalData,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ChartsContentEmptyPreview() {
    MedTimerTheme {
        Surface {
            ChartsContent(
                medicinePerDayData = MedicinePerDayData("7 days", persistentListOf(), persistentListOf()),
                takenSkippedData = PreviewData.sampleTakenSkippedData,
                takenSkippedTotalData = PreviewData.sampleTakenSkippedTotalData,
            )
        }
    }
}

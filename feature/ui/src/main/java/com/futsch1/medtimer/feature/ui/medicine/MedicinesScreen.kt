package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import java.lang.String.join

@Composable
fun MedicinesScreen(medicinesViewModel: MedicinesViewModel) {
    MedicinesScreen(medicinesViewModel.medicinesForUi.collectAsState().value)
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
fun MedicinesScreen(medicines: List<MedicineUiState>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = 60.dp)
    ) {
        items(medicines) { medicine ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    if (medicine.icon != null) {
                        Column {
                            Image(
                                bitmap = medicine.icon.asImageBitmap(),
                                contentDescription = stringResource(id = R.string.icon),
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                    Column {
                        Text(text = medicine.name, fontWeight = FontWeight.Bold)
                        Text(text = pluralStringResource(id = R.plurals.sum_reminders, count = medicine.reminderTimes.size, medicine.reminderTimes.size))
                        Text(text = join(", ", medicine.reminderTimes))
                        if (medicine.stockRunOutDate != null) {
                            Text(text = medicine.stockRunOutDate)
                        }
                        FlexBox(
                            config = {
                                direction(FlexDirection.Row)
                                alignItems(FlexAlignItems.Start)
                            }
                        ) {
                            medicine.tags.forEach { tag ->
                                FilterChip(
                                    label = { Text(text = tag) },
                                    selected = true,
                                    onClick = {})
                            }
                        }
                    }
                }
            }
        }
    }
}

@MedTimerPreview
@Composable
fun MedicinesScreenPreview() {
    val medicineWithStockExpirationDate = listOf(
        MedicineUiState(
            name = "Test",
            reminderTimes = listOf("1:00"),
            tags = listOf("Test 1", "Test 2"),
            icon = null,
            stockRunOutDate = null
        ),
        MedicineUiState(
            name = "Test",
            reminderTimes = listOf("2:00"),
            tags = listOf("Test 1", "Test 2"),
            icon = ResourcesCompat.getDrawable(LocalResources.current, R.drawable.capsule, null)
                ?.toBitmap(),
            stockRunOutDate = "Test 1 stock run out date"
        ),
    )

    MedicinesScreen(medicineWithStockExpirationDate)
}

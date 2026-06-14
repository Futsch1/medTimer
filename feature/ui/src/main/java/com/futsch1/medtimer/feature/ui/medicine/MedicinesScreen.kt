package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview

@Composable
fun MedicinesScreen(medicinesViewModel: MedicinesViewModel) {
    MedicinesScreen(medicinesViewModel.medicinesForUi.collectAsState().value)
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
fun MedicinesScreen(medicines: List<MedicineUiState>) {
    Scaffold(
        modifier = Modifier.padding(8.dp),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /*TODO*/ },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.plus_circle),
                        contentDescription = stringResource(id = R.string.add_medicine)
                    )
                },
                text = { Text(text = stringResource(id = R.string.add_medicine)) })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
        ) {
            items(medicines) { medicine ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (medicine.icon != null) {
                            Icon(
                                bitmap = medicine.icon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                        Column {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(medicine.name)
                                    }
                                    if (medicine.stockState.stockString != null) {
                                        append(" (")
                                        append(medicine.stockState.stockString)
                                        if (medicine.stockState.stockWarning) {
                                            append(" ")
                                            withStyle(
                                                SpanStyle(
                                                    color = Color(0xFFCC0000),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            ) {
                                                append("⚠")
                                            }
                                        }
                                        if (medicine.stockState.stockRunOutDate != null) {
                                            append(", ${medicine.stockState.stockRunOutDate}")
                                        }
                                        append(")")
                                    }
                                }
                            )
                            Text(
                                text = pluralStringResource(
                                    id = R.plurals.sum_reminders,
                                    count = medicine.reminderTimes.size,
                                    medicine.reminderTimes.size
                                )
                            )
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
}

@MedTimerPreview
@Composable
fun MedicinesScreenPreview() {
    val medicineWithStockExpirationDate = listOf(
        MedicineUiState(
            name = "Test",
            reminderTimes = listOf("1:00"),
            tags = listOf("Test 1", "Test 2"),
            stockState = StockState(null, false, null),
            icon = null
        ),
        MedicineUiState(
            name = "Test",
            reminderTimes = listOf("2:00"),
            tags = listOf("Test 1", "Test 2"),
            stockState = StockState("5 left", true, "08/01/25"),
            icon = ResourcesCompat.getDrawable(LocalResources.current, R.drawable.capsule, null)
                ?.toBitmap()
        ),
    )

    MedicinesScreen(medicineWithStockExpirationDate)
}

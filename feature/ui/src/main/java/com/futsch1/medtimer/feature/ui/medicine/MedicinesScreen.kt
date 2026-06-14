package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
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
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import kotlinx.coroutines.launch

@Composable
fun MedicinesScreen(
    medicinesViewModel: MedicinesViewModel,
    addMedicine: () -> Unit,
    deleteMedicine: (id: Int) -> Unit,
    editMedicine: (id: Int) -> Unit
) {
    MedicinesScreen(
        medicinesViewModel.medicinesForUi.collectAsState().value,
        addMedicine,
        deleteMedicine,
        editMedicine
    )
}

@Composable
fun MedicinesScreen(
    medicines: List<MedicineUiState>,
    addMedicine: () -> Unit,
    deleteMedicine: (id: Int) -> Unit = {},
    editMedicine: (id: Int) -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.padding(8.dp),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = addMedicine,
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
            modifier = Modifier.fillMaxWidth(),
            contentPadding = paddingValues
        ) {
            items(medicines, key = { it.id }) { medicine ->
                SwipeToDeleteContainer(medicine, deleteMedicine) {
                    MedicineCard(medicine, editMedicine)
                }
            }
        }
    }
}

@Composable
private fun SwipeToDeleteContainer(
    medicine: MedicineUiState,
    onDelete: (id: Int) -> Unit,
    content: @Composable () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            showDeleteDialog = true
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                scope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
            },
            title = { Text(stringResource(R.string.confirm)) },
            text = { Text(stringResource(R.string.are_you_sure_delete_medicine)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(medicine.id)
                }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxWidthPx = constraints.maxWidth.toFloat()
                val offset = try { dismissState.requireOffset() } catch (_: IllegalStateException) { 0f }
                val alpha = if (maxWidthPx > 0f) (-offset / maxWidthPx).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color(0xFFCC0000).copy(alpha = alpha)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        painter = painterResource(R.drawable.trash),
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(48.dp)
                    )
                }
            }
        }
    ) {
        content()
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
private fun MedicineCard(medicine: MedicineUiState, editMedicine: (id: Int) -> Unit) {
    val cardColors = if (medicine.color != null) {
        val bg = Color(medicine.color)
        CardDefaults.cardColors(containerColor = bg, contentColor = contentColorFor(bg))
    } else {
        CardDefaults.cardColors()
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { editMedicine(medicine.id) }
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            if (medicine.icon != null) {
                Icon(
                    bitmap = medicine.icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp)
                )
            }
            Column {
                MedicineHeader(medicine)
                Text(
                    text = pluralStringResource(
                        id = R.plurals.sum_reminders,
                        count = medicine.reminderTimes.size,
                        medicine.reminderTimes.size
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                if (medicine.reminderTimes.isNotEmpty()) {
                    Text(
                        text = medicine.reminderTimes.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                MedicineTags(medicine.tags)
            }
        }
    }
}

@Composable
private fun MedicineHeader(medicine: MedicineUiState) {
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
                    withStyle(SpanStyle(color = Color(0xFFCC0000), fontWeight = FontWeight.Bold)) {
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
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
private fun MedicineTags(tags: List<String>) {
    if (tags.isEmpty()) return
    val visibleTags = tags.take(MAX_VISIBLE_TAGS)
    val overflowCount = tags.size - visibleTags.size
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            alignItems(FlexAlignItems.Start)
        }
    ) {
        visibleTags.forEach { tag ->
            FilterChip(
                label = { Text(text = tag) },
                selected = true,
                onClick = {},
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        if (overflowCount > 0) {
            FilterChip(
                label = { Text(text = stringResource(R.string.more_tags, overflowCount)) },
                selected = false,
                onClick = {},
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

private const val MAX_VISIBLE_TAGS = 5

@Composable
private fun contentColorFor(backgroundColor: Color): Color {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val bg = backgroundColor.toArgb() or -0x1000000
    return if (ColorUtils.calculateContrast(onSurface.toArgb(), bg) >=
        ColorUtils.calculateContrast(onPrimary.toArgb(), bg)
    ) onSurface else onPrimary
}

@MedTimerPreview
@Composable
fun MedicinesScreenPreview() {
    MedicinesScreen(
        listOf(
            MedicineUiState(
                id = 1,
                name = "Test",
                reminderTimes = listOf("8:00"),
                tags = listOf("Test 1", "Test 2"),
                stockState = StockState(null, false, null),
                color = null,
                icon = null
            ),
            MedicineUiState(
                id = 2,
                name = "Test colored",
                reminderTimes = listOf("8:00", "12:00"),
                tags = listOf("Tag"),
                stockState = StockState("5 left", true, "08/01/25"),
                color = Color.Red.value.toInt(),
                icon = ResourcesCompat.getDrawable(LocalResources.current, R.drawable.capsule, null)
                    ?.toBitmap()
            ),
        ),
        {}, {}, {}
    )
}

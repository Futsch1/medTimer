package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun MedicinesScreen(
    medicinesScreenViewModel: MedicinesScreenViewModel,
    onMedicineAdd: () -> Unit,
    onMedicineDelete: (id: Int) -> Unit,
    onMedicineEdit: (id: Int) -> Unit,
    onMedicineMove: (id: Int, newPosition: Int) -> Unit
) {
    MedicinesScreen(
        medicinesScreenViewModel.state,
        onMedicineAdd,
        onMedicineDelete,
        onMedicineEdit,
        onMedicineMove
    )
}

@Composable
fun MedicinesScreen(
    state: MedicineScreenState,
    onMedicineAdd: () -> Unit = {},
    onMedicineDelete: (id: Int) -> Unit = {},
    onMedicineEdit: (id: Int) -> Unit = {},
    onMedicineMove: (id: Int, newPosition: Int) -> Unit = { _, _ -> }
) {
    val listState = rememberLazyListState()
    val reorderableMedicines = rememberReorderableMedicines(listState, state.medicines, onMedicineMove)

    Scaffold(
        modifier = Modifier
            .padding(8.dp)
            .semantics { testTagsAsResourceId = true },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag(MedicineTestTags.ADD_MEDICINE),
                onClick = onMedicineAdd,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.plus_circle),
                        contentDescription = stringResource(id = R.string.add_medicine)
                    )
                },
                text = { Text(text = stringResource(id = R.string.add_medicine)) })
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MedicineTestTags.MEDICINE_LIST),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = paddingValues.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding() + 80.dp
            )
        ) {
            items(reorderableMedicines.medicines, key = { it.id }) { medicine ->
                ReorderableItem(reorderableMedicines.reorderState, key = medicine.id) { isDragging ->
                    SwipeToDeleteContainer(medicine, onMedicineDelete) {
                        MedicineCard(
                            medicine = medicine,
                            onMedicineEdit = onMedicineEdit,
                            isDragging = isDragging,
                            dragHandleModifier = Modifier.draggableHandle()
                        )
                    }
                }
            }
        }
    }
}

private class ReorderableMedicines(
    val medicines: SnapshotStateList<MedicineScreenItem>,
    val reorderState: ReorderableLazyListState
)

/**
 * Keeps a local, drag-mutable shadow of [medicines] so reordering stays smooth while a drag is in
 * progress, tracks the last dragged item, and notifies [onMedicineMove] once the drag settles.
 */
@Composable
private fun rememberReorderableMedicines(
    listState: LazyListState,
    medicines: ImmutableList<MedicineScreenItem>,
    onMedicineMove: (id: Int, newPosition: Int) -> Unit
): ReorderableMedicines {
    val localMedicines = remember { mutableStateListOf<MedicineScreenItem>() }
    var lastDraggedId by remember { mutableStateOf<Int?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val item = localMedicines.removeAt(from.index)
        localMedicines.add(to.index, item)
        lastDraggedId = item.id
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LaunchedEffect(medicines) {
        if (!reorderState.isAnyItemDragging) {
            localMedicines.clear()
            localMedicines.addAll(medicines)
        }
    }

    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (reorderState.isAnyItemDragging || lastDraggedId == null) {
            return@LaunchedEffect
        }
        val newIndex = localMedicines.indexOfFirst { it.id == lastDraggedId }
        if (newIndex >= 0) {
            onMedicineMove(lastDraggedId!!, newIndex)
        }
        lastDraggedId = null
    }

    return remember(localMedicines, reorderState) { ReorderableMedicines(localMedicines, reorderState) }
}

@Composable
private fun SwipeToDeleteContainer(
    medicine: MedicineScreenItem,
    onMedicineDelete: (id: Int) -> Unit,
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
                    onMedicineDelete(medicine.id)
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
                val offset = try {
                    dismissState.requireOffset()
                } catch (_: IllegalStateException) {
                    0f
                }
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

object MedicineTestTags {
    const val MEDICINE_LIST = "medicine_list"
    const val MEDICINE_ITEM = "medicine_item"
    const val MEDICINE_NAME = "medicine_name"
    const val ADD_MEDICINE = "add_medicine"
}

@MedTimerPreview
@Composable
fun MedicinesScreenPreview() {
    val state = MutableMedicineScreenState().apply {
        medicines = persistentListOf(
            MedicineScreenItem(
                id = 1,
                name = "Test",
                reminderTimes = persistentListOf("8:00"),
                tags = persistentListOf("Test 1", "Test 2"),
                stockState = StockState(null, false, null),
                color = null,
                icon = null
            ),
            MedicineScreenItem(
                id = 2,
                name = "Test colored",
                reminderTimes = persistentListOf("8:00", "12:00"),
                tags = persistentListOf("Tag"),
                stockState = StockState("5 left", true, "08/01/25"),
                color = Color.LightGray.toArgb(),
                icon = ResourcesCompat.getDrawable(LocalResources.current, R.drawable.capsule, LocalContext.current.theme)
                    ?.toBitmap()
            ),
        )
    }
    MedTimerTheme {
        Surface {
            MedicinesScreen(state)
        }
    }
}

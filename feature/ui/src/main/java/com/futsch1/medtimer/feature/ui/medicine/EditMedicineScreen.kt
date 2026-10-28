package com.futsch1.medtimer.feature.ui.medicine

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.rememberMedicineIcon
import com.futsch1.medtimer.feature.ui.medicine.dialogs.NotesDialog

@Composable
fun EditMedicineScreen(
    editMedicineScreenViewModel: EditMedicineScreenViewModel,
    onEditName: (name: String) -> Unit,
    onReminderAdd: () -> Unit,
    onReminderDelete: (id: Int) -> Unit,
    onReminderEdit: (id: Int, type: ReminderType) -> Unit,
    onEditReminderAmount: (id: Int, amount: String) -> Unit,
    onEditReminderTime: (id: Int, timeMinutesOfDay: Int) -> Unit,
    onSelectIcon: () -> Unit,
    onEditNotes: (notes: String) -> Unit = {},
    onOpenHelp: () -> Unit,
    onOpenSubmenu: (submenu: EditMedicineSubmenus.Submenu) -> Unit
) {
    EditMedicineScreen(
        editMedicineScreenViewModel.state,
        onEditName,
        onReminderAdd,
        onReminderDelete,
        onReminderEdit,
        onEditReminderAmount,
        onEditReminderTime,
        onSelectIcon,
        onEditNotes,
        onOpenHelp,
        onOpenSubmenu,
        editMedicineScreenViewModel.fragmentState.medicine?.notes
    )
}

@Composable
fun EditMedicineScreen(
    editMedicineScreenState: EditMedicineScreenState,
    onEditName: (name: String) -> Unit,
    onReminderAdd: () -> Unit = {},
    onReminderDelete: (id: Int) -> Unit = {},
    onReminderEdit: (id: Int, type: ReminderType) -> Unit = { _, _ -> },
    onEditReminderAmount: (id: Int, amount: String) -> Unit = { _, _ -> },
    onEditReminderTime: (id: Int, timeMinutesOfDay: Int) -> Unit = { _, _ -> },
    onSelectIcon: () -> Unit = {},
    onEditNotes: (notes: String) -> Unit = {},
    onOpenHelp: () -> Unit = {},
    onOpenSubmenu: (submenu: EditMedicineSubmenus.Submenu) -> Unit = {},
    notes: String? = null,
) {
    val listState = rememberLazyListState()
    var showNotes by rememberSaveable { mutableStateOf(false) }
    if (showNotes && notes != null) {
        NotesDialog(notes = notes, onSave = onEditNotes, onDismiss = { showNotes = false })
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag(EditMedicineTestTags.ADD_REMINDER),
                onClick = onReminderAdd,
                icon = { Icon(painterResource(R.drawable.plus_circle), contentDescription = null) },
                text = { Text(stringResource(R.string.add_reminder)) },
            )
        }
    ) { paddingValues ->
        val direction = LocalLayoutDirection.current
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(direction),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateEndPadding(direction),
            bottom = paddingValues.calculateBottomPadding(),
        )
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp)) {
                Column(Modifier.weight(2f).verticalScroll(rememberScrollState())) {
                    MedicineDetailsCard(editMedicineScreenState, onEditName, onSelectIcon, notes != null, { showNotes = true }, onOpenSubmenu)
                }
                ReminderList(
                    state = editMedicineScreenState,
                    listState = listState,
                    onReminderAdd = onReminderAdd,
                    onReminderDelete = onReminderDelete,
                    onReminderEdit = onReminderEdit,
                    onEditAmount = onEditReminderAmount,
                    onEditTime = onEditReminderTime,
                    onOpenHelp = onOpenHelp,
                    modifier = Modifier.weight(3f),
                )
            }
        } else {
            ReminderList(
                state = editMedicineScreenState,
                listState = listState,
                onReminderAdd = onReminderAdd,
                onReminderDelete = onReminderDelete,
                onReminderEdit = onReminderEdit,
                onEditAmount = onEditReminderAmount,
                onEditTime = onEditReminderTime,
                onOpenHelp = onOpenHelp,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                header = {
                    MedicineDetailsCard(editMedicineScreenState, onEditName, onSelectIcon, notes != null, { showNotes = true }, onOpenSubmenu)
                },
            )
        }
    }
}

@Composable
private fun MedicineDetailsCard(
    state: EditMedicineScreenState,
    onEditName: (String) -> Unit,
    onSelectIcon: () -> Unit,
    notesAvailable: Boolean,
    onOpenNotes: () -> Unit,
    onOpenSubmenu: (EditMedicineSubmenus.Submenu) -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = rememberMedicineIcon(state.icon)
                FilledIconButton(onClick = onSelectIcon) {
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = stringResource(R.string.icon),
                            colorFilter = ColorFilter.tint(state.color?.let { Color(it) } ?: MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Icon(painterResource(R.drawable.capsule), stringResource(R.string.icon), Modifier.size(24.dp))
                    }
                }
                EditTextButton(
                    label = stringResource(R.string.medicine_name),
                    value = state.name,
                    onConfirm = onEditName,
                    modifier = Modifier.weight(1f).padding(4.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FilledIconButton(onClick = { onOpenSubmenu(EditMedicineSubmenus.Submenu.SETTINGS) }) {
                    Icon(painterResource(R.drawable.gear), stringResource(R.string.medicine_settings))
                }
                Spacer(Modifier.weight(1f))
                FilledIconButton(onClick = onOpenNotes, enabled = notesAvailable) {
                    Icon(painterResource(R.drawable.journal_text), stringResource(R.string.notes))
                }
                FilledIconButton(onClick = { onOpenSubmenu(EditMedicineSubmenus.Submenu.TAGS) }) {
                    Icon(painterResource(R.drawable.tag), stringResource(R.string.tags))
                }
                FilledIconButton(onClick = { onOpenSubmenu(EditMedicineSubmenus.Submenu.STOCK_TRACKING) }) {
                    Icon(painterResource(R.drawable.box_seam), stringResource(R.string.medicine_stock))
                }
                FilledIconButton(onClick = { onOpenSubmenu(EditMedicineSubmenus.Submenu.CALENDAR) }) {
                    Icon(painterResource(R.drawable.calendar_week), stringResource(R.string.calendar))
                }
            }
        }
    }
}

@Composable
private fun ReminderList(
    state: EditMedicineScreenState,
    listState: LazyListState,
    onReminderAdd: () -> Unit,
    onReminderDelete: (Int) -> Unit,
    onReminderEdit: (Int, ReminderType) -> Unit,
    onEditAmount: (Int, String) -> Unit,
    onEditTime: (Int, Int) -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.testTag(EditMedicineTestTags.REMINDER_LIST),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        if (header != null) item(key = "medicine") { header() }
        item(key = "reminders_heading") {
            Text(
                stringResource(R.string.reminders),
                Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (state.reminders.isEmpty()) {
            item(key = "add_medicine_guidance") {
                SetupGuidanceCard(
                    message = stringResource(R.string.setup_no_reminders_guidance),
                    testTag = EditMedicineTestTags.SETUP_ADD_REMINDER,
                    buttonText = stringResource(R.string.add_reminder),
                    onContinue = onReminderAdd,
                    onOpenHelp = onOpenHelp,
                )
            }
        }
        items(state.reminders, key = { it.id }) { reminder ->
            SwipeToDeleteContainer({ onReminderDelete(reminder.id) }, R.string.are_you_sure_delete_reminder) {
                ReminderCard(reminder, onReminderEdit, onEditAmount, onEditTime)
            }
        }
    }
}

object EditMedicineTestTags {
    const val ADD_REMINDER = "edit_medicine_add_reminder"
    const val REMINDER_LIST = "edit_medicine_reminder_list"
    const val SETUP_ADD_REMINDER = "edit_medicine_setup_add_reminder"
    const val MENU = "edit_medicine_menu"
}

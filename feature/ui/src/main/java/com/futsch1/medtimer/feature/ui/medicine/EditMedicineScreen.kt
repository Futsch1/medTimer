package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.R

@Composable
fun EditMedicineScreen(
    editMedicineScreenViewModel: EditMedicineScreenViewModel,
    onReminderAdd: () -> Unit,
    onReminderDelete: (id: Int) -> Unit,
    onReminderEdit: (id: Int) -> Unit,
    onEditAmount: (id: Int, amount: String) -> Unit,
    onEditTime: (id: Int, timeMinutesOfDay: Int) -> Unit,
    onOpenHelp: () -> Unit,
    onOpenSubmenu: (submenu: EditMedicineSubmenus.Submenu) -> Unit
) {
    EditMedicineScreen(
        editMedicineScreenViewModel.state,
        onReminderAdd,
        onReminderDelete,
        onReminderEdit,
        onEditAmount,
        onEditTime,
        onOpenHelp,
        onOpenSubmenu
    )
}

@Composable
fun EditMedicineScreen(
    editMedicineScreenState: EditMedicineScreenState,
    onReminderAdd: () -> Unit = {},
    onReminderDelete: (id: Int) -> Unit = {},
    onReminderEdit: (id: Int) -> Unit = {},
    onEditAmount: (id: Int, amount: String) -> Unit = { _, _ -> },
    onEditTime: (id: Int, timeMinutesOfDay: Int) -> Unit = { _, _ -> },
    onOpenHelp: () -> Unit = {},
    onOpenSubmenu: (submenu: EditMedicineSubmenus.Submenu) -> Unit = {}
) {
    val listState = rememberLazyListState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag(EditMedicineTestTags.ADD_REMINDER),
                onClick = onReminderAdd,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.plus_circle),
                        contentDescription = stringResource(id = R.string.add_reminder)
                    )
                },
                text = { Text(text = stringResource(id = R.string.add_reminder)) })
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(EditMedicineTestTags.REMINDER_LIST),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = paddingValues.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding() + 80.dp
            )
        ) {
            if (editMedicineScreenState.reminders.isEmpty()) {
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
            items(editMedicineScreenState.reminders, key = { it.id }) { reminder ->
                SwipeToDeleteContainer({ onReminderDelete(reminder.id) }) {
                    ReminderCard(
                        reminder = reminder,
                        onReminderEdit = onReminderEdit,
                        onEditAmount = onEditAmount,
                        onEditTime = onEditTime
                    )
                }
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
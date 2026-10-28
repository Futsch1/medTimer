package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.futsch1.medtimer.feature.ui.helpers.DeleteHelper
import com.futsch1.medtimer.feature.ui.medicine.dialogs.NotesDialog
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import com.futsch1.medtimer.core.ui.R as CoreUiR

/** Actions on the medicine being edited: sub-screen links, bulk activation, duplication, deletion. */
class EditMedicineActions @AssistedInject constructor(
    @Assisted private val editMedicineScreenViewModel: EditMedicineScreenViewModel,
    @Assisted private val fragment: EditMedicineFragment,
    @Assisted private val navController: NavController,
    private val editMedicineSubmenusFactory: EditMedicineSubmenus.Factory,
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            editMedicineScreenViewModel: EditMedicineScreenViewModel,
            fragment: EditMedicineFragment,
            navController: NavController
        ): EditMedicineActions
    }

    private val submenus by lazy {
        editMedicineSubmenusFactory.create(
            fragment,
            editMedicineScreenViewModel.medicineId
        )
    }

    val notes: String? get() = editMedicineScreenViewModel.fragmentState.medicine?.notes

    fun saveNotes(notes: String) = editMedicineScreenViewModel.editNotes(notes)

    fun open(submenu: EditMedicineSubmenus.Submenu) = submenus.open(submenu, navController)

    fun setRemindersActive(active: Boolean) {
        editMedicineScreenViewModel.setRemindersActive(active)
    }

    fun duplicate() {
        editMedicineScreenViewModel.duplicate { navController.navigateUp() }
    }

    fun duplicateIncludingReminders() {
        editMedicineScreenViewModel.duplicateIncludingReminders { navController.navigateUp() }
    }

    fun delete() {
        DeleteHelper.deleteItem(
            navController.context,
            CoreUiR.string.are_you_sure_delete_medicine,
            {
                editMedicineScreenViewModel.delete {
                    navController.navigateUp()
                }
            },
            { }
        )
    }
}

@Composable
fun EditMedicineMenu(actions: EditMedicineActions) {
    var expanded by remember { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(false) }
    val notes = actions.notes
    if (showNotes && notes != null) {
        NotesDialog(notes = notes, onSave = actions::saveNotes, onDismiss = { showNotes = false })
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(CoreUiR.drawable.three_dots_vertical),
                contentDescription = stringResource(CoreUiR.string.more_options),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag(EditMedicineTestTags.MENU),
        ) {
            @Composable
            fun item(labelRes: Int, iconRes: Int?, onClick: () -> Unit) {
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        expanded = false
                        onClick()
                    },
                    leadingIcon = iconRes?.let { { Icon(painterResource(it), contentDescription = null) } },
                )
            }

            item(CoreUiR.string.medicine_settings, CoreUiR.drawable.gear) {
                actions.open(EditMedicineSubmenus.Submenu.SETTINGS)
            }
            if (notes != null) {
                item(CoreUiR.string.notes, CoreUiR.drawable.journal_text) { showNotes = true }
            }
            item(CoreUiR.string.tags, CoreUiR.drawable.tag) {
                actions.open(EditMedicineSubmenus.Submenu.TAGS)
            }
            item(CoreUiR.string.medicine_stock_settings, CoreUiR.drawable.box_seam) {
                actions.open(EditMedicineSubmenus.Submenu.STOCK_TRACKING)
            }
            item(CoreUiR.string.calendar, CoreUiR.drawable.calendar_week) {
                actions.open(EditMedicineSubmenus.Submenu.CALENDAR)
            }

            HorizontalDivider()
            item(CoreUiR.string.activate_all, null) { actions.setRemindersActive(true) }
            item(CoreUiR.string.deactivate_all, null) { actions.setRemindersActive(false) }

            HorizontalDivider()
            item(CoreUiR.string.duplicate, CoreUiR.drawable.copy, actions::duplicate)
            item(
                CoreUiR.string.duplicate_including_reminders,
                CoreUiR.drawable.copy,
                actions::duplicateIncludingReminders
            )
            item(CoreUiR.string.delete, CoreUiR.drawable.trash, actions::delete)
        }
    }
}

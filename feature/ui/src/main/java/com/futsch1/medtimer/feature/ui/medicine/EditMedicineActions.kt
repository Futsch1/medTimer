package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.feature.ui.helpers.DeleteHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import com.futsch1.medtimer.core.ui.R as CoreUiR

object EditMedicineTestTags {
    const val OVERFLOW = "edit_medicine_overflow"
    const val DELETE = "edit_medicine_delete"
    const val OPEN_TAGS = "edit_medicine_open_tags"
    const val OPEN_NOTES = "edit_medicine_open_notes"
    const val OPEN_STOCK = "edit_medicine_open_stock"
    const val OPEN_SETTINGS = "edit_medicine_open_settings"
    const val OPEN_CALENDAR = "edit_medicine_open_calendar"
}

/** Actions on the medicine being edited: sub-screen links, bulk activation, duplication, deletion. */
class EditMedicineActions @AssistedInject constructor(
    @Assisted private val medicine: Medicine,
    @Assisted private val fragment: EditMedicineFragment,
    @Assisted private val navController: NavController,
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val tagRepository: TagRepository,
    private val editMedicineSubmenusFactory: EditMedicineSubmenus.Factory,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            medicine: Medicine,
            fragment: EditMedicineFragment,
            navController: NavController
        ): EditMedicineActions
    }

    private val submenus by lazy { editMedicineSubmenusFactory.create(fragment, medicine) }

    fun open(submenu: EditMedicineSubmenus.Submenu) = submenus.open(submenu, navController)

    fun setRemindersActive(active: Boolean) {
        fragment.lifecycleScope.launch {
            val reminders = reminderRepository.getAll(medicine.id)
            com.futsch1.medtimer.core.common.helpers.setRemindersActive(
                reminders,
                reminderRepository,
                active
            )
        }
    }

    fun duplicate() {
        fragment.lifecycleScope.launch(dispatcher) {
            val newMedicineId = medicineRepository.create(medicine.copy(id = 0))
            assignTags(medicine.id, newMedicineId)
        }
        navController.navigateUp()
    }

    fun duplicateIncludingReminders() {
        fragment.lifecycleScope.launch {
            val fullMedicine = medicineRepository.fetch(medicine.id) ?: return@launch
            val newMedicineId = medicineRepository.create(medicine.copy(id = 0))
            for (reminder in fullMedicine.reminders) {
                reminderRepository.create(reminder.copy(id = 0, medicineRelId = newMedicineId))
            }
            assignTags(medicine.id, newMedicineId)
            navController.navigateUp()
        }
    }

    fun delete() {
        DeleteHelper.deleteItem(
            navController.context,
            CoreUiR.string.are_you_sure_delete_medicine,
            {
                fragment.lifecycleScope.launch {
                    medicineRepository.delete(medicine.id)
                    navController.navigateUp()
                }
            },
            { }
        )
    }

    private suspend fun assignTags(oldMedicineId: Int, newMedicineId: Int) {
        val oldFullMedicine = medicineRepository.fetch(oldMedicineId) ?: return
        for (oldTag in oldFullMedicine.tags) {
            tagRepository.addMedicineTag(newMedicineId, oldTag.id)
        }
    }
}

@Composable
fun RowScope.EditMedicineMenu(actions: EditMedicineActions) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag(EditMedicineTestTags.OVERFLOW)) {
            Icon(
                painter = painterResource(CoreUiR.drawable.three_dots_vertical),
                contentDescription = stringResource(CoreUiR.string.more_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            @Composable
            fun item(labelRes: Int, iconRes: Int?, testTag: String?, onClick: () -> Unit) {
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        expanded = false
                        onClick()
                    },
                    modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier,
                    leadingIcon = iconRes?.let { { Icon(painterResource(it), contentDescription = null) } },
                )
            }

            item(CoreUiR.string.medicine_settings, CoreUiR.drawable.gear, EditMedicineTestTags.OPEN_SETTINGS) {
                actions.open(EditMedicineSubmenus.Submenu.SETTINGS)
            }
            item(CoreUiR.string.notes, CoreUiR.drawable.journal_text, EditMedicineTestTags.OPEN_NOTES) {
                actions.open(EditMedicineSubmenus.Submenu.NOTES)
            }
            item(CoreUiR.string.tags, CoreUiR.drawable.tag, EditMedicineTestTags.OPEN_TAGS) {
                actions.open(EditMedicineSubmenus.Submenu.TAGS)
            }
            item(CoreUiR.string.medicine_stock_settings, CoreUiR.drawable.box_seam, EditMedicineTestTags.OPEN_STOCK) {
                actions.open(EditMedicineSubmenus.Submenu.STOCK_TRACKING)
            }
            item(CoreUiR.string.calendar, CoreUiR.drawable.calendar_week, EditMedicineTestTags.OPEN_CALENDAR) {
                actions.open(EditMedicineSubmenus.Submenu.CALENDAR)
            }

            HorizontalDivider()
            item(CoreUiR.string.activate_all, null, null) { actions.setRemindersActive(true) }
            item(CoreUiR.string.deactivate_all, null, null) { actions.setRemindersActive(false) }

            HorizontalDivider()
            item(CoreUiR.string.duplicate, CoreUiR.drawable.copy, null, actions::duplicate)
            item(
                CoreUiR.string.duplicate_including_reminders,
                CoreUiR.drawable.copy,
                null,
                actions::duplicateIncludingReminders,
            )
            item(CoreUiR.string.delete, CoreUiR.drawable.trash, EditMedicineTestTags.DELETE, actions::delete)
        }
    }
}

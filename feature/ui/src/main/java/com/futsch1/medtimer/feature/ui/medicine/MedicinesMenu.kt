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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.setRemindersActive
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.futsch1.medtimer.core.ui.R as CoreUiR

object MedicinesMenuTestTags {
    /** Scopes the entries to this menu; each one is then selected by its own label. */
    const val MENU = "medicines_menu"
}

/** The medicine-list specific actions: bulk activation and sorting. */
class MedicinesMenu @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher
) {
    var medicinesProvider: () -> List<Medicine> = { emptyList() }

    fun setRemindersActive(active: Boolean) {
        applicationScope.launch {
            for (medicine in medicinesProvider()) {
                setRemindersActive(medicine.reminders, reminderRepository, active)
            }
        }
    }

    fun sortByName() = sortBy { m -> m.sortedBy { it.name } }

    fun sortByCreationDateAscending() = sortBy { m -> m.sortedBy { it.id } }

    fun sortByCreationDateDescending() = sortBy { m -> m.sortedByDescending { it.id } }

    private fun sortBy(sortFunction: (List<Medicine>) -> List<Medicine>) {
        applicationScope.launch(dispatcher) {
            var medicines = sortFunction(medicineRepository.getAll())
            medicines = medicines.map { it.copy(sortOrder = 1.0 + medicines.indexOf(it)) }
            medicineRepository.updateAll(medicines)
        }
    }
}

@Composable
fun MedicinesMenuActions(menu: MedicinesMenu) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(CoreUiR.drawable.capsule),
                contentDescription = stringResource(CoreUiR.string.tab_medicine),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag(MedicinesMenuTestTags.MENU),
        ) {
            @Composable
            fun item(labelRes: Int, onClick: () -> Unit) {
                MenuItem(labelRes) {
                    expanded = false
                    onClick()
                }
            }

            item(CoreUiR.string.activate_all) { menu.setRemindersActive(true) }
            item(CoreUiR.string.deactivate_all) { menu.setRemindersActive(false) }
            HorizontalDivider()
            item(CoreUiR.string.by_name, menu::sortByName)
            item(CoreUiR.string.by_creation_date_ascending, menu::sortByCreationDateAscending)
            item(CoreUiR.string.by_creation_date_descending, menu::sortByCreationDateDescending)
        }
    }
}

@Composable
private fun MenuItem(labelRes: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(labelRes)) },
        onClick = onClick,
    )
}

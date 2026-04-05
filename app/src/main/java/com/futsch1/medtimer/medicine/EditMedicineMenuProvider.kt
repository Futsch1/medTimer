package com.futsch1.medtimer.medicine

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.database.toModel
import com.futsch1.medtimer.database.TagRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.helpers.EntityEditOptionsMenu
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

class EditMedicineMenuProvider @AssistedInject constructor(
    @Assisted private val medicine: MedicineEntity,
    @Assisted private val fragment: EditMedicineFragment,
    @Assisted private val navController: NavController,
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val tagRepository: TagRepository,
    private val editMedicineSubmenusFactory: EditMedicineSubmenus.Factory,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher
) : EntityEditOptionsMenu {

    @AssistedFactory
    interface Factory {
        fun create(
            medicine: MedicineEntity,
            fragment: EditMedicineFragment,
            navController: NavController
        ): EditMedicineMenuProvider
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.edit_medicine_settings, menu)
        OptionsMenu.enableOptionalIcons(menu)
        menu.setGroupDividerEnabled(true)

        MedicinesMenu.setupMenu(menu, R.id.activate_all) {
            setRemindersActive(true)
        }
        MedicinesMenu.setupMenu(menu, R.id.deactivate_all) {
            setRemindersActive(false)
        }
        MedicinesMenu.setupMenu(menu, R.id.duplicate) {
            fragment.lifecycleScope.launch(dispatcher) {
                val oldMedicineId = medicine.medicineId
                medicine.medicineId = 0
                val newMedicineId = medicineRepository.create(medicine).toInt()
                assignTags(oldMedicineId, newMedicineId)
            }
            navController.navigateUp()
        }
        MedicinesMenu.setupMenu(menu, R.id.duplicate_including_reminders) {
            fragment.lifecycleScope.launch(dispatcher) {
                duplicateIncludingReminders()
            }
            navController.navigateUp()
        }
        setupDeleteMenu(menu)
        setupLinksMenu(menu)
    }

    private suspend fun assignTags(oldMedicineId: Int, newMedicineId: Int) {
        val oldFullMedicine = medicineRepository.getFull(oldMedicineId)!!
        for (oldTag in oldFullMedicine.tags) {
            tagRepository.addMedicineTag(newMedicineId, oldTag.tagId)
        }
    }

    private suspend fun duplicateIncludingReminders() {
        val fullMedicine = medicineRepository.getFull(medicine.medicineId)!!
        val oldMedicineId = medicine.medicineId
        medicine.medicineId = 0
        val newMedicineId = medicineRepository.create(medicine).toInt()
        for (reminder in fullMedicine.reminders) {
            reminderRepository.create(reminder.toModel().copy(id = 0, medicineRelId = newMedicineId))
        }
        assignTags(oldMedicineId, newMedicineId)
    }

    private fun setupDeleteMenu(menu: Menu) {
        menu.findItem(R.id.delete_medicine).setOnMenuItemClickListener { _: MenuItem? ->
            DeleteHelper.deleteItem(
                navController.context,
                R.string.are_you_sure_delete_medicine,
                {
                    fragment.lifecycleScope.launch {

                        medicineRepository.delete(medicine.medicineId)
                        navController.navigateUp()
                    }
                },
                {
                    // do nothing
                })
            true
        }
    }

    private fun setupLinksMenu(menu: Menu) {
        val subMenus = editMedicineSubmenusFactory.create(fragment, medicine)
        val idToSubmenu: Map<Int, EditMedicineSubmenus.Submenu> = mapOf(
            R.id.openCalendar to EditMedicineSubmenus.Submenu.CALENDAR,
            R.id.openStockTracking to EditMedicineSubmenus.Submenu.STOCK_TRACKING,
            R.id.openTags to EditMedicineSubmenus.Submenu.TAGS,
            R.id.openNotes to EditMedicineSubmenus.Submenu.NOTES
        )

        for ((id, submenu) in idToSubmenu) {
            menu.findItem(id).setOnMenuItemClickListener { _: MenuItem? ->
                subMenus.open(submenu, navController)
                true
            }
        }
    }

    private fun setRemindersActive(active: Boolean) {
        fragment.lifecycleScope.launch(dispatcher) {
            val reminders = reminderRepository.getAll(medicine.medicineId)
            com.futsch1.medtimer.helpers.setRemindersActive(reminders, reminderRepository, active)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    override fun onDestroy() {
        // Nothing to do
    }

}

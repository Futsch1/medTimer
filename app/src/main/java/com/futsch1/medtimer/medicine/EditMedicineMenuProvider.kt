package com.futsch1.medtimer.medicine

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.helpers.EntityEditOptionsMenu
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditMedicineMenuProvider(
    private val medicine: Medicine,
    private val fragment: EditMedicineFragment,
    private val medicineViewModel: MedicineViewModel,
    private val navController: NavController,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : EntityEditOptionsMenu {

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
                val newMedicineId = medicineViewModel.medicineRepository.insertMedicine(medicine).toInt()
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

    private fun assignTags(oldMedicineId: Int, newMedicineId: Int) {
        val oldFullMedicine = medicineViewModel.medicineRepository.getMedicine(oldMedicineId)!!
        for (oldTag in oldFullMedicine.tags) {
            medicineViewModel.medicineRepository.insertMedicineToTag(newMedicineId, oldTag.tagId)
        }
    }

    private fun duplicateIncludingReminders() {
        val fullMedicine = medicineViewModel.medicineRepository.getMedicine(medicine.medicineId)!!
        val oldMedicineId = medicine.medicineId
        medicine.medicineId = 0
        val newMedicineId = medicineViewModel.medicineRepository.insertMedicine(medicine).toInt()
        for (reminder in fullMedicine.reminders) {
            reminder.reminderId = 0
            reminder.medicineRelId = newMedicineId
            medicineViewModel.medicineRepository.insertReminder(reminder)
        }
        assignTags(oldMedicineId, newMedicineId)
    }

    private fun setupDeleteMenu(menu: Menu) {
        menu.findItem(R.id.delete_medicine).setOnMenuItemClickListener { _: MenuItem? ->
            val deleteHelper = DeleteHelper(navController.context)
            deleteHelper.deleteItem(R.string.are_you_sure_delete_medicine, {
                medicineViewModel.medicineRepository.deleteMedicine(medicine.medicineId)
                navController.navigateUp()
            }, {
                // do nothing
            })
            true
        }
    }

    private fun setupLinksMenu(menu: Menu) {
        val subMenus = EditMedicineSubmenus(fragment, medicine, medicineViewModel.medicineRepository)
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
            val reminders: List<Reminder> =
                medicineViewModel.medicineRepository.getReminders(medicine.medicineId)
            com.futsch1.medtimer.helpers.setRemindersActive(reminders, medicineViewModel.medicineRepository, active)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    override fun onDestroy() {
        // Nothing to do
    }

}
package com.futsch1.medtimer.medicine

import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.medicine.dialogs.NotesDialog
import com.futsch1.medtimer.medicine.tags.TagDataFromMedicine
import com.futsch1.medtimer.medicine.tags.TagsFragment

class EditMedicineSubmenus(val editMedicineFragment: EditMedicineFragment, val medicine: Medicine, val medicineRepository: MedicineRepository) {
    enum class Submenu {
        NOTES,
        TAGS,
        STOCK_TRACKING,
        CALENDAR
    }

    fun open(submenu: Submenu, navController: NavController) {
        when (submenu) {
            Submenu.NOTES -> NotesDialog(editMedicineFragment)
            Submenu.TAGS -> {
                val tagDataFromMedicine = TagDataFromMedicine(editMedicineFragment, medicine.medicineId)
                val dialog: DialogFragment = TagsFragment(tagDataFromMedicine)
                dialog.show(editMedicineFragment.getParentFragmentManager(), "tags")
            }

            Submenu.STOCK_TRACKING -> {
                val action =
                    EditMedicineFragmentDirections.actionEditMedicineFragmentToStockSettingsFragment(medicine.medicineId)
                try {
                    navController.navigate(action)
                } catch (_: IllegalArgumentException) {
                    // Intentionally empty
                }
            }

            Submenu.CALENDAR -> {
                val action =
                    EditMedicineFragmentDirections.actionEditMedicineFragmentToMedicineCalendarFragment(
                        medicine.medicineId, 1, 9
                    )
                try {
                    navController.navigate(action)
                } catch (_: IllegalArgumentException) {
                    // Intentionally empty
                }
            }
        }
    }
}
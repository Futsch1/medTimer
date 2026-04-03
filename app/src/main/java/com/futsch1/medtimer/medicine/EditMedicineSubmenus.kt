package com.futsch1.medtimer.medicine

import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.medicine.dialogs.NotesDialog
import com.futsch1.medtimer.medicine.tags.TagDataFromMedicine
import com.futsch1.medtimer.medicine.tags.TagsFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class EditMedicineSubmenus @AssistedInject constructor(
    @Assisted private val editMedicineFragment: EditMedicineFragment,
    @Assisted private val medicine: Medicine,
    private val tagDataFromMedicineFactory: TagDataFromMedicine.Factory,
    private val tagsFragmentFactory: TagsFragment.Factory
) {
    @AssistedFactory
    interface Factory {
        fun create(
            editMedicineFragment: EditMedicineFragment,
            medicine: Medicine
        ): EditMedicineSubmenus
    }

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
                val tagDataFromMedicine = tagDataFromMedicineFactory.create(editMedicineFragment, medicine.medicineId)
                val dialog: DialogFragment = tagsFragmentFactory.create(tagDataFromMedicine)
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

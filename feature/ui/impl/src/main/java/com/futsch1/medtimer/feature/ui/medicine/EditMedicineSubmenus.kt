package com.futsch1.medtimer.feature.ui.medicine

import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.feature.ui.medicine.dialogs.NotesDialog
import com.futsch1.medtimer.feature.ui.medicine.tags.TagDataFromMedicine
import com.futsch1.medtimer.feature.ui.medicine.tags.TagsFragment
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
    fun interface Factory {
        fun create(
            editMedicineFragment: EditMedicineFragment,
            medicine: Medicine
        ): EditMedicineSubmenus
    }

    enum class Submenu {
        SETTINGS,
        NOTES,
        TAGS,
        STOCK_TRACKING,
        CALENDAR
    }

    fun open(submenu: Submenu, navController: NavController) {
        when (submenu) {
            Submenu.SETTINGS -> {
                val action =
                    EditMedicineFragmentDirections.actionEditMedicineFragmentToMedicineSettingsFragment(
                        medicine.id
                    )
                try {
                    navController.navigate(action)
                } catch (_: IllegalArgumentException) {
                    // Intentionally empty
                }
            }

            Submenu.NOTES -> NotesDialog(editMedicineFragment)
            Submenu.TAGS -> {
                val tagDataFromMedicine =
                    tagDataFromMedicineFactory.create(editMedicineFragment, medicine.id)
                val dialog: DialogFragment = tagsFragmentFactory.create(tagDataFromMedicine)
                dialog.show(editMedicineFragment.getParentFragmentManager(), "tags")
            }

            Submenu.STOCK_TRACKING -> {
                val action =
                    EditMedicineFragmentDirections.actionEditMedicineFragmentToStockSettingsFragment(
                        medicine.id
                    )
                try {
                    navController.navigate(action)
                } catch (_: IllegalArgumentException) {
                    // Intentionally empty
                }
            }

            Submenu.CALENDAR -> {
                val action =
                    EditMedicineFragmentDirections.actionEditMedicineFragmentToMedicineCalendarFragment(
                        medicine.id, 1, 9
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

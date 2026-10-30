package com.futsch1.medtimer.robots

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasSetTextAction
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.R as CoreUiR
import com.futsch1.medtimer.feature.ui.medicine.EditMedicineTestTags

/** The medicine's notes editor, opened from the edit-medicine menu. */
class MedicineNotesRobot(private val menus: MenuRobot, private val ui: ComposeUi) {

    fun save(text: String) {
        val dialog = open()
        dialog.replaceText(hasText(ui.getString(CoreUiR.string.notes)) and hasSetTextAction(), text)
        dialog.click(hasText(ui.getString(R.string.ok)))
    }

    /** Clears the notes and leaves without saving, so the stored text has to survive. */
    fun clearAndDiscard() {
        val dialog = open()
        dialog.replaceText(hasText(ui.getString(CoreUiR.string.notes)) and hasSetTextAction(), "")
        dialog.click(hasText(ui.getString(R.string.cancel)))
    }

    fun assertContains(expected: String) {
        val dialog = open()
        dialog.assertDisplayed(hasText(expected, substring = true))
        dialog.click(hasText(ui.getString(R.string.cancel)))
    }

    private fun open(): UiScope {
        menus.clickEditMedicineOption(CoreUiR.string.notes)
        return ui.scope(EditMedicineTestTags.NOTES_DIALOG)
    }
}

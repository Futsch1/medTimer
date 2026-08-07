package com.futsch1.medtimer.robots

import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.clearText
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.futsch1.medtimer.core.ui.R as CoreUiR

/** The medicine's notes editor, opened from the edit-medicine menu. */
class MedicineNotesRobot(private val menus: MenuRobot) {

    fun save(text: String) {
        open()
        writeTo(NOTES, text)
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.confirmSaveNotes)
    }

    /** Clears the notes and leaves without saving, so the stored text has to survive. */
    fun clearAndDiscard() {
        open()
        clearText(NOTES)
        closeKeyboard()
        cancel()
    }

    fun assertContains(expected: String) {
        open()
        assertDisplayed(NOTES, expected)
        cancel()
    }

    private fun open() = menus.clickEditMedicineOption(CoreUiR.string.notes)

    private fun cancel() = clickOn(com.futsch1.medtimer.feature.ui.R.id.cancelSaveNotes)

    private companion object {
        val NOTES = com.futsch1.medtimer.feature.ui.R.id.notes
    }
}

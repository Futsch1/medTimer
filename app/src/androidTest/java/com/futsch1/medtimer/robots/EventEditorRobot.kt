package com.futsch1.medtimer.robots

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo

/** The editor behind an Overview event: its name, amount, timestamps, state and notes. */
class EventEditorRobot(private val overview: OverviewRobot) {

    /** Opens the event at [index] from the Overview, runs [block] and returns to the Overview. */
    fun forEvent(index: Int, block: EventEditorRobot.() -> Unit) {
        overview.clickEvent(index)
        block()
        pressBack()
    }

    fun assertName(expected: String) = assertContains(NAME, expected)

    fun assertNameDoesNotContain(text: String) = assertNotContains(NAME, text)

    fun assertAmount(expected: String) = assertContains(AMOUNT, expected)

    fun assertNotes(expected: String) = assertContains(NOTES, expected)

    fun assertMedicineNotes(expected: String) =
        assertContains(com.futsch1.medtimer.feature.ui.R.id.medicineNotes, expected)

    fun assertRemindedAt(time: String, date: String) {
        assertContains(REMINDED_TIME, time)
        assertContains(REMINDED_DATE, date)
    }

    fun assertTakenAt(time: String, date: String) {
        assertContains(TAKEN_TIME, time)
        assertContains(TAKEN_DATE, date)
    }

    fun markTaken() = clickOn(com.futsch1.medtimer.feature.ui.R.id.takenToggleButton)

    fun markSkipped() = clickOn(com.futsch1.medtimer.feature.ui.R.id.skippedToggleButton)

    fun setNotes(text: String) = writeTo(NOTES, text)

    fun setRemindedAt(time: String, date: String) {
        writeTo(REMINDED_TIME, time)
        writeTo(REMINDED_DATE, date)
    }

    fun setTakenAt(time: String, date: String) {
        writeTo(TAKEN_TIME, time)
        writeTo(TAKEN_DATE, date)
    }

    private companion object {
        val NAME = com.futsch1.medtimer.feature.ui.R.id.editEventName
        val AMOUNT = com.futsch1.medtimer.feature.ui.R.id.editEventAmount
        val NOTES = com.futsch1.medtimer.feature.ui.R.id.editEventNotes
        val REMINDED_TIME = com.futsch1.medtimer.feature.ui.R.id.editEventRemindedTimestamp
        val REMINDED_DATE = com.futsch1.medtimer.feature.ui.R.id.editEventRemindedDate
        val TAKEN_TIME = com.futsch1.medtimer.feature.ui.R.id.editEventTakenTimestamp
        val TAKEN_DATE = com.futsch1.medtimer.feature.ui.R.id.editEventTakenDate
    }
}

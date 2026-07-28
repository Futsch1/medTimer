package com.futsch1.medtimer.robots

import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.futsch1.medtimer.core.ui.R as CoreUiR

/**
 * Logging a dose by hand from the Overview: pick a medicine (or invent one), give an amount, confirm
 * the time. The dialogs it walks through are its own.
 */
class ManualDoseRobot(
    private val overview: OverviewRobot,
    private val dialogs: DialogRobot,
    private val pickers: MaterialPickers,
) {

    /** Opens the picker and runs [block] on it, for the tests that assert on what it offers. */
    fun inPicker(block: ManualDoseRobot.() -> Unit) {
        overview.logManualDose()
        block()
    }

    fun log(name: String, amount: String? = null) = inPicker {
        choose(name)
        amount?.let { enterAmount(it) }
        confirmTime()
    }

    /** Logs a dose for a medicine that does not exist, typing its name into the picker. */
    fun logCustom(name: String) = inPicker {
        chooseCustom(name)
        confirmTime()
    }

    fun choose(name: String) = dialogs.clickItem(name)

    fun chooseCustom(name: String) {
        dialogs.clickItem(CoreUiR.string.custom)
        dialogs.enterText(name)
        // The name dialog and the amount dialog behind it are dismissed by the same button.
        dialogs.confirm(retryIfStillVisible = false)
        dialogs.confirm()
    }

    fun enterAmount(amount: String) = dialogs.enterTextAndConfirm(amount)

    fun confirmTime() = pickers.confirmTime()

    fun cancel() = dialogs.dismiss()

    /** The picker lists previously logged doses as suggestions. */
    fun assertSuggests(text: String) = assertContains(com.futsch1.medtimer.feature.ui.R.id.entry_text, text)

    fun assertOffers(name: String) = dialogs.assertItemDisplayed(name)

    fun assertAmountPrefilled(expected: String) = dialogs.assertInputContains(expected)
}

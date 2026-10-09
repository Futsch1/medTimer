package com.futsch1.medtimer.robots

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextInput
import com.futsch1.medtimer.feature.ui.overview.manualDose.ManualDoseTestTags

/** Drives the Compose manual-dose dialog while keeping the test-facing picker vocabulary. */
class ManualDoseRobot(
    private val ui: ComposeUi,
    private val overview: OverviewRobot,
    private val dialogs: DialogRobot,
    private val pickers: MaterialPickers,
) {
    private val dialog get() = ui.scope(ManualDoseTestTags.DIALOG)
    private val medicineList get() = dialog.scope(ManualDoseTestTags.MEDICINE_LIST)
    private val amountList get() = dialog.scope(ManualDoseTestTags.AMOUNT_LIST)

    /** Opens the dialog and runs [block] on it, for tests that assert what it offers. */
    fun inPicker(block: ManualDoseRobot.() -> Unit) {
        overview.logManualDose()
        dialog.awaitSelfExists()
        block()
    }

    fun log(name: String, amount: String) = inPicker {
        selectMedicine(name)
        selectAmount(amount)
        confirmTime()
    }

    /** Logs a dose for a medicine that does not exist, typing its name and leaving amount blank. */
    fun logCustom(name: String) = inPicker {
        chooseCustom(name)
        enterAmount("")
        confirmTime()
    }

    /** Chooses the dialog's Custom medicine action and enters the supplied name. */
    fun chooseCustom(name: String) {
        dialog.click(hasTestTag(ManualDoseTestTags.CUSTOM_BUTTON))
        dialogs.enterText(name)
        dialogs.confirm(retryIfStillVisible = false)
        dialog.awaitSelfExists()
    }

    /** Opens Custom amount and enters [amount]. An empty value is supported for legacy tests. */
    fun enterAmount(amount: String) {
        dialog.click(hasTestTag(ManualDoseTestTags.CUSTOM_BUTTON))
        dialogs.enterTextAndConfirm(amount)
    }

    /** Opens the time picker and accepts its prefilled current time. */
    fun confirmTime() {
        dialog.click(hasTestTag(ManualDoseTestTags.ENTER_TIME))
        pickers.confirmTime()
    }

    fun cancel() = dialog.click(hasTestTag(ManualDoseTestTags.CANCEL))

    /** The staged dialog offers the previous custom amount as a recent choice. */
    fun assertAmountPrefilled(expected: String) {
        assertAmountOffered(expected)
    }

    private fun selectMedicineIfPresent(label: String): Boolean {
        val item = hasTestTag(ManualDoseTestTags.MEDICINE_ENTRY)
        if (!medicineList.exists(item)) return false
        medicineList.scrollUntilText(
            itemMatcher = item,
            textMatcher = hasText(label),
            substring = label,
        )
        val entry = item and hasText(label)
        if (!medicineList.exists(entry)) return false
        medicineList.click(entry)
        return true
    }

    /** Types into the search field that the medicine step shows once the list gets long. */
    fun search(query: String) {
        dialog.awaitExists(hasTestTag(ManualDoseTestTags.MEDICINE_SEARCH))
        dialog.node(hasTestTag(ManualDoseTestTags.MEDICINE_SEARCH)).performTextInput(query)
        dialog.settle()
    }

    fun assertMedicinesOffered(vararg labels: String) {
        medicineList.await { medicineList.textsUnder(hasTestTag(ManualDoseTestTags.MEDICINE_ENTRY)).sorted() == labels.sorted() }
    }

    fun selectMedicine(label: String) {
        if (recentChoiceExists(label)) {
            chooseRecent(label)
            return
        }
        check(selectMedicineIfPresent(label)) { "Medicine '$label' is not offered" }
    }

    private fun selectAmount(amount: String) {
        if (recentChoiceExists(amount)) {
            chooseRecent(amount)
            return
        }
        val item = hasTestTag(ManualDoseTestTags.AMOUNT_ENTRY)
        if (amountList.exists(item)) {
            amountList.scrollUntilText(
                itemMatcher = item,
                textMatcher = hasText(amount),
                substring = amount,
            )
            val entry = item and hasText(amount)
            if (amountList.exists(entry)) {
                amountList.click(entry)
                return
            }
        }

        dialog.click(hasTestTag(ManualDoseTestTags.CUSTOM_BUTTON))
        dialogs.enterTextAndConfirm(amount)
    }

    private fun assertAmountOffered(amount: String) {
        if (recentChoiceExists(amount)) {
            dialog.assertDisplayed(recentMatcher(amount))
            return
        }
        amountList.scrollUntilText(
            itemMatcher = hasTestTag(ManualDoseTestTags.AMOUNT_ENTRY),
            textMatcher = hasText(amount),
            substring = amount,
        )
        amountList.assertDisplayed(
            hasTestTag(ManualDoseTestTags.AMOUNT_ENTRY) and hasText(amount),
        )
    }

    private fun recentChoiceExists(label: String): Boolean = dialog.exists(recentMatcher(label))

    private fun recentMatcher(label: String) =
        hasTestTag(ManualDoseTestTags.RECENT_CHOICE) and hasText(label)

    private fun chooseRecent(label: String) = dialog.click(recentMatcher(label))
}

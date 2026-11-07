package com.futsch1.medtimer.robots

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.futsch1.medtimer.feature.ui.overview.manualDose.ManualDoseTestTags
import com.futsch1.medtimer.core.ui.R as CoreUiR

/** Drives the Compose manual-dose dialog while keeping the test-facing picker vocabulary. */
class ManualDoseRobot(
    private val ui: ComposeUi,
    private val overview: OverviewRobot,
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

    /** Logs a custom medicine dose at the current time, leaving the amount blank. */
    fun logCustom(name: String) = inPicker {
        chooseCustom(name)
        enterAmount("")
        dialog.click(hasText(ui.getString(CoreUiR.string.log_at_current_time)))
        dialog.awaitGone()
    }

    /** Chooses the dialog's Custom medicine action and enters the supplied name. */
    fun chooseCustom(name: String) {
        dialog.click(hasTestTag(ManualDoseTestTags.CUSTOM_BUTTON))
        enterCustomText(name)
        confirmCustomText()
        dialog.awaitSelfExists()
    }

    fun assertBlankCustomMedicineRejected() {
        dialog.click(hasTestTag(ManualDoseTestTags.CUSTOM_BUTTON))
        confirmCustomText()
        ui.scope(ManualDoseTestTags.CUSTOM_ERROR).self().assertIsDisplayed()
        ui.scope(ManualDoseTestTags.CUSTOM_CANCEL).clickSelf()
    }

    fun cancelPicker() = dialog.click(hasTestTag(ManualDoseTestTags.CANCEL))

    /** Opens Custom amount and enters [amount]. An empty value is supported for legacy tests. */
    fun enterAmount(amount: String) {
        dialog.click(hasTestTag(ManualDoseTestTags.CUSTOM_BUTTON))
        enterCustomText(amount)
        confirmCustomText()
    }

    private fun enterCustomText(text: String) {
        val input = ui.scope(ManualDoseTestTags.CUSTOM_INPUT)
        input.awaitSelfExists()
        input.self().performTextClearance()
        if (text.isNotEmpty()) input.self().performTextInput(text)
        input.settle()
    }

    private fun confirmCustomText() {
        val confirm = ui.scope(ManualDoseTestTags.CUSTOM_CONFIRM)
        confirm.awaitSelfExists()
        confirm.clickSelf()
    }

    /** Opens the time picker and accepts its prefilled current time. */
    fun confirmTime() {
        dialog.click(hasTestTag(ManualDoseTestTags.ENTER_TIME))
        pickers.confirmTime()
    }

    fun assertRecentDoseOffered(medicine: String, amount: String) {
        ensureRecentDoseVisible(medicine, amount)
        dialog.assertDisplayed(recentDoseMatcher(medicine, amount))
    }

    fun selectRecentDose(medicine: String, amount: String) {
        ensureRecentDoseVisible(medicine, amount)
        dialog.click(recentDoseMatcher(medicine, amount))
        dialog.awaitGone()
    }

    private fun ensureRecentDoseVisible(medicine: String, amount: String) {
        if (!dialog.exists(recentDoseMatcher(medicine, amount))) {
            medicineList.scrollToIndex(0)
        }
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
        medicineList.await {
            medicineList.textsUnder(hasTestTag(ManualDoseTestTags.MEDICINE_ENTRY)).sorted() == labels.sorted()
        }
    }

    fun selectMedicine(label: String) {
        check(selectMedicineIfPresent(label)) { "Medicine '$label' is not offered" }
    }

    private fun selectAmount(amount: String) {
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
        enterCustomText(amount)
        confirmCustomText()
    }

    private fun recentDoseMatcher(medicine: String, amount: String) =
        hasTestTag(ManualDoseTestTags.RECENT_DOSE) and hasText(medicine, substring = true) and
                hasText(amount, substring = true)
}

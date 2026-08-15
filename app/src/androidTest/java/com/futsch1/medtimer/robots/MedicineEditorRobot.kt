package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.test.hasText
import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.assertion.BaristaErrorAssertions.assertErrorDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.safelyScrollTo
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.utilities.awaitToast
import java.time.LocalTime
import kotlin.time.Duration

/**
 * The medicine editor: its name, its reminders and its stock settings.
 * Which reminder type card, and how far back it is to the editor, are implementation -
 * callers state what the medicine should end up with.
 */
class MedicineEditorRobot(
    private val ui: ComposeUi,
    private val menus: MenuRobot,
    private val preferences: PreferenceScreenRobot,
    private val pickers: MaterialPickers,
) {

    fun assertTitle(expected: String) =
        ui.scope(com.futsch1.medtimer.core.ui.ScreenTestTags.TOP_APP_BAR).assertDisplayed(hasText(expected))

    fun rename(name: String) = writeTo(com.futsch1.medtimer.feature.ui.R.id.editMedicineName, name)

    fun setAmount(amount: String) = writeTo(AMOUNT, amount)

    fun assertAmount(expected: String) = assertDisplayed(AMOUNT, expected)

    fun addReminder(amount: String, time: LocalTime? = null) {
        openCard(com.futsch1.medtimer.feature.ui.R.id.timeBasedCard)
        writeTo(AMOUNT, amount)
        closeKeyboard()

        if (time != null) {
            clickOn(com.futsch1.medtimer.feature.ui.R.id.editReminderTime)
            pickers.pickTime(time)
        }
        closeKeyboard()

        create()
    }

    fun addHourlyIntervalReminder(amount: String, interval: Duration) {
        openCard(com.futsch1.medtimer.feature.ui.R.id.continuousIntervalCard)
        writeTo(AMOUNT, amount)
        setIntervalHours(interval)
        create()
    }

    /** An interval reminder that only fires between [windowStart] and [windowEnd] each day. */
    fun addWindowedIntervalReminder(
        amount: String,
        windowStart: LocalTime,
        windowEnd: LocalTime,
        interval: Duration,
    ) {
        openCard(com.futsch1.medtimer.feature.ui.R.id.windowedIntervalCard)
        writeTo(AMOUNT, amount)
        closeKeyboard()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.editIntervalDailyStartTime)
        pickers.pickTime(windowStart)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.editIntervalDailyEndTime)
        pickers.pickTime(windowEnd)

        setIntervalHours(interval)
        create()
    }

    fun addStockReminder(threshold: String) {
        openStockReminderCard(threshold)
        create()
    }

    /** A stock reminder that fires at [time] on every day the stock is below the threshold. */
    fun addDailyStockReminder(threshold: String, time: LocalTime) {
        openStockReminderCard(threshold)
        clickOn(R.string.daily_below_threshold)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.editReminderTime)
        pickers.pickTime(time)
        create()
    }

    /** An expiration reminder that fires at [time] each day; defaults to the dialog's own default time. */
    fun addExpirationReminder(daysBefore: String, time: LocalTime? = null) {
        openCard(com.futsch1.medtimer.feature.ui.R.id.expirationDateReminderCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editExpirationDaysBefore, daysBefore)
        if (time != null) {
            clickOn(com.futsch1.medtimer.feature.ui.R.id.editReminderTime)
            pickers.pickTime(time)
        }
        create()
    }

    /** Types [amount] into a new time-based reminder and asserts it is rejected, leaving the card open. */
    fun assertAmountRejected(amount: String, @StringRes errorRes: Int) {
        openCard(com.futsch1.medtimer.feature.ui.R.id.timeBasedCard)
        writeTo(AMOUNT, amount)
        assertErrorDisplayed(AMOUNT, errorRes)
    }

    /** Opens the stock settings, runs [block] there and returns to the editor. */
    fun inStockSettings(block: () -> Unit) {
        menus.clickEditMedicineOption(R.string.medicine_stock_settings)
        block()
        pressBack()
    }

    /** Only the values given are written; an empty string clears the field. */
    fun setStock(amount: String? = null, unit: String? = null, refillSize: String? = null) {
        inStockSettings {
            amount?.let { preferences.setValue(R.string.amount, it) }
            unit?.let { preferences.setValue(R.string.unit, it) }
            refillSize?.let { preferences.setValue(R.string.refill_size, it) }
        }
    }

    fun refillNow() = inStockSettings { preferences.click(R.string.refill_now) }

    fun assertStockAmount(expected: String) =
        inStockSettings { preferences.assertSummary(R.string.amount, expected) }

    fun assertRefillSize(expected: String) =
        inStockSettings { preferences.assertSummary(R.string.refill_size, expected) }

    private fun openStockReminderCard(threshold: String) {
        openCard(com.futsch1.medtimer.feature.ui.R.id.stockReminderCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editStockThreshold, threshold)
    }

    /** On a short screen - a tablet in landscape - the card sits below the dialog's fold. */
    private fun openCard(cardId: Int) {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        safelyScrollTo(cardId)
        clickOn(cardId)
    }

    private fun setIntervalHours(interval: Duration) {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.intervalHours)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editIntervalTime, interval.inWholeHours.toString())
        closeKeyboard()
    }

    /** The button sits below the dialog's fold on a short screen, same as the cards. */
    private fun create() {
        safelyScrollTo(com.futsch1.medtimer.feature.ui.R.id.createReminder)
        awaitToast(ui.getString(R.string.successfully_created_reminder)) { clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder) }
    }

    private companion object {
        val AMOUNT = com.futsch1.medtimer.feature.ui.R.id.editAmount
    }
}

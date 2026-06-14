package com.futsch1.medtimer

import android.icu.util.Calendar
import android.text.format.DateFormat
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.futsch1.medtimer.feature.ui.medicine.MedicineTestTags
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.hamcrest.Matchers
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date
import java.util.Locale

object AndroidTestHelper {
    @JvmStatic
    fun createReminder(amount: String, time: LocalTime?) {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.timeBasedCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editAmount, amount)
        closeKeyboard()

        if (time != null) {
            clickOn(com.futsch1.medtimer.feature.ui.R.id.editReminderTime)
            setTime(time.hour, time.minute, false)
        }
        closeKeyboard()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)
    }

    fun setTime(hour: Int, minute: Int, isDeltaTime: Boolean) {
        var hour = hour
        if (!DateFormat.is24HourFormat(InstrumentationRegistry.getInstrumentation().targetContext) && !isDeltaTime) {
            clickOn(com.google.android.material.R.id.material_clock_period_am_button)
            if (hour == 12) {
                clickOn(com.google.android.material.R.id.material_clock_period_pm_button)
            }
            if (hour > 12) {
                hour -= 12
                clickOn(com.google.android.material.R.id.material_clock_period_pm_button)
            }
            if (hour == 0) {
                hour = 12
            }
        }

        clickOn(com.google.android.material.R.id.material_timepicker_mode_button)
        writeTo(com.google.android.material.R.id.material_hour_text_input, hour.toString())
        // Close the keyboard before clicking the minute field – on tablets the soft keyboard
        // causes the time-picker dialog to be shifted (adjustPan) and the minute field leaves
        // the global visible rect, making Espresso unable to click it and eventually
        // dismissing the dialog entirely.
        closeKeyboard()
        clickOn(com.google.android.material.R.id.material_minute_text_input)
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(com.google.android.material.R.id.material_minute_text_input)),
                ViewMatchers.isAssignableFrom(android.widget.EditText::class.java)
            )
        ).perform(ViewActions.replaceText(minute.toString()))
        closeKeyboard()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)
    }

    @JvmStatic
    fun setDate(date: Date) {
        val dateString = dateToStringForDateEdit(date)
        clickOn(com.google.android.material.R.id.mtrl_picker_header_toggle)
        writeTo(com.google.android.material.R.id.mtrl_picker_text_input_date, dateString)
        closeKeyboard()
        clickOn(com.google.android.material.R.id.confirm_button)
    }

    private fun dateToStringForDateEdit(date: Date): String {
        return defaultTextInputFormat.format(date)
    }

    val defaultTextInputFormat: SimpleDateFormat
        // Taken from UtcDates in Material DatePicker
        get() {
            var defaultFormatPattern =
                (java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.getDefault()) as SimpleDateFormat)
                    .toPattern()
            defaultFormatPattern = getDatePatternAsInputFormat(defaultFormatPattern)
            val format = SimpleDateFormat(defaultFormatPattern, Locale.getDefault())
            format.isLenient = false
            return format
        }

    fun getDatePatternAsInputFormat(localeFormat: String): String {
        return localeFormat
            .replace("[^dMy/\\-.]".toRegex(), "")
            .replace("d{1,2}".toRegex(), "dd")
            .replace("M{1,2}".toRegex(), "MM")
            .replace("y{1,4}".toRegex(), "yyyy")
            .replace("\\.$".toRegex(), "") // Removes a dot suffix that appears in some formats
            .replace("My".toRegex(), "M/y") // Edge case for the Kako locale
    }

    @JvmStatic
    fun setValue(value: String) {
        writeTo(android.R.id.edit, value)
        closeKeyboard()
        clickDialogPositiveButton()
    }

    @JvmStatic
    fun createIntervalReminder(amount: String, intervalMinutes: Int) {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.continuousIntervalCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editAmount, amount)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.intervalMinutes)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editIntervalTime, intervalMinutes.toString())

        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)
    }

    @JvmStatic
    fun createMedicine(name: String) {
        navigateTo(MainMenu.MEDICINES)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val addBtn = device.wait(Until.findObject(By.res(MedicineTestTags.ADD_MEDICINE)), 3_000)
            ?: throw AssertionError("Add medicine button not found")
        addBtn.click()
        writeTo(com.futsch1.medtimer.feature.ui.R.id.medicineName, name)

        clickDialogPositiveButton()
    }

    @JvmStatic
    fun navigateTo(mainMenu: MainMenu) {
        // MainMenu.ANALYSIS maps to the "statistics" destination/tag — the app uses both terms
        // (statisticsFragment + R.string.analysis); this preserves that existing naming.
        val tag = when (mainMenu) {
            MainMenu.OVERVIEW -> NavTestTags.OVERVIEW
            MainMenu.MEDICINES -> NavTestTags.MEDICINES
            MainMenu.ANALYSIS -> NavTestTags.STATISTICS
        }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Click twice: first navigates to the tab, second reselects it (some tests rely on the
        // legacy reselect behavior, e.g. onFragmentReselected resetting Overview to today).
        repeat(2) {
            val item = device.wait(Until.findObject(By.res(tag)), 5_000)
                ?: throw AssertionError("Navigation item with tag '$tag' not found")
            item.click()
        }
    }

    val nextNotificationTime: LocalDateTime
        get() {
            val rightNow = Calendar.getInstance()
            val dateTime = LocalDateTime.of(
                rightNow.get(Calendar.YEAR),
                rightNow.get(Calendar.MONTH) + 1,
                rightNow.get(Calendar.DAY_OF_MONTH),
                rightNow.get(Calendar.HOUR_OF_DAY),
                rightNow.get(Calendar.MINUTE),
                0
            )
            return dateTime.plusMinutes(10)
        }

    fun scrollDown() {
        val appViews = UiScrollable(
            UiSelector().scrollable(true)
        )
        try {
            appViews.scrollForward()
        } catch (_: UiObjectNotFoundException) {
            // Intentionally empty
        }
    }

    fun longClickListItem(@IdRes id: Int, position: Int) {
        Espresso.onView(ViewMatchers.withId(id))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder?>(position, ViewActions.longClick()))
    }

    fun waitForIdle(timeoutMs: Long = 2_000) {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle(timeoutMs)
    }

    fun waitForText(text: String, timeoutMs: Long = 3_000) {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).wait(Until.findObject(By.text(text)), timeoutMs)
    }

    fun getMedicineItems(): List<UiObject2> {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        return device.findObjects(By.res(MedicineTestTags.MEDICINE_ITEM))
            .sortedBy { it.visibleBounds.top }
    }

    fun clickMedicineItem(position: Int) {
        getMedicineItems()[position].click()
    }

    fun assertMedicineCount(expected: Int) {
        val actual = getMedicineItems().size
        assert(actual == expected) { "Expected $expected medicine items, found $actual" }
    }

    fun assertMedicineAtPosition(position: Int, expectedName: String) {
        val item = getMedicineItems()[position]
        val nameNode = item.findObject(By.res(MedicineTestTags.MEDICINE_NAME))
        val actual = nameNode?.text ?: ""
        assert(actual == expectedName || actual.startsWith("$expectedName (")) {
            "Medicine at position $position: expected name '$expectedName', got '$actual'"
        }
    }

    fun assertMedicineNameContains(text: String) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val names = device.findObjects(By.res(MedicineTestTags.MEDICINE_NAME))
        assert(names.any { it.text?.contains(text) == true }) {
            "No medicine name contains '$text'"
        }
    }

    fun assertMedicineNameNotContains(text: String) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val names = device.findObjects(By.res(MedicineTestTags.MEDICINE_NAME))
        assert(names.none { it.text?.contains(text) == true }) {
            "A medicine name contains '$text' but should not"
        }
    }

    fun dragMedicineItem(fromPosition: Int, toPosition: Int) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val items = getMedicineItems()
        val fromHandle = items[fromPosition].findObject(By.desc("Move medicine"))
        val toItemBounds = items[toPosition].visibleBounds
        val fromBounds = fromHandle.visibleBounds
        device.drag(
            fromBounds.centerX(), fromBounds.centerY(),
            toItemBounds.centerX(), toItemBounds.centerY(),
            40
        )
        device.waitForIdle(1_000)
    }

    enum class MainMenu {
        OVERVIEW, MEDICINES, ANALYSIS
    }
}

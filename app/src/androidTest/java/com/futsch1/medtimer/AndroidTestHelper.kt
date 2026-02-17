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
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.google.android.material.textfield.TextInputEditText
import org.hamcrest.Matchers
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date
import java.util.Locale

object AndroidTestHelper {
    @JvmStatic
    fun createReminder(amount: String, time: LocalTime?) {
        clickOn(R.id.addReminder)
        clickOn(R.id.timeBasedCard)
        writeTo(R.id.editAmount, amount)

        if (time != null) {
            clickOn(R.id.editReminderTime)
            setTime(time.hour, time.minute, false)
        }
        closeKeyboard()

        clickOn(R.id.createReminder)
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
        clickOn(com.google.android.material.R.id.material_minute_text_input)
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDisplayed(),
                ViewMatchers.withClassName(Matchers.`is`(TextInputEditText::class.java.getName()))
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
        BaristaDialogInteractions.clickDialogPositiveButton()
    }

    @JvmStatic
    fun createIntervalReminder(amount: String, intervalMinutes: Int) {
        clickOn(R.id.addReminder)
        clickOn(R.id.continuousIntervalCard)
        writeTo(R.id.editAmount, amount)

        clickOn(R.id.intervalMinutes)
        writeTo(R.id.editIntervalTime, intervalMinutes.toString())

        closeKeyboard()
        clickOn(R.id.createReminder)
    }

    @JvmStatic
    fun createMedicine(name: String) {
        navigateTo(MainMenu.MEDICINES)

        clickOn(R.id.addMedicine)
        writeTo(R.id.medicineName, name)

        BaristaDialogInteractions.clickDialogPositiveButton()
    }

    @JvmStatic
    fun navigateTo(mainMenu: MainMenu) {
        val menuIds =
            intArrayOf(R.id.overviewFragment, R.id.medicinesFragment, R.id.statisticsFragment)
        clickOn(menuIds[mainMenu.ordinal])
        clickOn(menuIds[mainMenu.ordinal])
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

    fun closeNotifications(device: UiDevice) {
        device.swipe(device.displayWidth / 2, device.displayHeight, device.displayWidth / 2, device.displayHeight / 2, 20)
        device.waitForIdle(200)
        if (!device.findObjects(By.res("android:id/expand_button")).isEmpty() || !device.findObjects(By.descContains("Expand")).isEmpty()) {
            device.pressBack()
        }
    }

    fun longClickListItem(@IdRes id: Int, position: Int) {
        Espresso.onView(ViewMatchers.withId(id))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder?>(position, ViewActions.longClick()))
    }

    enum class MainMenu {
        OVERVIEW, MEDICINES, ANALYSIS
    }
}

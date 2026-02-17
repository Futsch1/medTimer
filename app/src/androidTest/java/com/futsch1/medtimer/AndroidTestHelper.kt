package com.futsch1.medtimer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.icu.util.Calendar;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import com.google.android.material.textfield.TextInputEditText;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("java:S2925")
public class AndroidTestHelper {
    public static void createReminder(String amount, LocalTime time) {
        clickOn(R.id.addReminder);
        clickOn(R.id.timeBasedCard);
        writeTo(R.id.editAmount, amount);

        if (time != null) {
            clickOn(R.id.editReminderTime);
            setTime(time.getHour(), time.getMinute(), false);
        }
        closeKeyboard();

        clickOn(R.id.createReminder);
    }

    public static void setTime(int hour, int minute, boolean isDeltaTime) {
        if (!android.text.format.DateFormat.is24HourFormat(getInstrumentation().getTargetContext()) && !isDeltaTime) {
            clickOn(com.google.android.material.R.id.material_clock_period_am_button);
            if (hour == 12) {
                clickOn(com.google.android.material.R.id.material_clock_period_pm_button);
            }
            if (hour > 12) {
                hour -= 12;
                clickOn(com.google.android.material.R.id.material_clock_period_pm_button);
            }
            if (hour == 0) {
                hour = 12;
            }
        }

        clickOn(com.google.android.material.R.id.material_timepicker_mode_button);
        writeTo(com.google.android.material.R.id.material_hour_text_input, String.valueOf(hour));
        clickOn(com.google.android.material.R.id.material_minute_text_input);
        onView(allOf(isDisplayed(), withClassName(is(TextInputEditText.class.getName())))).perform(replaceText(String.valueOf(minute)));
        closeKeyboard();
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button);
    }

    public static void setDate(Date date) {
        String dateString = dateToStringForDateEdit(date);
        clickOn(com.google.android.material.R.id.mtrl_picker_header_toggle);
        writeTo(com.google.android.material.R.id.mtrl_picker_text_input_date, dateString);
        clickOn(com.google.android.material.R.id.confirm_button);
    }

    private static String dateToStringForDateEdit(Date date) {
        return getDefaultTextInputFormat().format(date);
    }

    // Taken from UtcDates in Material DatePicker
    static SimpleDateFormat getDefaultTextInputFormat() {
        String defaultFormatPattern =
                ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()))
                        .toPattern();
        defaultFormatPattern = getDatePatternAsInputFormat(defaultFormatPattern);
        SimpleDateFormat format = new SimpleDateFormat(defaultFormatPattern, Locale.getDefault());
        format.setLenient(false);
        return format;
    }

    @NonNull
    @SuppressWarnings("java:S5361")
    static String getDatePatternAsInputFormat(@NonNull String localeFormat) {
        return localeFormat
                .replaceAll("[^dMy/\\-.]", "")
                .replaceAll("d{1,2}", "dd")
                .replaceAll("M{1,2}", "MM")
                .replaceAll("y{1,4}", "yyyy")
                .replaceAll("\\.$", "") // Removes a dot suffix that appears in some formats
                .replaceAll("My", "M/y"); // Edge case for the Kako locale
    }

    public static void setValue(String value) {
        writeTo(android.R.id.edit, value);
        clickDialogPositiveButton();
    }

    public static void createIntervalReminder(String amount, int intervalMinutes) {
        clickOn(R.id.addReminder);
        clickOn(R.id.continuousIntervalCard);
        writeTo(R.id.editAmount, amount);

        clickOn(R.id.intervalMinutes);
        writeTo(R.id.editIntervalTime, String.valueOf(intervalMinutes));

        closeKeyboard();
        clickOn(R.id.createReminder);
    }

    public static void createMedicine(String name) {
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        clickOn(R.id.addMedicine);
        writeTo(R.id.medicineName, name);

        clickDialogPositiveButton();
    }

    public static void navigateTo(MainMenu mainMenu) {
        int[] menuIds = {R.id.overviewFragment, R.id.medicinesFragment, R.id.statisticsFragment};
        clickOn(menuIds[mainMenu.ordinal()]);
        clickOn(menuIds[mainMenu.ordinal()]);
    }

    public static LocalDateTime getNextNotificationTime() {
        Calendar rightNow = Calendar.getInstance();
        LocalDateTime dateTime = LocalDateTime.of(rightNow.get(Calendar.YEAR), rightNow.get(Calendar.MONTH) + 1, rightNow.get(Calendar.DAY_OF_MONTH), rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE), 0);
        return dateTime.plusMinutes(10);
    }

    public static void scrollDown() {
        UiScrollable appViews = new UiScrollable(
                new UiSelector().scrollable(true));
        try {
            appViews.scrollForward();
        } catch (UiObjectNotFoundException e) {
            // Intentionally empty
        }
    }

    public static void closeNotifications(UiDevice device) {
        device.swipe(device.getDisplayWidth() / 2, device.getDisplayHeight(), device.getDisplayWidth() / 2, device.getDisplayHeight() / 2, 20);
        device.waitForIdle(200);
        if (!device.findObjects(By.res("android:id/expand_button")).isEmpty() || !device.findObjects(By.descContains("Expand")).isEmpty()) {
            device.pressBack();
        }
    }

    public static void longClickListItem(@IdRes int id, int position) {
        onView(withId(id))
                .perform(actionOnItemAtPosition(position, longClick()));
    }

    public enum MainMenu {OVERVIEW, MEDICINES, ANALYSIS}
}

package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild;
import static com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu;
import static com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep;
import static com.futsch1.medtimer.NotificationTestKt.getNotificationText;
import static com.futsch1.medtimer.NotificationTestKt.makeNotificationExpanded;
import static junit.framework.TestCase.assertEquals;

import android.widget.TextView;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;

import com.evrencoskun.tableview.TableView;

import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

public class ScreenshotsTest extends BaseTestHelper {
    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Test
    //@AllowFlaky(attempts = 1)
    public void screenshotsTest() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        UiDevice device = UiDevice.getInstance(getInstrumentation());

        openMenu();
        clickOn(R.string.generate_test_data);

        clickListItemChild(R.id.reminders, 0, R.id.overviewContentContainer);
        internalAssert(device.findObject(By.textContains("Some note")) != null);
        pressBack();

        clickListItemChild(R.id.reminders, 0, R.id.stateButton);
        clickOn(R.id.takenButton);
        clickListItemChild(R.id.reminders, 2, R.id.stateButton);
        clickOn(R.id.takenButton);
        clickListItemChild(R.id.reminders, 3, R.id.stateButton);
        clickOn(R.id.skippedButton);

        device.openNotification();
        makeNotificationExpanded(device, getNotificationText(R.string.taken));
        Screengrab.screenshot("5");
        device.pressBack();

        clickListItemChild(R.id.reminders, 4, R.id.stateButton);
        clickOn(R.id.takenButton);

        Screengrab.screenshot("1");

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        Screengrab.screenshot("2");

        onView(withId(R.id.medicineList)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        Screengrab.screenshot("3");

        clickListItemChild(R.id.reminderList, 0, R.id.openAdvancedSettings);
        sleep(500);
        Screengrab.screenshot("4");

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS);
        clickOn(R.id.chartChip);
        Screengrab.screenshot("6");

        clickOn(R.id.timeSpinner);

        clickListItem(1);

        clickOn(R.id.tableChip);
        Screengrab.screenshot("7");

        clickListItem(com.evrencoskun.tableview.R.id.ColumnHeaderRecyclerView, 1);

        AtomicReference<TableView> tableView = new AtomicReference<>();
        tableView.set(baristaRule.getActivityTestRule().getActivity().findViewById(R.id.reminder_table));

        TextView view = tableView.get().getCellRecyclerView().findViewWithTag("medicineName");
        assertEquals("Selen (200 µg)", view.getText());

        onView(withId(R.id.filter)).perform(replaceText("B"), closeSoftKeyboard());

        view = tableView.get().getCellRecyclerView().findViewWithTag("medicineName");
        assertEquals("B12 (500µg)", view.getText());

        clickOn(com.google.android.material.R.id.text_input_end_icon);

        clickOn(R.id.calendarChip);
        Screengrab.screenshot("8");

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
    }
}

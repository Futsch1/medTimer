package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withResourceName;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton;
import static org.junit.Assert.assertNotNull;

import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Test;

public class NotificationTest extends BaseTestHelper {

    @Test
    public void notificationTest() {
        AndroidTestHelper.createMedicine("Test med");

        // Set color and icon
        clickOn(R.id.enableColor);
        clickOn(R.id.selectColor);
        onView(withResourceName("colorPickerView")).perform(new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_LEFT, Press.FINGER, InputDevice.SOURCE_UNKNOWN, MotionEvent.BUTTON_PRIMARY));
        clickDialogPositiveButton();

        clickOn(R.id.selectIcon);
        onView(withResourceName("icd_rcv_icon_list")).perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));

        AndroidTestHelper.createReminder("1", AndroidTestHelper.getNextNotificationTime().toLocalTime());

        clickOn(R.id.open_advanced_settings);

        clickOn(R.id.addLinkedReminder);
        clickDialogPositiveButton();
        AndroidTestHelper.setTime(0, 1, true);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);

        baristaRule.getActivityTestRule().finishActivity();

        UiDevice device = UiDevice.getInstance(getInstrumentation());
        device.openNotification();
        UiObject2 object = device.wait(Until.findObject(By.textContains("Test med")), 240_000);
        assertNotNull(object);
        object.fling(Direction.RIGHT);
        object = device.wait(Until.findObject(By.textContains("Test med")), 180_000);
        assertNotNull(object);
        device.pressBack();
    }

}

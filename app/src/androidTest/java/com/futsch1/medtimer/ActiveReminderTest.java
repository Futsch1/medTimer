package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;

import org.junit.Test;

import java.util.Calendar;

public class ActiveReminderTest extends BaseTestHelper {
    @Test
    //@AllowFlaky(attempts = 1)
    public void activeReminderTest() {
        Calendar futureTime = Calendar.getInstance();
        int year = futureTime.get(Calendar.YEAR);
        futureTime.set(year + 1, 1, 1);
        Calendar pastTime = Calendar.getInstance();
        pastTime.set(year - 1, 1, 1);

        AndroidTestHelper.createMedicine("Test");
        AndroidTestHelper.createReminder("1", null);

        clickOn(R.id.open_advanced_settings);
        clickOn(R.id.inactive);

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertNotDisplayed(R.id.nextReminders);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.open_advanced_settings);
        clickOn(R.id.active);

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertListItemCount(R.id.nextReminders, 1);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.open_advanced_settings);
        clickOn(R.id.timePeriod);
        clickOn(R.id.periodStart);
        onView(withId(R.id.periodStartDate)).perform(replaceText(AndroidTestHelper.dateToString(futureTime.getTime())));

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertNotDisplayed(R.id.nextReminders);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.open_advanced_settings);
        clickOn(R.id.periodStart);
        clickOn(R.id.periodEnd);
        onView(withId(R.id.periodEndDate)).perform(replaceText(AndroidTestHelper.dateToString(pastTime.getTime())));

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertNotDisplayed(R.id.nextReminders);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.open_advanced_settings);
        clickOn(R.id.periodStart);
        onView(withId(R.id.periodStartDate)).perform(replaceText(AndroidTestHelper.dateToString(pastTime.getTime())));
        onView(withId(R.id.periodEndDate)).perform(replaceText(AndroidTestHelper.dateToString(futureTime.getTime())));

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertListItemCount(R.id.nextReminders, 1);
    }
}

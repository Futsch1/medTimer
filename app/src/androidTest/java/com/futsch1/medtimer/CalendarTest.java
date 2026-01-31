package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.pressBack;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild;
import static com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu;

import org.junit.Test;

public class CalendarTest extends BaseTestHelper {

    @Test
    //@AllowFlaky(attempts = 1)
    public void calendarTest() {
        openMenu();
        clickOn(R.string.generate_test_data);

        clickListItemChild(R.id.reminders, 0, R.id.stateButton);
        clickOn(R.id.takenButton);
        clickListItemChild(R.id.reminders, 2, R.id.stateButton);
        clickOn(R.id.takenButton);
        clickListItemChild(R.id.reminders, 3, R.id.stateButton);
        clickOn(R.id.takenButton);
        clickListItemChild(R.id.reminders, 4, R.id.stateButton);
        clickOn(R.id.takenButton);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.openCalendar);
        assertContains(R.id.currentDayEvents, "Omega 3 (EPA/DHA 500mg)");
        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS);

        clickOn(R.id.calendarChip);
        assertContains(R.id.currentDayEvents, "Selen (200 µg)");
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void testDeletedEventNotInCalendarView() {
        // Create event
        clickOn(R.id.logManualDose);
        clickOn(R.string.custom);
        writeTo(android.R.id.input, "Test");
        clickDialogPositiveButton();
        clickDialogPositiveButton();
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button);

        // Delete event
        clickListItemChild(R.id.reminders, 0, R.id.stateButton);
        clickOn(R.id.deleteButton);
        clickDialogPositiveButton();

        // Check that the event is not listed in the calendar view
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS);
        clickOn(R.id.calendarChip);
        assertNotContains(R.id.currentDayEvents, "Test");
    }
}

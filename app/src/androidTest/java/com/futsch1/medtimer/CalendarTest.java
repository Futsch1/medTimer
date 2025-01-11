package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.pressBack;
import static com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild;
import static com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu;

import androidx.test.filters.LargeTest;

import org.junit.Test;

@LargeTest
public class CalendarTest extends BaseTestHelper {

    @Test
    //@AllowFlaky(attempts = 1)
    public void calendarTest() {
        openMenu();
        clickOn(R.string.generate_test_data);

        if (isNotTimeBetween9And23()) {
            AndroidTestHelper.setAllRemindersTo12AM();
        }

        clickOn(R.id.showOnlyOpen);

        int takenReminders = 0;

        while (takenReminders < 10) {
            try {
                clickListItemChild(R.id.latestReminders, 0, R.id.chipTaken);
            } catch (Exception e) {
                break;
            }
            takenReminders++;
        }
        assertListItemCount(R.id.latestReminders, 0);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.openCalendar);
        assertContains(R.id.currentDayEvents, "Omega 3 (EPA/DHA 500mg)");
        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS);

        clickOn(R.id.calendarChip);
        assertContains(R.id.currentDayEvents, "Selen (200 µg)");
    }
}

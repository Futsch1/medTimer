package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild;
import static com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu;

import androidx.test.filters.LargeTest;

import com.adevinta.android.barista.rule.flaky.AllowFlaky;

import org.junit.Test;

@LargeTest
public class TestDataAndDeleteAndManualDoseTest extends BaseTestHelper {

    @Test
    @AllowFlaky(attempts = 1)
    public void testDataAndDeleteAndManualDoseTest() {
        openMenu();
        clickOn(R.string.generate_test_data);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        clickListItem(R.id.medicineList, 0);

        openMenu();
        clickOn(R.string.delete);
        clickOn(R.string.yes);
        assertListItemCount(R.id.medicineList, 3);

        clickListItem(R.id.medicineList, 2);
        clickListItemChild(R.id.reminderList, 1, R.id.open_advanced_settings);

        openMenu();
        clickOn(R.string.delete);
        clickOn(R.string.yes);
        assertListItemCount(R.id.reminderList, 1);

        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);

        clickOn(R.id.logManualDose);

        clickListItem(2);

        writeTo(android.R.id.input, "12");
        clickDialogPositiveButton();

        clickOn(com.google.android.material.R.id.material_timepicker_ok_button);

        String expectedText = getInstrumentation().getTargetContext().getString(R.string.reminder_event, "12", "Ginseng (200mg)", "");
        assertContains(R.id.reminderEventText, expectedText);
    }

}

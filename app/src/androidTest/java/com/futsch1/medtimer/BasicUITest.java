package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;
import static com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo;

import org.junit.Test;

public class BasicUITest extends BaseTestHelper {

    @Test
    //@AllowFlaky(attempts = 1)
    public void basicUITest() {
        AndroidTestHelper.createMedicine("Test");
        AndroidTestHelper.createReminder("1", null);

        clickOn(R.id.openAdvancedSettings);

        scrollTo(R.id.editInstructionsLayout);
        clickOn(com.google.android.material.R.id.text_input_end_icon);
        clickListItem(1);

        pressBack();

        clickOn(R.id.openAdvancedSettings);
        assertContains(R.string.before_meal);
        pressBack();

        writeTo(R.id.editAmount, "2");
        pressBack();

        clickListItem(R.id.medicineList, 0);
        assertDisplayed(R.id.editAmount, "2");
        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        String expectedText = getInstrumentation().getTargetContext().getString(R.string.reminder_event, "2", "Test", "");
        assertContains(expectedText);
    }

}

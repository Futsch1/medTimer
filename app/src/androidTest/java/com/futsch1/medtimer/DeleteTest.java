package com.futsch1.medtimer;


import static com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild;
import static com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu;

import org.junit.Test;

public class DeleteTest extends BaseTestHelper {

    @Test
    //@AllowFlaky(attempts = 1)
    public void testDelete() {
        openMenu();
        clickOn(R.string.generate_test_data);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        clickListItem(R.id.medicineList, 0);

        openMenu();
        clickOn(R.string.delete);
        clickOn(R.string.yes);
        assertListItemCount(R.id.medicineList, 3);

        clickListItem(R.id.medicineList, 2);
        clickListItemChild(R.id.reminderList, 1, R.id.openAdvancedSettings);

        openMenu();
        clickOn(R.string.delete);
        clickOn(R.string.yes);
        assertListItemCount(R.id.reminderList, 1);
    }

}

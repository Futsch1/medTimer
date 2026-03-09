package com.futsch1.medtimer

import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import org.junit.Test


class DeleteTest : BaseTestHelper() {
    //@AllowFlaky(attempts = 1)
    @Test
    fun testDelete() {
        openMenu()
        clickOn(R.string.generate_test_data)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        clickListItem(R.id.medicineList, 0)

        openMenu()
        clickOn(R.string.delete)
        clickOn(R.string.yes)
        assertListItemCount(R.id.medicineList, 3)

        clickListItem(R.id.medicineList, 2)
        clickListItemChild(R.id.reminderList, 1, R.id.openAdvancedSettings)

        openMenu()
        clickOn(R.string.delete)
        clickOn(R.string.yes)
        assertListItemCount(R.id.reminderList, 1)
    }
}

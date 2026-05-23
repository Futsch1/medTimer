package com.futsch1.medtimer

import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import org.junit.Test


class DeleteTest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 3)
    fun testDelete() {
        openMenu()
        clickOn(com.futsch1.medtimer.feature.ui.R.string.generate_test_data)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)

        openMenu()
        clickOn(com.futsch1.medtimer.feature.ui.R.string.delete)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.yes)
        assertListItemCount(com.futsch1.medtimer.feature.ui.R.id.medicineList, 3)

        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 2)
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminderList, 1, com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)

        openMenu()
        clickOn(com.futsch1.medtimer.feature.ui.R.string.delete)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.yes)
        assertListItemCount(com.futsch1.medtimer.feature.ui.R.id.reminderList, 1)
    }
}

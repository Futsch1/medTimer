package com.futsch1.medtimer

import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import org.junit.Test

class ManualDoseTest : BaseTestHelper() {
    @Test
    //@AllowFlaky(attempts = 1)
    fun testManualDose() {
        openMenu()
        clickOn(R.string.generate_test_data)

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        clickOn(R.id.logManualDose)

        clickListItem(position = 3)

        writeTo(android.R.id.input, "12")
        BaristaDialogInteractions.clickDialogPositiveButton()

        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        assertContains(R.id.reminderEventText, "Ginseng (200mg) (12)")
    }

}
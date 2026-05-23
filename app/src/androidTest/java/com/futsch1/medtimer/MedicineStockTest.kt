package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.assertion.BaristaErrorAssertions.assertErrorDisplayed
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertCustomAssertionAtPosition
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.AndroidTestHelper.setValue
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import com.futsch1.medtimer.utilities.openNotification
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

class MedicineStockTest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 3)
    fun medicineStockTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationTitle = context.getString(com.futsch1.medtimer.core.ui.R.string.out_of_stock_notification_title)

        AndroidTestHelper.createMedicine("Test")
        // Interval reminder (amount 3.5) 10 minutes from now
        AndroidTestHelper.createIntervalReminder("Of the pills 3.5 are to be taken", 10)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.amount)
        setValue("")
        clickOn(com.futsch1.medtimer.feature.ui.R.string.refill_size)
        setValue("")
        pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.amount)
        setValue("10.5")
        clickOn(com.futsch1.medtimer.feature.ui.R.string.unit)
        setValue("pills")
        clickOn(com.futsch1.medtimer.feature.ui.R.string.refill_size)
        setValue("10.8")
        pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.stockReminderCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editStockThreshold, "4")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)

        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        // Mark reminder as taken, no out of stock reminder expected (7 left)
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenButton)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        checkNotificationWithTitle(device, notificationTitle, false)

        // Mark reminder as skipped (10.5 left)
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.skippedButton)
        checkNotificationWithTitle(device, notificationTitle, false)

        // Mark reminder as taken again, no out of stock reminder expected (7 left)
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenButton)
        checkNotificationWithTitle(device, notificationTitle, false)

        // Mark next instance as taken, out of stock reminder expected (3.5 left)
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 1, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenButton)
        checkNotificationWithTitle(device, notificationTitle, true, dismiss = false, additionalExpectedString = "3.5")

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        assertContains(com.futsch1.medtimer.feature.ui.R.id.medicineName, "⚠")
        assertContains(com.futsch1.medtimer.feature.ui.R.id.medicineName, "pills")
        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(3.5, "pills"))
        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.logManualDose)
        clickListItem(null, 1)
        writeTo(android.R.id.input, "12")
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.refill_now)

        assertDisplayed(MedicineHelper.formatAmount(10.8, "pills"))
        pressBack()
        pressBack()

        assertContains(com.futsch1.medtimer.feature.ui.R.id.medicineName, MedicineHelper.formatAmount(10.8, "pills"))
        assertNotContains(com.futsch1.medtimer.feature.ui.R.id.medicineName, "⚠")
        assertContains(com.futsch1.medtimer.feature.ui.R.id.medicineName, "pills")
    }

    private fun checkNotificationWithTitle(
        device: UiDevice,
        notificationTitle: String,
        expected: Boolean,
        dismiss: Boolean = false,
        additionalExpectedString: String? = null
    ) {
        openNotification().use {
            val notificationTitleObject = device.wait(
                Until.findObject(By.textContains(notificationTitle)),
                1_000
            )
            if (expected) {
                internalAssert(notificationTitleObject != null)
            } else {
                internalAssert(notificationTitleObject == null)
            }
            if (additionalExpectedString != null) {
                internalAssert(device.findObject(By.textContains(additionalExpectedString)) != null)
            }

            if (dismiss) {
                notificationTitleObject.fling(Direction.RIGHT)
            }
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun hiddenMedicineNameInStockReminder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        openMenu()
        clickOn(com.futsch1.medtimer.feature.ui.R.string.tab_settings)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.privacy_settings)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.hide_med_name)

        AndroidTestHelper.createMedicine("TestMed")

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.amount)
        setValue("120")
        clickOn(com.futsch1.medtimer.feature.ui.R.string.unit)
        setValue("pills")
        AndroidTestHelper.scrollDown()
        clickOn(com.futsch1.medtimer.feature.ui.R.string.refill_size)
        setValue("100")
        pressBack()

        AndroidTestHelper.createIntervalReminder("So many pills - 130", 10)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.stockReminderCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editStockThreshold, "0")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenButton)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        openNotification().use {
            device.wait(
                Until.findObject(By.textContains(context.getString(com.futsch1.medtimer.core.ui.R.string.out_of_stock_notification_title).substring(0, 30))),
                1_000
            )
            internalAssert(device.findObject(By.textContains("T******")) != null)
            internalAssert(device.findObject(By.textContains("TestMed")) == null)
            clickNotificationButton(device, getNotificationText(com.futsch1.medtimer.feature.reminders.R.string.refill_amount, "100"))
        }

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(100.0, "pills"))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun reminderAmountWarningTest() {
        AndroidTestHelper.createMedicine("Test")

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.amount)
        setValue("10.5")
        clickOn(com.futsch1.medtimer.feature.ui.R.string.unit)
        setValue("pills")
        pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.stockReminderCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editStockThreshold, "4")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.timeBasedCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editAmount, "something")

        assertErrorDisplayed(com.futsch1.medtimer.feature.ui.R.id.editAmount, com.futsch1.medtimer.feature.ui.R.string.invalid_amount)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun bigStockAmounts() {
        AndroidTestHelper.createMedicine("Test")

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.amount)
        setValue("10005")
        clickOn(com.futsch1.medtimer.feature.ui.R.string.unit)
        setValue("pills")

        pressBack()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(10005.0, "pills"))
        assertDisplayed("---")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun runOutDate() {
        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("3", LocalTime.of(1, 0))

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.amount)
        setValue("10")
        assertDisplayed(timeFormatter().localDateToString(LocalDate.now().plusDays(4)))

        clickOn(com.futsch1.medtimer.feature.ui.R.string.amount)
        setValue("13")
        assertDisplayed(timeFormatter().localDateToString(LocalDate.now().plusDays(5)))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun allTaken() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        openMenu()
        clickOn(com.futsch1.medtimer.feature.ui.R.string.tab_settings)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.display_settings)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.combine_notifications)

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createReminder("3", LocalTime.of(22, 0))
        AndroidTestHelper.createReminder("2", LocalTime.of(22, 0))

        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.stockReminderCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editStockThreshold, "12")
        clickOn(com.futsch1.medtimer.core.ui.R.string.daily_below_threshold)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.editReminderTime)
        AndroidTestHelper.setTime(22, 0, false)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.amount)
        setValue("10")
        pressBack()

        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(InstrumentationRegistry.getInstrumentation().targetContext)
        openNotification().use {
            device.wait(Until.findObject(By.textContains(TEST_MED)), 5_000)
            internalAssert(clickNotificationButton(device, getNotificationText(com.futsch1.medtimer.core.ui.R.string.taken)))
        }
        device.waitForIdle(2_000)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        assertDisplayed("5")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun expirationDateTest() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationTitle = context.getString(com.futsch1.medtimer.core.ui.R.string.expiration_reminder)

        val expirationTime = Calendar.getInstance()
        val day = expirationTime.get(Calendar.DAY_OF_MONTH)
        expirationTime.set(Calendar.DAY_OF_MONTH, day + 7)

        AndroidTestHelper.createMedicine("Test")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        AndroidTestHelper.scrollDown()
        clickOn(com.futsch1.medtimer.core.ui.R.string.expiration_date)
        AndroidTestHelper.setDate(expirationTime.time)
        AndroidTestHelper.scrollDown()
        clickOn(com.futsch1.medtimer.feature.ui.R.string.clear_dates)

        pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.expirationDateReminderCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editExpirationDaysBefore, "10")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        checkNotificationWithTitle(device, notificationTitle, false)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        AndroidTestHelper.scrollDown()
        clickOn(com.futsch1.medtimer.core.ui.R.string.expiration_date)
        AndroidTestHelper.setDate(expirationTime.time)
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.R.id.stateButton,
            matches(withTagValue(equalTo(com.futsch1.medtimer.feature.ui.R.drawable.bell)))
        )
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.id.acknowledgedButton)
        pressBack()

        checkNotificationWithTitle(device, notificationTitle, expected = true, dismiss = true)

        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.R.id.stateButton,
            matches(withTagValue(equalTo(com.futsch1.medtimer.core.ui.R.drawable.check2_circle)))
        )
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.deleteButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.yes)

        checkNotificationWithTitle(device, notificationTitle, false)
        assertListItemCount(com.futsch1.medtimer.feature.ui.R.id.reminders, 0)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun undoStockOnDeleteTest() {
        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("2", LocalTime.of(20, 0))

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.amount)
        setValue("10")
        clickOn(com.futsch1.medtimer.feature.ui.R.string.unit)
        setValue("pills")
        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenButton)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(8.0, "pills"))
        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.deleteButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.yes)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(10.0, "pills"))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun undoStockOnReraiseTest() {
        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("2", LocalTime.of(20, 0))

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.amount)
        setValue("10")
        clickOn(com.futsch1.medtimer.feature.ui.R.string.unit)
        setValue("pills")
        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenButton)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(8.0, "pills"))
        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.reraiseButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.yes)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(10.0, "pills"))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun dailyStockReminderTest() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationTitle = context.getString(com.futsch1.medtimer.core.ui.R.string.out_of_stock_notification_title)

        AndroidTestHelper.createMedicine("Test")

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openStockTracking)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.amount)
        setValue("10.5")
        pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.stockReminderCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editStockThreshold, "14")
        clickOn(com.futsch1.medtimer.core.ui.R.string.daily_below_threshold)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.editReminderTime)
        AndroidTestHelper.setTime(22, 0, false)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)

        checkNotificationWithTitle(device, notificationTitle, false)
        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(context)
        checkNotificationWithTitle(device, notificationTitle, true)
    }
}

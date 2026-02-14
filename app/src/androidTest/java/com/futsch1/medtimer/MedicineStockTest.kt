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
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.AndroidTestHelper.setValue
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

class MedicineStockTest : BaseTestHelper() {

    @Test
    //@AllowFlaky(attempts = 1)
    fun medicineStockTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationTitle = context.getString(R.string.out_of_stock_notification_title)

        AndroidTestHelper.createMedicine("Test")
        // Interval reminder (amount 3.5) 10 minutes from now
        AndroidTestHelper.createIntervalReminder("Of the pills 3.5 are to be taken", 10)

        clickOn(R.id.openStockTracking)
        clickOn(R.string.amount)
        setValue("")
        clickOn(R.string.refill_size)
        setValue("")
        pressBack()

        clickOn(R.id.openStockTracking)
        clickOn(R.string.amount)
        setValue("10.5")
        clickOn(R.string.unit)
        setValue("pills")
        clickOn(R.string.refill_size)
        setValue("10.8")
        pressBack()

        clickOn(R.id.addReminder)
        clickOn(R.id.stockReminderCard)
        writeTo(R.id.editStockThreshold, "4")
        clickOn(R.id.createReminder)

        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        // Mark reminder as taken, no out of stock reminder expected (7 left)
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        checkNotificationWithTitle(device, notificationTitle, false)

        // Mark reminder as skipped (10.5 left)
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.skippedButton)
        checkNotificationWithTitle(device, notificationTitle, false)

        // Mark reminder as taken again, no out of stock reminder expected (7 left)
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)
        checkNotificationWithTitle(device, notificationTitle, false)

        // Mark next instance as taken, out of stock reminder expected (3.5 left)
        clickListItemChild(R.id.reminders, 1, R.id.stateButton)
        clickOn(R.id.takenButton)
        checkNotificationWithTitle(device, notificationTitle, true)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        assertContains(R.id.medicineName, "⚠")
        assertContains(R.id.medicineName, "pills")
        clickListItem(R.id.medicineList, 0)
        clickOn(R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(3.5, "pills"))
        pressBack()
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickOn(R.id.logManualDose)
        clickListItem(null, 1)
        writeTo(android.R.id.input, "12")
        BaristaDialogInteractions.clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        clickListItem(R.id.medicineList, 0)
        clickOn(R.id.openStockTracking)
        clickOn(R.string.refill_now)

        assertDisplayed(MedicineHelper.formatAmount(10.8, "pills"))
        pressBack()
        pressBack()

        assertContains(R.id.medicineName, MedicineHelper.formatAmount(10.8, "pills"))
        assertNotContains(R.id.medicineName, "⚠")
        assertContains(R.id.medicineName, "pills")
    }

    private fun checkNotificationWithTitle(device: UiDevice, notificationTitle: String, expected: Boolean, dismiss: Boolean = false) {
        device.openNotification()
        val o = device.wait(
            Until.findObject(By.textContains(notificationTitle)),
            1_000
        )
        if (expected) {
            internalAssert(o != null)
        } else {
            internalAssert(o == null)
        }
        if (dismiss) {
            o.fling(Direction.RIGHT)
        }
        device.pressBack()
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun hiddenMedicineNameInStockReminder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.privacy_settings)
        clickOn(R.string.hide_med_name)

        AndroidTestHelper.createMedicine("TestMed")

        clickOn(R.id.openStockTracking)
        clickOn(R.string.amount)
        setValue("120")
        clickOn(R.string.unit)
        setValue("pills")
        AndroidTestHelper.scrollDown()
        clickOn(R.string.refill_size)
        setValue("100")
        pressBack()

        AndroidTestHelper.createIntervalReminder("So many pills - 130", 10)

        clickOn(R.id.addReminder)
        clickOn(R.id.stockReminderCard)
        writeTo(R.id.editStockThreshold, "0")
        clickOn(R.id.createReminder)
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        device.wait(
            Until.findObject(By.textContains(context.getString(R.string.out_of_stock_notification_title).substring(0, 30))),
            1_000
        )
        internalAssert(device.findObject(By.textContains("T******")) != null)
        internalAssert(device.findObject(By.textContains("TestMed")) == null)
        clickNotificationButton(device, getNotificationText(R.string.refill_amount, "100"))
        device.pressBack()

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        clickListItem(R.id.medicineList, 0)
        clickOn(R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(100.0, "pills"))
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun reminderAmountWarningTest() {
        AndroidTestHelper.createMedicine("Test")

        clickOn(R.id.openStockTracking)
        clickOn(R.string.amount)
        setValue("10.5")
        clickOn(R.string.unit)
        setValue("pills")
        pressBack()

        clickOn(R.id.addReminder)
        clickOn(R.id.stockReminderCard)
        writeTo(R.id.editStockThreshold, "4")
        clickOn(R.id.createReminder)

        clickOn(R.id.addReminder)
        clickOn(R.id.timeBasedCard)
        writeTo(R.id.editAmount, "something")

        assertErrorDisplayed(R.id.editAmount, R.string.invalid_amount)
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun bigStockAmounts() {
        AndroidTestHelper.createMedicine("Test")

        clickOn(R.id.openStockTracking)
        clickOn(R.string.amount)
        setValue("10005")
        clickOn(R.string.unit)
        setValue("pills")

        pressBack()
        clickOn(R.id.openStockTracking)
        assertDisplayed(MedicineHelper.formatAmount(10005.0, "pills"))
        assertDisplayed("---")
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun runOutDate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("3", LocalTime.of(1, 0))

        clickOn(R.id.openStockTracking)
        clickOn(R.string.amount)
        setValue("10")
        assertDisplayed(TimeHelper.localDateToString(context, LocalDate.now().plusDays(4)))

        clickOn(R.string.amount)
        setValue("13")
        assertDisplayed(TimeHelper.localDateToString(context, LocalDate.now().plusDays(5)))
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun allTaken() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.display_settings)
        clickOn(R.string.combine_notifications)

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createReminder("3", LocalTime.of(22, 0))
        AndroidTestHelper.createReminder("2", LocalTime.of(22, 0))

        clickOn(R.id.openStockTracking)
        clickOn(R.string.amount)
        setValue("10")
        pressBack()

        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(InstrumentationRegistry.getInstrumentation().targetContext, 0, 1)
        sleep(2_000)
        device.openNotification()
        device.wait(Until.findObject(By.textContains(TEST_MED)), 2_000)
        internalAssert(clickNotificationButton(device, getNotificationText(R.string.taken)))
        device.pressBack()
        sleep(1_000)

        clickOn(R.id.openStockTracking)
        assertDisplayed("5")
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun expirationDateTest() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationTitle = context.getString(R.string.expiration_reminder)

        val expirationTime = Calendar.getInstance()
        val day = expirationTime.get(Calendar.DAY_OF_MONTH)
        expirationTime.set(Calendar.DAY_OF_MONTH, day + 7)

        AndroidTestHelper.createMedicine("Test")
        clickOn(R.id.openStockTracking)
        clickOn(R.string.expiration_date)
        AndroidTestHelper.setDate(expirationTime.time)
        AndroidTestHelper.scrollDown()
        clickOn(R.string.clear_dates)

        pressBack()

        clickOn(R.id.addReminder)
        clickOn(R.id.expirationDateReminderCard)
        writeTo(R.id.editExpirationDaysBefore, "10")
        clickOn(R.id.createReminder)
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        checkNotificationWithTitle(device, notificationTitle, false)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        clickListItem(R.id.medicineList, 0)
        clickOn(R.id.openStockTracking)
        clickOn(R.string.expiration_date)
        AndroidTestHelper.setDate(expirationTime.time)
        pressBack()

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        assertCustomAssertionAtPosition(
            R.id.reminders,
            0,
            R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.bell)))
        )
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        assertDisplayed(R.id.acknowledgedButton)
        pressBack()

        checkNotificationWithTitle(device, notificationTitle, expected = true, dismiss = true)

        assertCustomAssertionAtPosition(
            R.id.reminders,
            0,
            R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.check2_circle)))
        )
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.deleteButton)
        clickOn(R.string.yes)

        checkNotificationWithTitle(device, notificationTitle, false)
        assertListItemCount(R.id.reminders, 0)
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun dailyStockReminderTest() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationTitle = context.getString(R.string.out_of_stock_notification_title)

        AndroidTestHelper.createMedicine("Test")

        clickOn(R.id.openStockTracking)
        clickOn(R.string.amount)
        setValue("10.5")
        pressBack()

        clickOn(R.id.addReminder)
        clickOn(R.id.stockReminderCard)
        writeTo(R.id.editStockThreshold, "14")
        clickOn(R.string.daily_below_threshold)
        clickOn(R.id.editReminderTime)
        AndroidTestHelper.setTime(22, 0, false)
        clickOn(R.id.createReminder)

        checkNotificationWithTitle(device, notificationTitle, false)
        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(context)
        checkNotificationWithTitle(device, notificationTitle, true)
    }
}

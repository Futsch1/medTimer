package com.futsch1.medtimer;

import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.MedicineHelper;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class MedicineHelperTest extends TestCase {

    public void testGetMaxDaysBetweenReminders() {
        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 2);
        medicineWithReminders.reminders.add(reminder);
        Reminder reminder2 = TestHelper.buildReminder(1, 2, "2", 481, 1);
        medicineWithReminders.reminders.add(reminder2);
        Reminder reminder3 = TestHelper.buildReminder(1, 2, "2", 481, 4);
        medicineWithReminders.reminders.add(reminder3);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);
        int expectedMaxDaysBetweenReminders = 4;
        int actualMaxDaysBetweenReminders = MedicineHelper.getMaxDaysBetweenReminders(medicineList);

        assertEquals(expectedMaxDaysBetweenReminders, actualMaxDaysBetweenReminders);
    }
}
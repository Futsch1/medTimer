package com.futsch1.medtimer;

import static com.futsch1.medtimer.ReminderSchedulerUnitTest.TEST_1;
import static com.futsch1.medtimer.ReminderSchedulerUnitTest.getScheduler;
import static com.futsch1.medtimer.TestHelper.assertReminded;
import static com.futsch1.medtimer.TestHelper.on;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.preferences.PreferencesNames;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReminderSchedulerWeekendModeTest {
    @Test
    public void testWeekendDaysEmpty() {
        ReminderScheduler scheduler = getScheduler();

        Mockito.when(scheduler.getSharedPreferences().getBoolean(PreferencesNames.WEEKEND_MODE, false)).thenReturn(true);
        Mockito.when(scheduler.getSharedPreferences().getStringSet(eq(PreferencesNames.WEEKEND_DAYS), any())).thenReturn(new HashSet<>());

        FullMedicine medicineWithReminders1 = TestHelper.buildFullMedicine(1, TEST_1);
        Reminder reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1);
        medicineWithReminders1.reminders.add(reminder1);
        List<ScheduledReminder> scheduledReminders = scheduler.schedule(new ArrayList<>() {{
            add(medicineWithReminders1);
        }}, new ArrayList<>());
        assertReminded(scheduledReminders, on(1, 16), medicineWithReminders1.medicine, reminder1);
    }

    @Test
    public void testWeekendMode() {
        // 1.1.1970 is a Thursday
        ReminderScheduler scheduler = getScheduler();

        Mockito.when(scheduler.getSharedPreferences().getBoolean(eq(PreferencesNames.WEEKEND_MODE), anyBoolean())).thenReturn(true);
        Mockito.when(scheduler.getSharedPreferences().getInt(eq(PreferencesNames.WEEKEND_TIME), anyInt())).thenReturn(10 * 60);
        Set<String> weekendDays = new HashSet<>();
        weekendDays.add(String.valueOf(DayOfWeek.SATURDAY.getValue()));
        weekendDays.add(String.valueOf(DayOfWeek.SUNDAY.getValue()));
        Mockito.when(scheduler.getSharedPreferences().getStringSet(eq(PreferencesNames.WEEKEND_DAYS), any())).thenReturn(weekendDays);

        FullMedicine medicineWithReminders1 = TestHelper.buildFullMedicine(1, TEST_1);
        Reminder reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1);
        medicineWithReminders1.reminders.add(reminder1);
        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders1);

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, new ArrayList<>());
        assertReminded(scheduledReminders, on(1, 16), medicineWithReminders1.medicine, reminder1);

        Mockito.when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(2));

        scheduledReminders = scheduler.schedule(medicineList, new ArrayList<>());
        assertReminded(scheduledReminders, on(3, 10 * 60), medicineWithReminders1.medicine, reminder1);
    }
}


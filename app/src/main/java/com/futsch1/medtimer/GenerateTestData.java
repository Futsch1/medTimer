package com.futsch1.medtimer;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;

import java.time.LocalDate;

public class GenerateTestData {
    private final MedicineViewModel viewModel;

    public GenerateTestData(MedicineViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void deleteAll() {
        viewModel.deleteAll();
    }

    public void generateTestMedicine() {
        TestMedicine[] testMedicines = new TestMedicine[]{
                new TestMedicine("Omega 3 (EPA/DHA 500mg)", null, new TestReminder[]{
                        new TestReminder("1", 9 * 60, 1, 0, ""),
                        new TestReminder("1", 18 * 60, 2, 2, "after meals")
                }),
                new TestMedicine("B12 (500µg)", 0xFF8b0000, new TestReminder[]{
                        new TestReminder("2", 7 * 60, 1, 0, "")
                }),
                new TestMedicine("Ginseng (200mg)", 0xFF90EE90, new TestReminder[]{
                        new TestReminder("1", 9 * 60, 1, 0, "before breakfast")
                }),
                new TestMedicine("Selen (200 µg)", null, new TestReminder[]{
                        new TestReminder("2", 9 * 60, 1, 0, ""),
                        new TestReminder("1", 18 * 60, 1, 1, "")
                })
        };

        for (TestMedicine testMedicine : testMedicines) {
            Medicine medicine = new Medicine(testMedicine.name);
            if (testMedicine.color != null) {
                medicine.useColor = true;
                medicine.color = testMedicine.color;
            }
            int medicineId = viewModel.insertMedicine(medicine);
            for (TestReminder testReminder : testMedicine.reminders) {
                Reminder reminder = new Reminder(medicineId);
                reminder.amount = testReminder.amount;
                reminder.timeInMinutes = testReminder.time;
                reminder.consecutiveDays = testReminder.consecutiveDays;
                reminder.instructions = testReminder.instructions;
                reminder.pauseDays = testReminder.pauseDays;
                reminder.cycleStartDay = LocalDate.now().toEpochDay();
                viewModel.insertReminder(reminder);
            }
        }
    }

    @SuppressWarnings("java:S6218")
    private record TestMedicine(String name, Integer color, TestReminder[] reminders) {
        // Record, intentionally empty
    }

    private record TestReminder(String amount, int time, int consecutiveDays,
                                int pauseDays,
                                String instructions) {
        // Record, intentionally empty
    }
}

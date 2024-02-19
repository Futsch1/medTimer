package com.futsch1.medtimer;

import androidx.annotation.Nullable;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;

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
                        new TestReminder("1", 9 * 60),
                        new TestReminder("1", 18 * 60)
                }),
                new TestMedicine("B12 (500µg)", 0xFF8b0000, new TestReminder[]{
                        new TestReminder("2", 7 * 60)
                }),
                new TestMedicine("Ginseng (200mg)", 0xFF90EE90, new TestReminder[]{
                        new TestReminder("1", 9 * 60)
                }),
                new TestMedicine("Selen (200 µg)", null, new TestReminder[]{
                        new TestReminder("2", 9 * 60),
                        new TestReminder("1", 18 * 60)
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
                viewModel.insertReminder(reminder);
            }
        }
    }

    private static class TestMedicine {
        String name;
        Integer color;
        TestReminder[] reminders;

        public TestMedicine(String name, @Nullable Integer color, TestReminder[] reminders) {
            this.name = name;
            this.color = color;
            this.reminders = reminders;
        }
    }

    private static class TestReminder {
        String amount;
        int time;

        public TestReminder(String amount, int time) {
            this.amount = amount;
            this.time = time;
        }
    }
}

package com.futsch1.medtimer.database;

import com.google.gson.GsonBuilder;

import java.time.Instant;
import java.util.List;

public class JSONMedicineBackup extends JSONBackup<MedicineWithReminders> {

    public JSONMedicineBackup() {
        super(MedicineWithReminders.class);
    }

    @Override
    protected GsonBuilder registerTypeAdapters(GsonBuilder builder) {
        return builder
                .registerTypeAdapter(Medicine.class, new FullDeserialize<Medicine>())
                .registerTypeAdapter(Reminder.class, new FullDeserialize<Reminder>());
    }

    public void applyBackup(List<MedicineWithReminders> listOfMedicineWithReminders, MedicineRepository medicineRepository) {
        medicineRepository.deleteReminders();
        medicineRepository.deleteMedicines();

        for (MedicineWithReminders medicineWithReminders : listOfMedicineWithReminders) {
            long medicineId = medicineRepository.insertMedicine(medicineWithReminders.medicine);
            for (Reminder reminder : medicineWithReminders.reminders) {
                reminder.medicineRelId = (int) medicineId;
                reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000;
                medicineRepository.insertReminder(reminder);
            }
        }
    }
}

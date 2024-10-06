package com.futsch1.medtimer.database;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.time.Instant;
import java.util.List;

public class JSONMedicineBackup extends JSONBackup<MedicineWithReminders> {

    public JSONMedicineBackup() {
        super(MedicineWithReminders.class);
    }

    @Override
    public JsonElement createBackup(int databaseVersion, List<MedicineWithReminders> list) {
        // Fix the medicines where the instructions are null
        for (MedicineWithReminders medicineWithReminders : list) {
            medicineWithReminders.reminders.stream().filter(reminder -> reminder.instructions == null).forEach(reminder -> reminder.instructions = "");
        }
        return super.createBackup(databaseVersion, list);
    }

    @Override
    protected GsonBuilder registerTypeAdapters(GsonBuilder builder) {
        return builder
                .registerTypeAdapter(Medicine.class, new FullDeserialize<Medicine>())
                .registerTypeAdapter(Reminder.class, new FullDeserialize<Reminder>());
    }

    protected boolean isInvalid(MedicineWithReminders medicineWithReminders) {
        return medicineWithReminders == null || medicineWithReminders.medicine == null || medicineWithReminders.reminders == null;
    }

    public void applyBackup(List<MedicineWithReminders> listOfMedicineWithReminders, MedicineRepository medicineRepository) {
        medicineRepository.deleteReminders();
        medicineRepository.deleteMedicines();

        for (MedicineWithReminders medicineWithReminders : listOfMedicineWithReminders) {
            long medicineId = medicineRepository.insertMedicine(medicineWithReminders.medicine);
            processReminders(medicineRepository, medicineWithReminders, (int) medicineId);
        }
    }

    private static void processReminders(MedicineRepository medicineRepository, MedicineWithReminders medicineWithReminders, int medicineId) {
        for (Reminder reminder : medicineWithReminders.reminders) {
            if (reminder != null) {
                reminder.medicineRelId = medicineId;
                reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000;
                medicineRepository.insertReminder(reminder);
            }
        }
    }
}

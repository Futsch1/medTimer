package com.futsch1.medtimer.database;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.time.Instant;
import java.util.List;

public class JSONMedicineBackup extends JSONBackup<FullMedicine> {

    public JSONMedicineBackup() {
        super(FullMedicine.class);
    }

    @Override
    public JsonElement createBackup(int databaseVersion, List<FullMedicine> list) {
        // Fix the medicines where the instructions are null
        for (FullMedicine FullMedicine : list) {
            FullMedicine.reminders.stream().filter(reminder -> reminder.instructions == null).forEach(reminder -> reminder.instructions = "");
        }
        return super.createBackup(databaseVersion, list);
    }

    @Override
    protected GsonBuilder registerTypeAdapters(GsonBuilder builder) {
        return builder
                .registerTypeAdapter(Medicine.class, new FullDeserialize<Medicine>())
                .registerTypeAdapter(Tag.class, new FullDeserialize<>())
                .registerTypeAdapter(Reminder.class, new FullDeserialize<Reminder>());
    }

    protected boolean isInvalid(FullMedicine fullMedicine) {
        return fullMedicine == null || fullMedicine.medicine == null || fullMedicine.reminders == null;
    }

    public void applyBackup(List<FullMedicine> listOfFullMedicine, MedicineRepository medicineRepository) {
        medicineRepository.deleteReminders();
        medicineRepository.deleteMedicines();
        medicineRepository.deleteTags();

        var sortOrder = 1.0;

        for (FullMedicine fullMedicine : listOfFullMedicine) {
            if (fullMedicine.medicine.sortOrder == 0.0) {
                fullMedicine.medicine.sortOrder = sortOrder++;
            }
            long medicineId = medicineRepository.insertMedicine(fullMedicine.medicine);
            processReminders(medicineRepository, fullMedicine, (int) medicineId);
            processTags(medicineRepository, fullMedicine, (int) medicineId);
        }
    }

    private static void processReminders(MedicineRepository medicineRepository, FullMedicine fullMedicine, int medicineId) {
        for (Reminder reminder : fullMedicine.reminders) {
            if (reminder != null) {
                reminder.medicineRelId = medicineId;
                reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000;
                medicineRepository.insertReminder(reminder);
            }
        }
    }

    private void processTags(MedicineRepository medicineRepository, FullMedicine fullMedicine, int medicineId) {
        for (Tag tag : fullMedicine.tags) {
            int tagId = (int) medicineRepository.insertTag(tag);
            medicineRepository.insertMedicineToTag(medicineId, tagId);
        }
    }
}

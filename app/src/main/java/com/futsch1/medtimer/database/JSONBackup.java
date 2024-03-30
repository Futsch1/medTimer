package com.futsch1.medtimer.database;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;

public class JSONBackup {
    public String createBackup(int databaseVersion, List<MedicineWithReminders> medicinesWithReminders) {
        DatabaseContentWithVersion content = new DatabaseContentWithVersion(databaseVersion, medicinesWithReminders);
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        return gson.toJson(content);
    }

    public @Nullable List<MedicineWithReminders> parseBackup(String jsonFile) {
        // In a first step, parse with the version set to 0
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setVersion(0.0)
                .registerTypeAdapter(Medicine.class, new FullDeserialize<Medicine>())
                .registerTypeAdapter(Reminder.class, new FullDeserialize<Reminder>())
                .create();
        try {
            DatabaseContentWithVersion content = gson.fromJson(jsonFile, DatabaseContentWithVersion.class);
            gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setVersion(content.version).create();
            return gson.fromJson(jsonFile, DatabaseContentWithVersion.class).medicinesWithReminders;
        } catch (JsonParseException e) {
            Log.e("JSONBackup", e.getMessage());
            return null;
        }
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

    private record DatabaseContentWithVersion(@Expose int version,
                                              @Expose List<MedicineWithReminders> medicinesWithReminders) {
    }

    private static class FullDeserialize<T> implements JsonDeserializer<T> {

        public T deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
            T pojo = new Gson().fromJson(je, type);

            Field[] fields = pojo.getClass().getDeclaredFields();
            for (Field f : fields) {
                try {
                    if (f.get(pojo) == null) {
                        throw new JsonParseException("Missing field in JSON: " + f.getName());
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Log.e("JSONBackup", "Internal error");
                }

            }
            return pojo;

        }
    }
}

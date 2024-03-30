package com.futsch1.medtimer.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.List;

public class JSONBackup {
    public String createBackup(int databaseVersion, List<MedicineWithReminders> medicinesWithReminders) {
        DatabaseContentWithVersion content = new DatabaseContentWithVersion(databaseVersion, medicinesWithReminders);
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        return gson.toJson(content);
    }

    private record DatabaseContentWithVersion(@Expose int version,
                                              @Expose List<MedicineWithReminders> medicinesWithReminders) {
    }
}

package com.futsch1.medtimer;

import static org.mockito.Mockito.mock;

import android.util.Log;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.futsch1.medtimer.database.JSONBackup;
import com.futsch1.medtimer.database.JSONMedicineBackup;
import com.futsch1.medtimer.database.JSONReminderEventBackup;
import com.futsch1.medtimer.database.MedicineRepository;

import java.util.List;

@SuppressWarnings("java:S2187")
public class JSONBackupFuzzTest {
    @FuzzTest
    void fuzzTestMedicineBackup(String json) {
        JSONMedicineBackup jsonMedicineBackup = new JSONMedicineBackup();
        JSONReminderEventBackup jsonReminderEventBackup = new JSONReminderEventBackup();

        Log.enable = false;

        checkBackup(jsonMedicineBackup, json);
        checkBackup(jsonReminderEventBackup, json);
    }

    private <T> void checkBackup(JSONBackup<T> backup, String json) {
        List<T> parsedData = backup.parseBackup(json);
        MedicineRepository medicineRepository = mock(MedicineRepository.class);
        if (parsedData != null) {
            backup.applyBackup(parsedData, medicineRepository);
        }
    }
}

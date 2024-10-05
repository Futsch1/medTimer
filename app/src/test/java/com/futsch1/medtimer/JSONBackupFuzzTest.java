package com.futsch1.medtimer;

public class JSONBackupFuzzTest {
    //@FuzzTest
    /*void fuzzTestMedicineBackup(FuzzedDataProvider data) {
        JSONMedicineBackup jsonMedicineBackup = new JSONMedicineBackup();
        JSONReminderEventBackup jsonReminderEventBackup = new JSONReminderEventBackup();
        String json = data.consumeRemainingAsString();

        checkBackup(jsonMedicineBackup, json);
        checkBackup(jsonReminderEventBackup, json);
    }

    private <T> void checkBackup(JSONBackup<T> backup, String json) {
        List<T> parsedData = backup.parseBackup(json);
        MedicineRepository medicineRepository = mock(MedicineRepository.class);
        if (parsedData != null) {
            backup.applyBackup(parsedData, medicineRepository);
        }
    }*/
}

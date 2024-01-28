package com.futsch1.medtimer.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MedicineRepository {

    private final MedicineDao medicineDao;
    private final LiveData<List<MedicineWithReminders>> medicinesWithReminders;

    // Note that in order to unit test the WordRepository, you have to remove the Application
    // dependency. This adds complexity and much more code, and this sample is not about testing.
    // See the BasicSample in the android-architecture-components repository at
    // https://github.com/googlesamples
    public MedicineRepository(Application application) {
        MedicineRoomDatabase db = MedicineRoomDatabase.getDatabase(application);
        medicineDao = db.medicineDao();
        medicinesWithReminders = medicineDao.getMedicines();
    }

    public LiveData<List<MedicineWithReminders>> getMedicines() {
        return medicinesWithReminders;
    }


    public LiveData<List<Reminder>> getReminders(int medicineId) {
        return medicineDao.getReminders(medicineId);
    }

    public LiveData<List<ReminderEvent>> getReminderEvents(int limit) {
        if (limit > 0) {
            return medicineDao.getLatestReminderEvents(limit);
        } else return medicineDao.getReminderEvents();
    }

    public void insertMedicine(Medicine medicine) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.insertMedicine(medicine));
    }

    public void updateMedicine(Medicine medicine) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.updateMedicine(medicine));
    }

    public void deleteMedicine(Medicine medicine) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.deleteMedicine(medicine));
    }

    public void insertReminder(Reminder reminder) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.insertReminder(reminder));
    }

    public void updateReminder(Reminder reminder) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.updateReminder(reminder));
    }

    public void deleteReminder(Reminder reminder) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.deleteReminder(reminder));
    }

    public long insertReminderEvent(ReminderEvent reminderEvent) {
        Future<Long> future = MedicineRoomDatabase.databaseWriteExecutor.submit(() -> medicineDao.insertReminderEvent(reminderEvent));
        long rowId = 0;

        try {
            rowId = future.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
        }
        return rowId;
    }

    public ReminderEvent getReminderEvent(int reminderEventId) {
        return medicineDao.getReminderEvent(reminderEventId);
    }

    public void updateReminderEvent(ReminderEvent reminderEvent) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.updateReminderEvent(reminderEvent));
    }
}

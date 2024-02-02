package com.futsch1.medtimer.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MedicineRepository {

    private final MedicineDao medicineDao;

    public MedicineRepository(Application application) {
        MedicineRoomDatabase db = MedicineRoomDatabase.getDatabase(application);
        medicineDao = db.medicineDao();
    }

    public LiveData<List<MedicineWithReminders>> getLiveMedicines() {
        return medicineDao.getLiveMedicines();
    }

    public List<MedicineWithReminders> getMedicines() {
        return medicineDao.getMedicines();
    }

    public Medicine getMedicine(int medicineId) {
        return medicineDao.getMedicine(medicineId);
    }


    public LiveData<List<Reminder>> getLiveReminders(int medicineId) {
        return medicineDao.getReminders(medicineId);
    }

    public Reminder getReminder(int reminderId) {
        return medicineDao.getReminder(reminderId);
    }

    public LiveData<List<ReminderEvent>> getLiveReminderEvents(int limit, long timeStamp) {
        if (limit == 0) {
            return medicineDao.getLiveReminderEvents(timeStamp);
        } else {
            return medicineDao.getReminderEvents(limit);
        }
    }

    public ReminderEvent getLastReminderEvent() {
        return medicineDao.getLastReminderEvent();
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

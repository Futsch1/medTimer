package com.futsch1.medtimer.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

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

    public void insert(Medicine medicine) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> {
            medicineDao.insertMedicine(medicine);
        });
    }

    public void update(Medicine medicine) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> {
            medicineDao.updateMedicine(medicine);
        });
    }

    public void delete(Medicine medicine) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> {
            medicineDao.deleteMedicine(medicine);
        });
    }
}

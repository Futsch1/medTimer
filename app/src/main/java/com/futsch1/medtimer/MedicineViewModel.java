package com.futsch1.medtimer;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.util.List;

public class MedicineViewModel extends AndroidViewModel {

    private final LiveData<List<MedicineWithReminders>> medicines;
    private final MedicineRepository repository;

    public MedicineViewModel(Application application) {
        super(application);
        repository = new MedicineRepository(application);
        medicines = repository.getMedicines();
    }

    LiveData<List<MedicineWithReminders>> getMedicines() {
        return medicines;
    }

    void insertMedicine(Medicine medicine) {
        repository.insertMedicine(medicine);
    }

    void updateMedicine(Medicine medicine) {
        repository.updateMedicine(medicine);
    }

    public void deleteMedicine(Medicine medicine) {
        repository.deleteMedicine(medicine);
    }


    public LiveData<List<Reminder>> getReminders(int medicineId) {
        return repository.getReminders(medicineId);
    }

    void insertReminder(Reminder reminder) {
        repository.insertReminder(reminder);
    }

    public void updateReminder(Reminder reminder) {
        repository.updateReminder(reminder);
    }

    public void deleteReminder(Reminder reminder) {
        repository.deleteReminder(reminder);
    }

    public void insertReminderEvent(ReminderEvent reminderEvent) {
        repository.insertReminderEvent(reminderEvent);
    }

    public LiveData<List<ReminderEvent>> getReminderEvents() {
        return repository.getReminderEvents();
    }
}

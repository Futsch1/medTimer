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

    public final MedicineRepository medicineRepository;
    private final LiveData<List<MedicineWithReminders>> medicines;

    public MedicineViewModel(Application application) {
        super(application);
        medicineRepository = new MedicineRepository(application);
        medicines = medicineRepository.getMedicines();
    }

    LiveData<List<MedicineWithReminders>> getMedicines() {
        return medicines;
    }

    void insertMedicine(Medicine medicine) {
        medicineRepository.insertMedicine(medicine);
    }

    void updateMedicine(Medicine medicine) {
        medicineRepository.updateMedicine(medicine);
    }

    public void deleteMedicine(Medicine medicine) {
        medicineRepository.deleteMedicine(medicine);
    }


    public LiveData<List<Reminder>> getReminders(int medicineId) {
        return medicineRepository.getReminders(medicineId);
    }

    void insertReminder(Reminder reminder) {
        medicineRepository.insertReminder(reminder);
    }

    public void updateReminder(Reminder reminder) {
        medicineRepository.updateReminder(reminder);
    }

    public void deleteReminder(Reminder reminder) {
        medicineRepository.deleteReminder(reminder);
    }

    public LiveData<List<ReminderEvent>> getReminderEvents() {
        return medicineRepository.getReminderEvents(0);
    }

    public LiveData<List<ReminderEvent>> getReminderEvents(int limit) {
        return medicineRepository.getReminderEvents(limit);
    }

    public void updateReminderEvent(ReminderEvent reminderEvent) {
        medicineRepository.updateReminderEvent(reminderEvent);
    }
}

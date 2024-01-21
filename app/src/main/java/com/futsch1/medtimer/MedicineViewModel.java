package com.futsch1.medtimer;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.MedicineWithReminders;

import java.util.List;

public class MedicineViewModel extends AndroidViewModel {

    // Using LiveData and caching what getAlphabetizedWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
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

    void insert(Medicine medicine) {
        repository.insert(medicine);
    }
}

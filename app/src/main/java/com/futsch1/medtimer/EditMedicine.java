package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_MEDICINE;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;

import java.util.List;

public class EditMedicine extends AppCompatActivity {

    MedicineViewModel medicineViewModel;
    EditText editMedicineName;
    int medicineId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_medicine);

        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        editMedicineName = findViewById(R.id.editMedicineName);

        final Observer<List<MedicineWithReminders>> nameObserver = new Observer<List<MedicineWithReminders>>() {
            @Override
            public void onChanged(@Nullable final List<MedicineWithReminders> newList) {
                // Update the UI, in this case, a TextView.
                if (newList != null) {
                    Medicine medicine = newList.get(getIntent().getIntExtra("medicineIndex", 0)).medicine;
                    medicineId = medicine.medicineId;
                    editMedicineName.setText(medicine.name);
                }
            }
        };

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        medicineViewModel.getMedicines().observe(this, nameObserver);

        final ImageButton button = findViewById(R.id.saveMedicineButton);
        button.setOnClickListener(view -> {
            Intent replyIntent = new Intent();
            if (TextUtils.isEmpty(editMedicineName.getText())) {
                setResult(RESULT_CANCELED, replyIntent);
            } else {
                String word = editMedicineName.getText().toString();
                replyIntent.putExtra(EXTRA_MEDICINE, word);
                replyIntent.putExtra(EXTRA_ID, medicineId);
                setResult(RESULT_OK, replyIntent);
            }
            finish();
        });
    }
}
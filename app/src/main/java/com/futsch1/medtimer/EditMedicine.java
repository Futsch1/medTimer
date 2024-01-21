package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REPLY;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.futsch1.medtimer.database.MedicineWithReminders;

import java.util.List;

public class EditMedicine extends AppCompatActivity {

    MedicineViewModel medicineViewModel;
    EditText editMedicineName;

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
                    int index = getIntent().getIntExtra("medicineIndex", 0);
                    editMedicineName.setText(newList.get(index).medicine.name);
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
                replyIntent.putExtra(EXTRA_REPLY, word);
                setResult(RESULT_OK, replyIntent);
            }
            finish();
        });
    }
}
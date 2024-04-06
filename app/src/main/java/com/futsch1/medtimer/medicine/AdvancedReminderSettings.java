package com.futsch1.medtimer.medicine;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_MEDICINE_NAME;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;
import com.google.android.material.button.MaterialButton;

import java.util.Objects;

public class AdvancedReminderSettings extends AppCompatActivity {

    private final HandlerThread backgroundThread;
    private EditText editDaysBetweenReminders;
    private EditText editInstructions;
    private MaterialButton instructionSuggestions;
    private MedicineViewModel medicineViewModel;
    private Reminder reminder;

    public AdvancedReminderSettings() {
        backgroundThread = new HandlerThread("AdvancedReminderSettings");
        backgroundThread.start();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::loadReminder);
    }

    private void loadReminder() {
        int reminderId = getIntent().getIntExtra(EXTRA_ID, 0);
        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        reminder = medicineViewModel.getReminder(reminderId);

        runOnUiThread(this::setupView);
    }

    @SuppressLint("SetTextI18n")
    private void setupView() {

        setContentView(R.layout.activity_advanced_reminder_settings);

        editDaysBetweenReminders = findViewById(R.id.daysBetweenReminders);
        editInstructions = findViewById(R.id.editInstructions);
        instructionSuggestions = findViewById(R.id.instructionSuggestions);

        editDaysBetweenReminders.setText(Integer.toString(reminder.daysBetweenReminders));
        editInstructions.setText(reminder.instructions);

        setupInstructionSuggestions();

        String medicineName = getIntent().getStringExtra(EXTRA_MEDICINE_NAME);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.advanced_settings) + " - " + medicineName);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    private void setupInstructionSuggestions() {
        instructionSuggestions.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.instruction_templates);
            builder.setItems(R.array.instructions_suggestions, (dialog, which) -> {
                if (which > 0) {
                    String[] instructionSuggestionsArray = getResources().getStringArray(R.array.instructions_suggestions);
                    editInstructions.setText(instructionSuggestionsArray[which]);
                } else {
                    editInstructions.setText("");
                }
            });
            builder.show();

        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        reminder.instructions = editInstructions.getText().toString();
        try {
            reminder.daysBetweenReminders = Integer.parseInt(editDaysBetweenReminders.getText().toString());
            if (reminder.daysBetweenReminders <= 0) {
                reminder.daysBetweenReminders = 1;
            }
        } catch (NumberFormatException e) {
            reminder.daysBetweenReminders = 1;
        }

        medicineViewModel.updateReminder(reminder);
    }
}
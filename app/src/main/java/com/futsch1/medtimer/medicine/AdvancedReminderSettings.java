package com.futsch1.medtimer.medicine;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_MEDICINE_NAME;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Objects;

public class AdvancedReminderSettings extends AppCompatActivity {

    private final HandlerThread backgroundThread;
    private String[] daysArray;
    private EditText editDaysBetweenReminders;
    private EditText editInstructions;
    private MaterialButton instructionSuggestions;
    private MedicineViewModel medicineViewModel;
    private Reminder reminder;
    private TextView remindOnDays;

    public AdvancedReminderSettings() {
        backgroundThread = new HandlerThread("AdvancedReminderSettings");
        backgroundThread.start();

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::loadReminder);

        daysArray = getResources().getStringArray(R.array.days);
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
        remindOnDays = findViewById(R.id.remindOnDays);

        editDaysBetweenReminders.setText(Integer.toString(reminder.daysBetweenReminders));
        editInstructions.setText(reminder.instructions);

        setupInstructionSuggestions();
        setupRemindOnDays();

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

    private void setupRemindOnDays() {
        setDaysText();

        remindOnDays.setOnClickListener(v -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.remind_on)
                    .setCancelable(false);

            boolean[] checkedItems = new boolean[daysArray.length];
            for (int i = 0; i < daysArray.length; i++) {
                checkedItems[i] = reminder.days.get(i);
            }
            builder.setMultiChoiceItems(daysArray, checkedItems, (DialogInterface.OnMultiChoiceClickListener) (dialogInterface, i, b) -> checkedItems[i] = b);

            builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                for (int j = 0; j < daysArray.length; j++) {
                    reminder.days.set(j, checkedItems[j]);
                }

                setDaysText();
            });

            builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
            // show dialog
            builder.show();
        });
    }

    private void setDaysText() {
        ArrayList<String> checkedDays = new ArrayList<>();
        for (int j = 0; j < daysArray.length; j++) {
            if (Boolean.TRUE.equals(reminder.days.get(j))) {
                checkedDays.add(daysArray[j]);
            }
        }

        if (checkedDays.size() == daysArray.length) {
            remindOnDays.setText(R.string.every_day);
        } else {
            remindOnDays.setText(String.join(", ", checkedDays));
        }

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
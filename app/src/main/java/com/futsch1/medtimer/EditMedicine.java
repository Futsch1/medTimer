package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_INDEX;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Editable;
import android.text.format.DateFormat;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.adapters.ReminderViewAdapter;
import com.futsch1.medtimer.adapters.ReminderViewHolder;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class EditMedicine extends AppCompatActivity {

    MedicineViewModel medicineViewModel;
    EditText editMedicineName;
    int medicineId;
    HandlerThread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.thread = new HandlerThread("DeleteMedicine");
        this.thread.start();

        setContentView(R.layout.activity_edit_medicine);

        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);
        medicineId = getIntent().getIntExtra(EXTRA_ID, 0);

        RecyclerView recyclerView = findViewById(R.id.reminderList);
        final ReminderViewAdapter adapter = new ReminderViewAdapter(new ReminderViewAdapter.ReminderDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        editMedicineName = findViewById(R.id.editMedicineName);
        final Observer<List<MedicineWithReminders>> nameObserver = newList -> {
            if (newList != null) {
                Medicine medicine = newList.get(getIntent().getIntExtra(EXTRA_INDEX, 0)).medicine;
                editMedicineName.setText(medicine.name);
            }
        };

        // Swipe to delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.LEFT) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(EditMedicine.this);
                    builder.setTitle(R.string.confirm);
                    builder.setMessage(R.string.are_you_sure_delete_reminder);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                        final Handler handler = new Handler(thread.getLooper());
                        handler.post(() -> {
                            Reminder reminder = medicineViewModel.getReminder((int) viewHolder.getItemId());
                            medicineViewModel.deleteReminder(reminder);
                            final Handler mainHandler = new Handler(Looper.getMainLooper());
                            mainHandler.post(() -> adapter.notifyItemRangeChanged(viewHolder.getAdapterPosition(), viewHolder.getAdapterPosition() + 1));
                        });
                    });
                    builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> adapter.notifyItemRangeChanged(viewHolder.getAdapterPosition(), viewHolder.getAdapterPosition() + 1));
                    builder.show();
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);


        ExtendedFloatingActionButton fab = findViewById(R.id.addReminder);
        fab.setOnClickListener(view -> {
            TextInputLayout textInputLayout = new TextInputLayout(this);
            TextInputEditText editText = new TextInputEditText(this);
            editText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            editText.setHint(R.string.amount);
            textInputLayout.addView(editText);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(textInputLayout);
            builder.setTitle(R.string.add_reminder);
            builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                Editable e = editText.getText();
                if (e != null) {
                    String amount = e.toString();
                    Reminder reminder = new Reminder(medicineId);
                    reminder.amount = amount;

                    TimePickerDialog timePickerDialog = new TimePickerDialog(this, (tpView, hourOfDay, minute) -> {
                        reminder.timeInMinutes = hourOfDay * 60 + minute;
                        medicineViewModel.insertReminder(reminder);
                    }, 8, 0, DateFormat.is24HourFormat(view.getContext()));
                    timePickerDialog.show();

                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        medicineViewModel.getMedicines().observe(this, nameObserver);

        medicineViewModel.getReminders(medicineId).observe(this, adapter::submitList);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        String word = editMedicineName.getText().toString();
        medicineViewModel.updateMedicine(new Medicine(word, medicineId));

        RecyclerView recyclerView = findViewById(R.id.reminderList);
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            ReminderViewHolder viewHolder = (ReminderViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(i));

            medicineViewModel.updateReminder(viewHolder.reminder);
        }

        thread.quitSafely();
    }
}
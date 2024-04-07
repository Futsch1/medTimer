package com.futsch1.medtimer.medicine;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_COLOR;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_MEDICINE_NAME;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_USE_COLOR;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.DeleteHelper;
import com.futsch1.medtimer.helpers.DialogHelper;
import com.futsch1.medtimer.helpers.SwipeHelper;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.futsch1.medtimer.helpers.ViewColorHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class EditMedicine extends AppCompatActivity {

    MedicineViewModel medicineViewModel;
    EditText editMedicineName;
    int medicineId;
    HandlerThread thread;
    ReminderViewAdapter adapter;
    private SwipeHelper swipeHelper;
    private MaterialSwitch enableColor;
    private MaterialButton colorButton;
    private int color;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.thread = new HandlerThread("DeleteMedicine");
        this.thread.start();

        setContentView(R.layout.activity_edit_medicine);

        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);
        medicineId = getIntent().getIntExtra(EXTRA_ID, 0);
        String medicineName = getIntent().getStringExtra(EXTRA_MEDICINE_NAME);

        RecyclerView recyclerView = findViewById(R.id.reminderList);
        adapter = new ReminderViewAdapter(new ReminderViewAdapter.ReminderDiff(), EditMedicine.this::deleteItem, medicineName);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        editMedicineName = findViewById(R.id.editMedicineName);
        editMedicineName.setText(medicineName);

        boolean useColor = getIntent().getBooleanExtra(EXTRA_USE_COLOR, false);
        enableColor = findViewById(R.id.enableColor);
        enableColor.setChecked(useColor);
        enableColor.setOnCheckedChangeListener((buttonView, isChecked) -> colorButton.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        setupColorButton(useColor);

        // Swipe to delete
        swipeHelper = new SwipeHelper(Color.RED, android.R.drawable.ic_menu_delete, this) {
            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.LEFT) {
                    EditMedicine.this.deleteItem(EditMedicine.this, viewHolder.getItemId(), viewHolder.getAdapterPosition());
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelper);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        setupAddReminderButton();

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        medicineViewModel.getReminders(medicineId).observe(this, adapter::submitList);

        Objects.requireNonNull(getSupportActionBar()).setTitle(medicineName);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPref.getString("delete_items", "0").equals("0")) {
            swipeHelper.setDefaultSwipeDirs(ItemTouchHelper.LEFT);
        } else {
            swipeHelper.setDefaultSwipeDirs(0);
        }
    }

    private void deleteItem(Context context, long itemId, int adapterPosition) {
        DeleteHelper<ReminderViewHolder> deleteHelper = new DeleteHelper<>(context, thread, adapter);
        deleteHelper.deleteItem(adapterPosition, R.string.are_you_sure_delete_reminder, () -> {
            Reminder reminder = medicineViewModel.getReminder((int) itemId);
            medicineViewModel.deleteReminder(reminder);
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
        });
    }

    private void setupColorButton(boolean useColor) {
        color = getIntent().getIntExtra(EXTRA_COLOR, Color.DKGRAY);
        colorButton = findViewById(R.id.selectColor);
        ViewColorHelper.setButtonBackground(colorButton, color);
        colorButton.setOnClickListener(v -> {
            ColorPickerDialog.Builder builder = new ColorPickerDialog.Builder(this)
                    .setTitle(R.string.color)
                    .setPositiveButton(getString(R.string.confirm),
                            (ColorEnvelopeListener) (envelope, fromUser) -> {
                                color = envelope.getColor();
                                ViewColorHelper.setButtonBackground(colorButton, color);
                                Toast.makeText(this, R.string.change_color_toast, Toast.LENGTH_LONG).show();
                            })
                    .setNegativeButton(getString(R.string.cancel),
                            (dialogInterface, i) -> dialogInterface.dismiss())
                    .attachAlphaSlideBar(false)
                    .setBottomSpace(12);

            builder.show();
            // Workaround to make the brightness slider be setup correctly
            new Handler(getMainLooper()).post(() -> builder.getColorPickerView().setInitialColor(color));
        });
        colorButton.setVisibility(useColor ? View.VISIBLE : View.GONE);
    }

    private void setupAddReminderButton() {
        ExtendedFloatingActionButton fab = findViewById(R.id.addReminder);
        fab.setOnClickListener(view -> DialogHelper.showTextInputDialog(this, R.string.add_reminder, R.string.create_reminder_dosage_hint, this::createReminder));
    }

    private void createReminder(String amount) {
        Reminder reminder = new Reminder(medicineId);
        reminder.amount = amount;
        reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000;
        reminder.cycleStartDay = LocalDate.now().plusDays(1).toEpochDay();

        new TimeHelper.TimePickerWrapper(this).show(0, 0, minutes -> {
            reminder.timeInMinutes = minutes;
            medicineViewModel.insertReminder(reminder);
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

        String word = editMedicineName.getText().toString();
        Medicine medicine = new Medicine(word, medicineId);
        medicine.useColor = enableColor.isChecked();
        medicine.color = color;
        medicineViewModel.updateMedicine(medicine);

        RecyclerView recyclerView = findViewById(R.id.reminderList);
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            ReminderViewHolder viewHolder = (ReminderViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(i));

            medicineViewModel.updateReminder(viewHolder.getReminder());
        }

        thread.quitSafely();
    }
}
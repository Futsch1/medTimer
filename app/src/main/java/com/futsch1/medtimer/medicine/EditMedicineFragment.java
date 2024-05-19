package com.futsch1.medtimer.medicine;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
import com.google.android.material.textfield.TextInputEditText;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.time.Instant;
import java.time.LocalDate;

public class EditMedicineFragment extends Fragment {

    MedicineViewModel medicineViewModel;
    EditText editMedicineName;
    int medicineId;
    HandlerThread thread;
    ReminderViewAdapter adapter;
    private SwipeHelper swipeHelper;
    private MaterialSwitch enableColor;
    private MaterialButton colorButton;
    private MaterialSwitch enableInventory;
    private TextInputEditText editInventory;
    private int color;
    private int inventory;
    private View fragmentEditMedicine;
    private EditMedicineFragmentArgs editMedicineArgs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        fragmentEditMedicine = inflater.inflate(R.layout.fragment_edit_medicine, container, false);

        this.thread = new HandlerThread("DeleteMedicine");
        this.thread.start();

        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        assert getArguments() != null;
        editMedicineArgs = EditMedicineFragmentArgs.fromBundle(getArguments());
        medicineId = editMedicineArgs.getMedicineId();
        String medicineName = editMedicineArgs.getMedicineName();

        RecyclerView recyclerView = fragmentEditMedicine.findViewById(R.id.reminderList);
        adapter = new ReminderViewAdapter(new ReminderViewAdapter.ReminderDiff(), EditMedicineFragment.this::deleteItem, medicineName, requireActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        editMedicineName = fragmentEditMedicine.findViewById(R.id.editMedicineName);
        editMedicineName.setText(medicineName);

        boolean useColor = editMedicineArgs.getUseColor();
        setupEnableColor(useColor);
        setupColorButton(useColor);

        boolean useInventory = editMedicineArgs.getUseInventory();
        setupEnableInventory(useInventory);
        setupInventory(useInventory);

        setupSwiping(recyclerView);
        setupAddReminderButton();

        medicineViewModel.getReminders(medicineId).observe(requireActivity(), adapter::submitList);

        return fragmentEditMedicine;
    }

    @Override
    public void onResume() {
        super.onResume();
        swipeHelper.setup(requireContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        String word = editMedicineName.getText().toString();
        Medicine medicine = new Medicine(word, medicineId);
        medicine.useColor = enableColor.isChecked();
        medicine.color = color;
        medicine.useInventory = enableInventory.isChecked();
        medicine.inventory = inventory;
        medicineViewModel.updateMedicine(medicine);

        RecyclerView recyclerView = fragmentEditMedicine.findViewById(R.id.reminderList);
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            ReminderViewHolder viewHolder = (ReminderViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(i));

            medicineViewModel.updateReminder(viewHolder.getReminder());
        }

        if (thread != null) {
            thread.quitSafely();
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

    private void setupEnableColor(boolean useColor) {
        enableColor = fragmentEditMedicine.findViewById(R.id.enableColor);
        enableColor.setChecked(useColor);
        enableColor.setOnCheckedChangeListener((buttonView, isChecked) -> colorButton.setEnabled(isChecked));
    }

    private void setupColorButton(boolean useColor) {
        color = editMedicineArgs.getColor();
        colorButton = fragmentEditMedicine.findViewById(R.id.selectColor);
        ViewColorHelper.setButtonBackground(colorButton, color);
        colorButton.setOnClickListener(v -> {
            ColorPickerDialog.Builder builder = new ColorPickerDialog.Builder(requireContext())
                    .setTitle(R.string.color)
                    .setPositiveButton(getString(R.string.confirm),
                            (ColorEnvelopeListener) (envelope, fromUser) -> {
                                color = envelope.getColor();
                                ViewColorHelper.setButtonBackground(colorButton, color);
                                Toast.makeText(requireContext(), R.string.change_color_toast, Toast.LENGTH_LONG).show();
                            })
                    .setNegativeButton(getString(R.string.cancel),
                            (dialogInterface, i) -> dialogInterface.dismiss())
                    .attachAlphaSlideBar(false)
                    .setBottomSpace(12);

            builder.show();
            // Workaround to make the brightness slider be setup correctly
            new Handler(requireActivity().getMainLooper()).post(() -> builder.getColorPickerView().setInitialColor(color));
        });
        colorButton.setEnabled(useColor);
    }

    private void setupEnableInventory(boolean useInventory) {
        enableInventory = fragmentEditMedicine.findViewById(R.id.enableInventory);
        enableInventory.setChecked(useInventory);
        enableInventory.setOnCheckedChangeListener((buttonView, isChecked) -> editInventory.setEnabled(isChecked));
    }

    private void setupInventory(boolean useInventory) {
        inventory = editMedicineArgs.getInventory();
        editInventory = fragmentEditMedicine.findViewById(R.id.editInventory);
        editInventory.setText(String.valueOf(inventory));
        editInventory.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    inventory = Integer.parseInt(editInventory.getText().toString());
                } catch (NumberFormatException e) {
                    inventory = 0;
                    editInventory.setText("0");
                }
            }
        });
        editInventory.setEnabled(useInventory);
    }

    private void setupSwiping(RecyclerView recyclerView) {
        swipeHelper = new SwipeHelper(requireContext(), ItemTouchHelper.LEFT, 0xFF8B0000, android.R.drawable.ic_menu_delete, "delete_items") {
            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.LEFT) {
                    EditMedicineFragment.this.deleteItem(requireContext(), viewHolder.getItemId(), viewHolder.getAdapterPosition());
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelper);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void setupAddReminderButton() {
        ExtendedFloatingActionButton fab = fragmentEditMedicine.findViewById(R.id.addReminder);
        fab.setOnClickListener(view -> DialogHelper.showTextInputDialog(requireContext(), R.string.add_reminder, R.string.create_reminder_dosage_hint, this::createReminder));
    }

    private void createReminder(String amount) {
        Reminder reminder = new Reminder(medicineId);
        reminder.amount = amount;
        reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000;
        reminder.cycleStartDay = LocalDate.now().plusDays(1).toEpochDay();

        new TimeHelper.TimePickerWrapper(requireActivity()).show(0, 0, minutes -> {
            reminder.timeInMinutes = minutes;
            medicineViewModel.insertReminder(reminder);
        });
    }
}
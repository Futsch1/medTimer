package com.futsch1.medtimer.medicine;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.DeleteHelper;
import com.futsch1.medtimer.helpers.DialogHelper;
import com.futsch1.medtimer.helpers.MedicineIcons;
import com.futsch1.medtimer.helpers.SwipeHelper;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.futsch1.medtimer.helpers.ViewColorHelper;
import com.futsch1.medtimer.medicine.editMedicine.NotificationImportanceKt;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.maltaisn.icondialog.IconDialog;
import com.maltaisn.icondialog.IconDialogSettings;
import com.maltaisn.icondialog.data.Icon;
import com.maltaisn.icondialog.pack.IconPack;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class EditMedicineFragment extends Fragment implements IconDialog.Callback {

    private static final String ICON_DIALOG_TAG = "icon-dialog";
    final HandlerThread thread;
    MedicineViewModel medicineViewModel;
    EditText editMedicineName;
    int medicineId;
    int iconId;
    ReminderViewAdapter adapter;
    private MaterialSwitch enableColor;
    private MaterialButton colorButton;
    private int color;
    private View fragmentEditMedicine;
    private AutoCompleteTextView notificationImportance;
    private EditMedicineFragmentArgs editMedicineArgs;
    private MaterialButton selectIconButton;

    public EditMedicineFragment() {
        this.thread = new HandlerThread("DeleteMedicine");
        this.thread.start();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        fragmentEditMedicine = inflater.inflate(R.layout.fragment_edit_medicine, container, false);

        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        assert getArguments() != null;
        editMedicineArgs = EditMedicineFragmentArgs.fromBundle(getArguments());
        medicineId = editMedicineArgs.getMedicineId();
        String medicineName = editMedicineArgs.getMedicineName();

        RecyclerView recyclerView = setupMedicineList(medicineName);

        editMedicineName = fragmentEditMedicine.findViewById(R.id.editMedicineName);
        editMedicineName.setText(medicineName);

        setupOpenCalendarButton();

        boolean useColor = editMedicineArgs.getUseColor();
        setupEnableColor(useColor);
        setupColorButton(useColor);

        setupSwiping(recyclerView);
        setupAddReminderButton();

        iconId = editMedicineArgs.getIconId();
        setupSelectIcon();

        medicineViewModel.getLiveReminders(medicineId).observe(requireActivity(), adapter::submitList);

        requireActivity().addMenuProvider(new EditMedicineMenuProvider(medicineId, thread, medicineViewModel, fragmentEditMedicine), getViewLifecycleOwner());

        return fragmentEditMedicine;
    }

    private @NonNull RecyclerView setupMedicineList(String medicineName) {
        RecyclerView recyclerView = fragmentEditMedicine.findViewById(R.id.reminderList);
        adapter = new ReminderViewAdapter(new ReminderViewAdapter.ReminderDiff(), medicineName, requireActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        return recyclerView;
    }

    private void setupOpenCalendarButton() {
        MaterialButton openCalendar = fragmentEditMedicine.findViewById(R.id.openCalendar);
        openCalendar.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(openCalendar);
            EditMedicineFragmentDirections.ActionEditMedicineFragmentToMedicineCalendarFragment action =
                    EditMedicineFragmentDirections.actionEditMedicineFragmentToMedicineCalendarFragment(
                            medicineId,
                            30,
                            30
                    );
            navController.navigate(action);
        });
    }

    private void setupEnableColor(boolean useColor) {
        enableColor = fragmentEditMedicine.findViewById(R.id.enableColor);
        enableColor.setChecked(useColor);
        enableColor.setOnCheckedChangeListener((buttonView, isChecked) -> colorButton.setVisibility(isChecked ? View.VISIBLE : View.GONE));
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
        colorButton.setVisibility(useColor ? View.VISIBLE : View.GONE);
    }

    private void setupSwiping(RecyclerView recyclerView) {
        SwipeHelper.createLeftSwipeTouchHelper(requireContext(), viewHolder -> deleteItem(requireContext(), viewHolder.getItemId(), viewHolder.getBindingAdapterPosition()))
                .attachToRecyclerView(recyclerView);
    }

    private void setupAddReminderButton() {
        ExtendedFloatingActionButton fab = fragmentEditMedicine.findViewById(R.id.addReminder);
        fab.setOnClickListener(view -> DialogHelper.showTextInputDialog(requireContext(), R.string.add_reminder, R.string.create_reminder_dosage_hint, this::createReminder));
    }

    private void setupSelectIcon() {
        selectIconButton = fragmentEditMedicine.findViewById(R.id.selectIcon);
        selectIconButton.setIcon(MedicineIcons.getIconDrawable(iconId));

        FragmentManager fragmentManager = getChildFragmentManager();
        IconDialog dialog = (IconDialog) fragmentManager.findFragmentByTag(ICON_DIALOG_TAG);
        IconDialogSettings.Builder builder = new IconDialogSettings.Builder();
        builder.setShowClearBtn(true);
        builder.setShowSelectBtn(false);
        IconDialog iconDialog = dialog != null ? dialog
                : IconDialog.newInstance(builder.build());

        selectIconButton.setOnClickListener(v -> {
                    iconDialog.setSelectedIconIds(List.of(iconId));
                    iconDialog.show(fragmentManager, ICON_DIALOG_TAG);
                }
        );
    }

    private void deleteItem(Context context, long itemId, int adapterPosition) {
        DeleteHelper deleteHelper = new DeleteHelper(context);
        deleteHelper.deleteItem(R.string.are_you_sure_delete_reminder, () -> {
            final Handler threadHandler = new Handler(thread.getLooper());
            threadHandler.post(() -> {
                Reminder reminder = medicineViewModel.getReminder((int) itemId);
                medicineViewModel.deleteReminder(reminder);
                final Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
            });
        }, () -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
    }

    private void createReminder(String amount) {
        Reminder reminder = new Reminder(medicineId);
        reminder.amount = amount;
        reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000;
        reminder.cycleStartDay = LocalDate.now().plusDays(1).toEpochDay();
        reminder.instructions = "";

        new TimeHelper.TimePickerWrapper(requireActivity()).show(0, 0, minutes -> {
            reminder.timeInMinutes = minutes;
            medicineViewModel.insertReminder(reminder);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setupNotificationImportance();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (editMedicineName != null && enableColor != null) {
            String word = editMedicineName.getText().toString();
            Medicine medicine = new Medicine(word, medicineId);
            medicine.useColor = enableColor.isChecked();
            medicine.color = color;
            medicine.notificationImportance = NotificationImportanceKt.importanceStringToValue(notificationImportance.getText().toString(), this.getResources());
            medicine.iconId = iconId;
            medicineViewModel.updateMedicine(medicine);
        }

        updateReminders();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread.quitSafely();
    }

    private void updateReminders() {
        if (fragmentEditMedicine != null) {
            RecyclerView recyclerView = fragmentEditMedicine.findViewById(R.id.reminderList);
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                ReminderViewHolder viewHolder = (ReminderViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(i));

                medicineViewModel.updateReminder(viewHolder.getReminder());
            }
        }
    }

    private void setupNotificationImportance() {
        notificationImportance = fragmentEditMedicine.findViewById(R.id.notificationImportance);

        String[] importanceTexts = this.getResources().getStringArray(R.array.notification_importance);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, importanceTexts);
        notificationImportance.setAdapter(arrayAdapter);
        notificationImportance.setText(NotificationImportanceKt.importanceValueToString(editMedicineArgs.getNotificationImportance(), this.getResources()), false);
    }


    @Nullable
    @Override
    public IconPack getIconDialogIconPack() {
        return MedicineIcons.getIconPack();
    }

    @Override
    public void onIconDialogCancelled() {
        // Intentionally empty
    }

    @Override
    public void onIconDialogIconsSelected(@NonNull IconDialog iconDialog, @NonNull List<Icon> list) {
        iconId = list.get(0).getId();
        selectIconButton.setIcon(MedicineIcons.getIconDrawable(iconId));
    }
}
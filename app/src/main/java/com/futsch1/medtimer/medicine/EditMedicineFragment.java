package com.futsch1.medtimer.medicine;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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
import com.futsch1.medtimer.helpers.MedicineIcons;
import com.futsch1.medtimer.helpers.SwipeHelper;
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

import java.util.List;

import kotlin.Unit;

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
        editMedicineName = fragmentEditMedicine.findViewById(R.id.editMedicineName);

        postponeEnterTransition();

        assert getArguments() != null;
        medicineId = EditMedicineFragmentArgs.fromBundle(getArguments()).getMedicineId();
        medicineViewModel.getLiveMedicine(medicineId).observe(getViewLifecycleOwner(), this::setupViews);

        setupOpenCalendarButton();
        setupAddReminderButton();

        requireActivity().addMenuProvider(new EditMedicineMenuProvider(medicineId, thread, medicineViewModel, fragmentEditMedicine), getViewLifecycleOwner());

        return fragmentEditMedicine;
    }

    private void setupViews(Medicine medicine) {
        color = medicine.color;
        iconId = medicine.iconId;

        setupEnableColor(medicine.useColor);
        setupColorButton(medicine.useColor);
        editMedicineName.setText(medicine.name);
        RecyclerView recyclerView = setupMedicineList();
        setupSwiping(recyclerView);
        setupSelectIcon();
        setupNotificationImportance(medicine.notificationImportance);

        medicineViewModel.getLiveReminders(medicineId).observe(getViewLifecycleOwner(), l -> {
                    this.sortAndSubmitList(l);
                    startPostponedEnterTransition();
                }
        );
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

    private void setupAddReminderButton() {
        ExtendedFloatingActionButton fab = fragmentEditMedicine.findViewById(R.id.addReminder);
        fab.setOnClickListener(view -> new NewReminder(requireContext(), requireActivity(), medicineId, medicineViewModel));
    }

    private void setupEnableColor(boolean useColor) {
        enableColor = fragmentEditMedicine.findViewById(R.id.enableColor);
        enableColor.setChecked(useColor);
        enableColor.setOnCheckedChangeListener((buttonView, isChecked) -> colorButton.setVisibility(isChecked ? View.VISIBLE : View.GONE));
    }

    private void setupColorButton(boolean useColor) {
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

    private @NonNull RecyclerView setupMedicineList() {
        RecyclerView recyclerView = fragmentEditMedicine.findViewById(R.id.reminderList);
        adapter = new ReminderViewAdapter(new ReminderViewAdapter.ReminderDiff(), requireActivity(), thread);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        return recyclerView;
    }

    private void setupSwiping(RecyclerView recyclerView) {
        SwipeHelper.createLeftSwipeTouchHelper(requireContext(), viewHolder -> deleteItem(viewHolder.getItemId(), viewHolder.getBindingAdapterPosition()))
                .attachToRecyclerView(recyclerView);
    }

    private void setupSelectIcon() {
        selectIconButton = fragmentEditMedicine.findViewById(R.id.selectIcon);
        selectIconButton.setIcon(new MedicineIcons(requireContext()).getIconDrawable(iconId));

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

    private void setupNotificationImportance(int notificationImportanceValue) {
        notificationImportance = fragmentEditMedicine.findViewById(R.id.notificationImportance);

        String[] importanceTexts = this.getResources().getStringArray(R.array.notification_importance);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, importanceTexts);
        notificationImportance.setAdapter(arrayAdapter);
        notificationImportance.setText(NotificationImportanceKt.importanceValueToString(notificationImportanceValue, this.getResources()), false);
    }

    private void sortAndSubmitList(List<Reminder> reminders) {
        adapter.submitList(new LinkedReminderAlgorithms().sortRemindersList(reminders));
    }

    private void deleteItem(long itemId, int adapterPosition) {
        final Handler threadHandler = new Handler(thread.getLooper());
        threadHandler.post(() -> {
            Reminder reminder = medicineViewModel.getReminder((int) itemId);
            new LinkedReminderHandling(reminder, medicineViewModel).deleteReminder(fragmentEditMedicine, thread, () -> {
                adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1);
                return Unit.INSTANCE;
            });
        });
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

    @Nullable
    @Override
    public IconPack getIconDialogIconPack() {
        return new MedicineIcons(requireContext()).getIconPack();
    }

    @Override
    public void onIconDialogCancelled() {
        // Intentionally empty
    }

    @Override
    public void onIconDialogIconsSelected(@NonNull IconDialog iconDialog, @NonNull List<Icon> list) {
        iconId = list.get(0).getId();
        selectIconButton.setIcon(new MedicineIcons(requireContext()).getIconDrawable(iconId));
    }
}
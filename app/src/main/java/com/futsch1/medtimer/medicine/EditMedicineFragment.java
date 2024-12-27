package com.futsch1.medtimer.medicine;

import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.DatabaseEntityEditFragment;
import com.futsch1.medtimer.helpers.MedicineEntityInterface;
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
import java.util.Objects;

import kotlin.Unit;

public class EditMedicineFragment extends DatabaseEntityEditFragment<Medicine>
        implements IconDialog.Callback {

    private static final String ICON_DIALOG_TAG = "icon-dialog";
    int iconId;
    ReminderViewAdapter adapter;
    private MaterialSwitch enableColor;
    private MaterialButton colorButton;
    private int color;
    private AutoCompleteTextView notificationImportance;
    private MaterialButton selectIconButton;

    public EditMedicineFragment() {
        super(new MedicineEntityInterface(), R.layout.fragment_edit_medicine, EditMedicineFragment.class.getName());
    }

    @Override
    protected void setupMenu(@NonNull View fragmentView) {
        requireActivity().addMenuProvider(new EditMedicineMenuProvider(getEntityId(), this.getThread(), this.getMedicineViewModel(), fragmentView), getViewLifecycleOwner());
    }

    @Override
    public boolean onEntityLoaded(Medicine entity, @NonNull View fragmentView) {
        color = entity.color;
        iconId = entity.iconId;

        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(entity.name);

        setupEnableColor(fragmentView, entity.useColor);
        setupColorButton(fragmentView, entity.useColor);
        ((EditText) fragmentView.findViewById(R.id.editMedicineName)).setText(entity.name);
        RecyclerView recyclerView = setupMedicineList(fragmentView);
        setupSwiping(recyclerView);
        setupSelectIcon(fragmentView);
        setupNotificationImportance(fragmentView, entity.notificationImportance);
        setupStockButton(fragmentView);

        setupOpenCalendarButton(fragmentView);
        setupAddReminderButton(fragmentView);

        this.getMedicineViewModel().getLiveReminders(this.getEntityId()).observe(getViewLifecycleOwner(), l -> {
                    this.sortAndSubmitList(l);
                    this.setFragmentReady();
                }
        );
        return false;
    }

    private void setupEnableColor(View fragmentView, boolean useColor) {
        enableColor = fragmentView.findViewById(R.id.enableColor);
        enableColor.setChecked(useColor);
        enableColor.setOnCheckedChangeListener((buttonView, isChecked) -> colorButton.setVisibility(isChecked ? View.VISIBLE : View.GONE));
    }

    private void setupColorButton(View fragmentView, boolean useColor) {
        colorButton = fragmentView.findViewById(R.id.selectColor);
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

    private @NonNull RecyclerView setupMedicineList(View fragmentView) {
        RecyclerView recyclerView = fragmentView.findViewById(R.id.reminderList);
        adapter = new ReminderViewAdapter(new ReminderViewAdapter.ReminderDiff(), requireActivity(), getThread());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        return recyclerView;
    }

    private void setupSwiping(RecyclerView recyclerView) {
        SwipeHelper.createLeftSwipeTouchHelper(requireContext(), viewHolder -> deleteItem(viewHolder.getItemId(), viewHolder.getBindingAdapterPosition()))
                .attachToRecyclerView(recyclerView);
    }

    private void setupSelectIcon(View fragmentView) {
        selectIconButton = fragmentView.findViewById(R.id.selectIcon);
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

    private void setupNotificationImportance(View fragmentView, int notificationImportanceValue) {
        notificationImportance = fragmentView.findViewById(R.id.notificationImportance);

        String[] importanceTexts = this.getResources().getStringArray(R.array.notification_importance);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, importanceTexts);
        notificationImportance.setAdapter(arrayAdapter);
        notificationImportance.setText(NotificationImportanceKt.importanceValueToString(notificationImportanceValue, this.getResources()), false);
    }

    private void setupStockButton(View fragmentView) {
        MaterialButton openStockTracking = fragmentView.findViewById(R.id.openStockTracking);
        openStockTracking.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(openStockTracking);
            EditMedicineFragmentDirections.ActionEditMedicineFragmentToMedicineStockFragment action =
                    EditMedicineFragmentDirections.actionEditMedicineFragmentToMedicineStockFragment(getEntityId());
            navController.navigate(action);
        });
    }

    private void setupOpenCalendarButton(View fragmentView) {
        MaterialButton openCalendar = fragmentView.findViewById(R.id.openCalendar);
        openCalendar.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(openCalendar);
            EditMedicineFragmentDirections.ActionEditMedicineFragmentToMedicineCalendarFragment action =
                    EditMedicineFragmentDirections.actionEditMedicineFragmentToMedicineCalendarFragment(
                            getEntityId(),
                            30,
                            30
                    );
            navController.navigate(action);
        });
    }

    private void setupAddReminderButton(View fragmentView) {
        ExtendedFloatingActionButton fab = fragmentView.findViewById(R.id.addReminder);
        fab.setOnClickListener(view -> new NewReminder(requireContext(), requireActivity(), getEntityId(), this.getMedicineViewModel()));
    }

    private void sortAndSubmitList(List<Reminder> reminders) {
        adapter.submitList(new LinkedReminderAlgorithms().sortRemindersList(reminders));
    }

    private void deleteItem(long itemId, int adapterPosition) {
        final Handler threadHandler = new Handler(getThread().getLooper());
        threadHandler.post(() -> {
            Reminder reminder = this.getMedicineViewModel().getReminder((int) itemId);
            new LinkedReminderHandling(reminder, this.getMedicineViewModel()).deleteReminder(requireContext(), getThread(), () -> {
                adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1);
                return Unit.INSTANCE;
            });
        });
    }

    @Override
    public void fillEntityData(Medicine entity, @NonNull View fragmentView) {
        entity.name = ((EditText) fragmentView.findViewById(R.id.editMedicineName)).getText().toString();
        entity.useColor = enableColor.isChecked();
        entity.color = color;
        entity.notificationImportance = NotificationImportanceKt.importanceStringToValue(notificationImportance.getText().toString(), this.getResources());
        entity.iconId = iconId;

        updateReminders(fragmentView);
    }

    private void updateReminders(View fragmentView) {
        if (fragmentView != null) {
            RecyclerView recyclerView = fragmentView.findViewById(R.id.reminderList);
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                ReminderViewHolder viewHolder = (ReminderViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(i));

                this.getMedicineViewModel().updateReminder(viewHolder.getReminder());
            }
        }
    }

    @Override
    public int getEntityId() {
        return EditMedicineFragmentArgs.fromBundle(requireArguments()).getMedicineId();
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
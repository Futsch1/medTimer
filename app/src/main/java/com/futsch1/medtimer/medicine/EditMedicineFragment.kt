package com.futsch1.medtimer.medicine

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.EntityEditOptionsMenu
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.SimpleIdlingResource
import com.futsch1.medtimer.helpers.SwipeHelper
import com.futsch1.medtimer.helpers.ViewColorHelper
import com.futsch1.medtimer.helpers.safeStartActivity
import com.futsch1.medtimer.medicine.dialogs.ColorPickerDialog
import com.futsch1.medtimer.medicine.dialogs.NewReminderTypeDialog
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.maltaisn.icondialog.IconDialog
import com.maltaisn.icondialog.IconDialogSettings
import com.maltaisn.icondialog.data.Icon
import com.maltaisn.icondialog.pack.IconPack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class EditMedicineFragment : Fragment(), IconDialog.Callback {
    companion object {
        private const val ICON_DIALOG_TAG = "icon-dialog"
    }

    @Inject
    @Dispatcher(MedTimerDispatchers.IO)
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @Dispatcher(MedTimerDispatchers.Main)
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var linkedReminderHandlingFactory: LinkedReminderHandling.Factory

    @Inject
    lateinit var reminderViewAdapterFactory: ReminderViewAdapter.Factory

    @Inject
    lateinit var newReminderTypeDialogFactory: NewReminderTypeDialog.Factory

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var editMedicineSubmenusFactory: EditMedicineSubmenus.Factory

    @Inject
    lateinit var editMedicineMenuProviderFactory: EditMedicineMenuProvider.Factory

    @Inject
    lateinit var medicineIcons: MedicineIcons

    @Inject
    lateinit var linkedReminderAlgorithms: LinkedReminderAlgorithms

    @Inject
    lateinit var medicineRepository: MedicineRepository

    @Inject
    lateinit var reminderRepository: ReminderRepository

    private lateinit var medicine: Medicine
    private lateinit var fragmentView: View
    private var fragmentReady = false
    private lateinit var optionsMenu: EntityEditOptionsMenu

    private val idlingResource = SimpleIdlingResource(EditMedicineFragment::class.java.name)

    var iconId: Int = 0
    private lateinit var adapter: ReminderViewAdapter
    private lateinit var enableColor: MaterialSwitch
    private lateinit var colorButton: MaterialButton
    private var color = 0
    private lateinit var notificationImportance: Spinner
    private lateinit var selectIconButton: MaterialButton
    var notes: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        idlingResource.setBusy()
        fragmentView = inflater.inflate(R.layout.fragment_edit_medicine, container, false)

        // Do not enter fragment just yet, first fetch entity from database and setup UI
        postponeEnterTransition()

        val navController = findNavController()

        setupSelectIcon()
        enableColor = fragmentView.findViewById(R.id.enableColor)
        colorButton = fragmentView.findViewById(R.id.selectColor)
        notificationImportance = fragmentView.findViewById(R.id.notificationImportance)

        val recyclerView = setupMedicineList()
        setupSwiping(recyclerView)

        lifecycleScope.launch(ioDispatcher) {
            val medicine = medicineRepository.get(getMedicineId()) ?: return@launch

            this@EditMedicineFragment.medicine = medicine

            setupMenu(navController, medicine)

            withContext(mainDispatcher) {
                if (::optionsMenu.isInitialized) {
                    requireActivity().addMenuProvider(optionsMenu, viewLifecycleOwner)
                }

                // Signal that entity was loaded
                onMedicineLoaded(medicine)
            }
        }

        return fragmentView
    }

    private fun setFragmentReady() {
        // Only now allow getting data from fragment UI when it is closed
        fragmentReady = true
        idlingResource.setIdle()
        // Now enter fragment
        startPostponedEnterTransition()
    }

    private fun setupMenu(navController: NavController, medicine: Medicine) {
        optionsMenu = editMedicineMenuProviderFactory.create(medicine, this, navController)
    }

    override fun onDestroy() {
        super.onDestroy()
        idlingResource.destroy()
        if (::optionsMenu.isInitialized) {
            optionsMenu.onDestroy()
        }
    }

    override fun onStop() {
        super.onStop()
        if (fragmentReady) {
            applicationScope.launch {
                val newMedicine = buildMedicine()
                if (medicine != newMedicine) {
                    medicineRepository.update(newMedicine)
                }
            }
        }
    }

    private fun onMedicineLoaded(medicine: Medicine) {

        color = medicine.color
        iconId = medicine.iconId
        notes = medicine.notes

        (requireActivity() as AppCompatActivity).supportActionBar?.title = medicine.name
        fragmentView.findViewById<EditText>(R.id.editMedicineName).apply {
            setText(medicine.name)
        }

        val subMenus = editMedicineSubmenusFactory.create(this, medicine)

        selectIconButton.setIcon(medicineIcons.getIconDrawable(iconId))
        setupEnableColor(medicine.useColor)
        setupColorButton(medicine.useColor)

        setupNotificationImportance()
        setupNotesButton(subMenus)
        setupOpenCalendarButton(subMenus)
        setupStockButton(subMenus)
        setupTagsButton(subMenus)

        setupAddReminderButton()

        adapter.setMedicine(medicine)

        viewLifecycleOwner.lifecycleScope.launch {
            reminderRepository.getAllFlow(getMedicineId()).collect {
                sortAndSubmitList(it)
                setFragmentReady()
            }
        }
    }

    private fun setupSelectIcon() {
        selectIconButton = fragmentView.findViewById(R.id.selectIcon)

        selectIconButton.setOnClickListener {
            val fragmentManager = getChildFragmentManager()
            val dialog = fragmentManager.findFragmentByTag(ICON_DIALOG_TAG) as IconDialog?
            val builder = IconDialogSettings.Builder()
            builder.showClearBtn = true
            builder.showSelectBtn = false

            val iconDialog = dialog ?: IconDialog.newInstance(builder.build())
            iconDialog.selectedIconIds = listOf(iconId)
            iconDialog.show(fragmentManager, ICON_DIALOG_TAG)
        }
    }

    private fun setupEnableColor(useColor: Boolean) {
        enableColor.setChecked(useColor)
        enableColor.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            colorButton.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupColorButton(useColor: Boolean) {
        ViewColorHelper.setButtonBackground(colorButton, color)
        colorButton.setOnClickListener {
            ColorPickerDialog(requireContext(), requireActivity(), color) { newColor ->
                color = newColor
                ViewColorHelper.setButtonBackground(colorButton, color)
                Toast.makeText(requireContext(), R.string.change_color_toast, Toast.LENGTH_LONG).show()
            }
        }
        colorButton.visibility = if (useColor) View.VISIBLE else View.GONE
    }

    private fun setupNotificationImportance() {
        val notificationImportance = fragmentView.findViewById<Spinner>(R.id.notificationImportance)
        val importanceTexts = this.resources.getStringArray(R.array.notification_importance)
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, importanceTexts)
        val index = if (medicine.notificationImportance == Medicine.NotificationImportance.DEFAULT) {
            0
        } else {
            if (medicine.showNotificationAsAlarm) 2 else 1
        }

        notificationImportance.setAdapter(arrayAdapter)

        notificationImportance.setSelection(index)
        notificationImportance.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != 2) {
                    return
                }
                showEnablePermissionsDialog(notificationManager)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Intentionally empty
            }
        }
    }

    private fun setupNotesButton(subMenus: EditMedicineSubmenus) {
        val openNotes = fragmentView.findViewById<MaterialButton>(R.id.openNotes)
        openNotes.setOnClickListener {
            subMenus.open(EditMedicineSubmenus.Submenu.NOTES, findNavController(openNotes))
        }
    }

    private fun setupOpenCalendarButton(subMenus: EditMedicineSubmenus) {
        val openCalendar = fragmentView.findViewById<MaterialButton>(R.id.openCalendar)
        openCalendar.setOnClickListener {
            subMenus.open(EditMedicineSubmenus.Submenu.CALENDAR, findNavController(openCalendar))
        }
    }

    private fun setupStockButton(subMenus: EditMedicineSubmenus) {
        val openStockTracking = fragmentView.findViewById<MaterialButton>(R.id.openStockTracking)
        openStockTracking.setOnClickListener {
            subMenus.open(EditMedicineSubmenus.Submenu.STOCK_TRACKING, findNavController(openStockTracking))
        }
    }

    private fun setupTagsButton(subMenus: EditMedicineSubmenus) {
        val openTags = fragmentView.findViewById<MaterialButton>(R.id.openTags)
        openTags.setOnClickListener {
            subMenus.open(EditMedicineSubmenus.Submenu.TAGS, findNavController(openTags))
        }
    }

    private fun setupMedicineList(): RecyclerView {
        val recyclerView = fragmentView.findViewById<RecyclerView>(R.id.reminderList)
        adapter = reminderViewAdapterFactory.create(requireActivity())
        recyclerView.setAdapter(adapter)
        recyclerView.setLayoutManager(LinearLayoutManager(recyclerView.context))
        return recyclerView
    }

    private fun setupSwiping(recyclerView: RecyclerView) {
        SwipeHelper.createSwipeHelper(
            requireContext(),
            { viewHolder -> deleteItem(viewHolder.itemId, viewHolder.getBindingAdapterPosition()) },
            null
        )
            .attachToRecyclerView(recyclerView)
    }

    private fun setupAddReminderButton() {
        val fab = fragmentView.findViewById<ExtendedFloatingActionButton>(R.id.addReminder)
        fab.setOnClickListener { newReminderTypeDialogFactory.create(requireActivity(), medicine) }
    }

    private fun sortAndSubmitList(reminders: List<Reminder>) {
        adapter.submitList(linkedReminderAlgorithms.sortRemindersList(reminders))
    }

    private fun deleteItem(itemId: Long, adapterPosition: Int) {
        lifecycleScope.launch(ioDispatcher) {
            val reminder = reminderRepository.get(itemId.toInt())
            if (reminder != null) {
                withContext(mainDispatcher) {
                    linkedReminderHandlingFactory.create(reminder, lifecycleScope).deleteReminder(requireContext(), { }, {
                        adapter.notifyItemChanged(adapterPosition)
                    })
                }
            }
        }
    }

    private suspend fun buildMedicine(): Medicine {
        updateReminders()

        return medicine.copy(
            name = fragmentView.findViewById<EditText>(R.id.editMedicineName).getText().toString().trim(),
            useColor = enableColor.isChecked,
            color = color,
            iconId = iconId,
            notes = notes,
            notificationImportance = if (notificationImportance.selectedItemPosition == 0) Medicine.NotificationImportance.DEFAULT else Medicine.NotificationImportance.HIGH,
            showNotificationAsAlarm = notificationImportance.selectedItemPosition == 2
        )
    }

    private suspend fun updateReminders() {
        val recyclerView = fragmentView.findViewById<RecyclerView>(R.id.reminderList)
        for (i in 0..<recyclerView.size) {
            val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as ReminderViewHolder

            this.reminderRepository.update(viewHolder.getUpdatedReminder())
        }
    }

    private fun getMedicineId(): Int {
        return EditMedicineFragmentArgs.fromBundle(requireArguments()).medicineId
    }

    override val iconDialogIconPack: IconPack
        get() = medicineIcons.getIconPack()

    override fun onIconDialogCancelled() {
        // Intentionally empty to suppress default behavior
    }

    override fun onIconDialogIconsSelected(dialog: IconDialog, icons: List<Icon>) {
        iconId = icons[0].id
        selectIconButton.setIcon(medicineIcons.getIconDrawable(iconId))
    }

    fun showEnablePermissionsDialog(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || notificationManager.canUseFullScreenIntent()) {
            return
        }

        val context = requireContext()
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage(R.string.enable_notification_alarm_dialog)
            .setPositiveButton(R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = "package:${context.packageName}".toUri()
                }
                safeStartActivity(context, intent)
            }

            .setNegativeButton(R.string.cancel) { _, _ ->
                // Intentionally empty
            }.create()

        dialog.show()
    }
}
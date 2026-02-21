package com.futsch1.medtimer.medicine

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.DatabaseEntityEditFragment
import com.futsch1.medtimer.helpers.FullMedicineEntityInterface
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.SwipeHelper
import com.futsch1.medtimer.helpers.ViewColorHelper
import com.futsch1.medtimer.medicine.dialogs.ColorPickerDialog
import com.futsch1.medtimer.medicine.dialogs.NewReminderTypeDialog
import com.futsch1.medtimer.medicine.editMedicine.importanceIndexToMedicine
import com.futsch1.medtimer.medicine.editMedicine.importanceValueToIndex
import com.futsch1.medtimer.medicine.editMedicine.showEnablePermissionsDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.maltaisn.icondialog.IconDialog
import com.maltaisn.icondialog.IconDialogSettings
import com.maltaisn.icondialog.data.Icon
import com.maltaisn.icondialog.pack.IconPack
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditMedicineFragment :
    DatabaseEntityEditFragment<FullMedicine>(FullMedicineEntityInterface(), R.layout.fragment_edit_medicine, EditMedicineFragment::class.java.getName()),
    IconDialog.Callback {
    var iconId: Int = 0
    var adapter: ReminderViewAdapter? = null
    private var enableColor: MaterialSwitch? = null
    private var colorButton: MaterialButton? = null
    private var color = 0
    private var notificationImportance: Spinner? = null
    private var selectIconButton: MaterialButton? = null
    private var notes: String? = null

    override fun setupMenu(navController: NavController, entity: FullMedicine) {
        optionsMenu = EditMedicineMenuProvider(entity.medicine, this, this.medicineViewModel, navController)
    }

    override fun onEntityLoaded(entity: FullMedicine, fragmentView: View): Boolean {
        val medicine = entity.medicine

        color = medicine.color
        iconId = medicine.iconId
        notes = medicine.notes

        (requireActivity() as AppCompatActivity).supportActionBar?.title = medicine.name
        (fragmentView.findViewById<View?>(R.id.editMedicineName) as EditText).setText(medicine.name)

        val subMenus = EditMedicineSubmenus(this, medicine, this.medicineViewModel.medicineRepository)

        setupSelectIcon(fragmentView)
        setupEnableColor(fragmentView, medicine.useColor)
        setupColorButton(fragmentView, medicine.useColor)

        setupNotificationImportance(fragmentView, medicine)
        setupNotesButton(fragmentView, subMenus)
        setupOpenCalendarButton(fragmentView, subMenus)
        setupStockButton(fragmentView, subMenus)
        setupTagsButton(fragmentView, subMenus)

        val recyclerView = setupMedicineList(fragmentView)
        setupSwiping(recyclerView)

        setupAddReminderButton(fragmentView, entity)

        adapter!!.setMedicine(entity)

        this.medicineViewModel.medicineRepository.getLiveReminders(this.getEntityId()).observe(getViewLifecycleOwner(), Observer { l: List<Reminder> ->
            this.sortAndSubmitList(l)
            this.setFragmentReady()
        }
        )
        return false
    }

    private fun setupSelectIcon(fragmentView: View) {
        selectIconButton = fragmentView.findViewById(R.id.selectIcon)
        selectIconButton!!.setIcon(MedicineIcons(requireContext()).getIconDrawable(iconId))

        selectIconButton!!.setOnClickListener { _: View? ->
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

    private fun setupEnableColor(fragmentView: View, useColor: Boolean) {
        enableColor = fragmentView.findViewById(R.id.enableColor)
        enableColor!!.setChecked(useColor)
        enableColor!!.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            colorButton!!.visibility =
                if (isChecked) View.VISIBLE else View.GONE

        }
    }

    private fun setupColorButton(fragmentView: View, useColor: Boolean) {
        colorButton = fragmentView.findViewById(R.id.selectColor)
        ViewColorHelper.setButtonBackground(colorButton!!, color)
        colorButton!!.setOnClickListener { _: View? ->
            ColorPickerDialog(requireContext(), requireActivity(), color) { newColor: Int? ->
                color = newColor!!
                ViewColorHelper.setButtonBackground(colorButton!!, color)
                Toast.makeText(requireContext(), R.string.change_color_toast, Toast.LENGTH_LONG).show()
            }
        }
        colorButton!!.visibility = if (useColor) View.VISIBLE else View.GONE
    }

    private fun setupNotificationImportance(fragmentView: View, medicine: Medicine) {
        notificationImportance = fragmentView.findViewById(R.id.notificationImportance)

        val importanceTexts = this.resources.getStringArray(R.array.notification_importance)
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, importanceTexts)
        notificationImportance!!.setAdapter(arrayAdapter)
        notificationImportance!!.setSelection(importanceValueToIndex(medicine))
        notificationImportance!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 2) {
                    showEnablePermissionsDialog(requireContext())
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Intentionally empty
            }
        }
    }

    private fun setupNotesButton(fragmentView: View, subMenus: EditMedicineSubmenus) {
        val openNotes = fragmentView.findViewById<MaterialButton>(R.id.openNotes)
        openNotes.setOnClickListener { _: View? ->
            subMenus.open(EditMedicineSubmenus.Submenu.NOTES, findNavController(openNotes))
        }
    }

    private fun setupOpenCalendarButton(fragmentView: View, subMenus: EditMedicineSubmenus) {
        val openCalendar = fragmentView.findViewById<MaterialButton>(R.id.openCalendar)
        openCalendar.setOnClickListener { _: View? ->
            subMenus.open(EditMedicineSubmenus.Submenu.CALENDAR, findNavController(openCalendar))
        }
    }

    private fun setupStockButton(fragmentView: View, subMenus: EditMedicineSubmenus) {
        val openStockTracking = fragmentView.findViewById<MaterialButton>(R.id.openStockTracking)
        openStockTracking.setOnClickListener { _: View? ->
            subMenus.open(EditMedicineSubmenus.Submenu.STOCK_TRACKING, findNavController(openStockTracking))
        }
    }

    private fun setupTagsButton(fragmentView: View, subMenus: EditMedicineSubmenus) {
        val openTags = fragmentView.findViewById<MaterialButton>(R.id.openTags)
        openTags.setOnClickListener { _: View? ->
            subMenus.open(EditMedicineSubmenus.Submenu.TAGS, findNavController(openTags))
        }
    }

    private fun setupMedicineList(fragmentView: View): RecyclerView {
        val recyclerView = fragmentView.findViewById<RecyclerView>(R.id.reminderList)
        adapter = ReminderViewAdapter(requireActivity())
        recyclerView.setAdapter(adapter)
        recyclerView.setLayoutManager(LinearLayoutManager(recyclerView.context))
        return recyclerView
    }

    private fun setupSwiping(recyclerView: RecyclerView?) {
        SwipeHelper.createSwipeHelper(
            requireContext(),
            { viewHolder: RecyclerView.ViewHolder? -> deleteItem(viewHolder!!.itemId, viewHolder.getBindingAdapterPosition()) },
            null
        )
            .attachToRecyclerView(recyclerView)
    }

    private fun setupAddReminderButton(fragmentView: View, fullMedicine: FullMedicine) {
        val fab = fragmentView.findViewById<ExtendedFloatingActionButton>(R.id.addReminder)
        fab.setOnClickListener { _: View? -> NewReminderTypeDialog(requireActivity(), fullMedicine, this.medicineViewModel.medicineRepository) }
    }

    private fun sortAndSubmitList(reminders: List<Reminder>) {
        adapter!!.submitList(LinkedReminderAlgorithms().sortRemindersList(reminders))
    }

    private fun deleteItem(itemId: Long, adapterPosition: Int) {
        lifecycleScope.launch(ioDispatcher) {
            val reminder = medicineViewModel.medicineRepository.getReminder(itemId.toInt())
            if (reminder != null) {
                withContext(mainDispatcher) {
                    LinkedReminderHandling(reminder, medicineViewModel.medicineRepository, lifecycleScope).deleteReminder(requireContext(), { }, {
                        adapter!!.notifyItemChanged(adapterPosition)
                    })
                }
            }
        }
    }

    override fun fillEntityData(entity: FullMedicine, fragmentView: View) {
        val medicine = entity.medicine
        medicine.name = (fragmentView.findViewById<View?>(R.id.editMedicineName) as EditText).getText().toString().trim()
        medicine.useColor = enableColor!!.isChecked
        medicine.color = color
        importanceIndexToMedicine(notificationImportance!!.selectedItemPosition, medicine)
        medicine.iconId = iconId
        medicine.notes = notes

        updateReminders(fragmentView)
    }

    private fun updateReminders(fragmentView: View?) {
        if (fragmentView != null) {
            val recyclerView = fragmentView.findViewById<RecyclerView>(R.id.reminderList)
            for (i in 0..<recyclerView.size) {
                val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as ReminderViewHolder

                this.medicineViewModel.medicineRepository.updateReminder(viewHolder.getUpdatedReminder())
            }
        }
    }

    override fun getEntityId(): Int {
        return EditMedicineFragmentArgs.fromBundle(requireArguments()).medicineId
    }

    override val iconDialogIconPack: IconPack
        get() = MedicineIcons(requireContext()).getIconPack()

    override fun onIconDialogCancelled() {
        // Intentionally empty
    }

    override fun onIconDialogIconsSelected(dialog: IconDialog, icons: List<Icon>) {
        iconId = icons[0].id
        selectIconButton!!.setIcon(MedicineIcons(requireContext()).getIconDrawable(iconId))
    }

    companion object {
        private const val ICON_DIALOG_TAG = "icon-dialog"
    }
}

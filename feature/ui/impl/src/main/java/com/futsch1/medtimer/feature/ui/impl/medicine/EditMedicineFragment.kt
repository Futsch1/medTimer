package com.futsch1.medtimer.feature.ui.impl.medicine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.EntityEditOptionsMenu
import com.futsch1.medtimer.core.common.helpers.SimpleIdlingResource
import com.futsch1.medtimer.core.common.helpers.SwipeHelper
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.MedicineIcons
import com.futsch1.medtimer.feature.ui.impl.R
import com.futsch1.medtimer.feature.ui.impl.medicine.dialogs.NewReminderTypeDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
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

    private var medicine: Medicine? = null
    private lateinit var fragmentView: View
    private var fragmentReady = false
    private lateinit var optionsMenu: EntityEditOptionsMenu

    private val idlingResource = SimpleIdlingResource(EditMedicineFragment::class.java.name)

    var iconId: Int = 0
    private lateinit var adapter: ReminderViewAdapter
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
        val recyclerView = setupMedicineList()
        setupSwiping(recyclerView)

        lifecycleScope.launch(ioDispatcher) {
            val medicine = medicineRepository.fetch(getMedicineId()) ?: return@launch

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
                val (newMedicine, updatedReminders) = withContext(mainDispatcher) {
                    Pair(buildMedicine(), collectUpdatedReminders())
                }
                updatedReminders.forEach { reminderRepository.update(it) }
                if (medicine != newMedicine && newMedicine != null) {
                    medicineRepository.update(newMedicine)
                }
            }
        }
    }

    private fun onMedicineLoaded(medicine: Medicine) {
        iconId = medicine.iconId
        notes = medicine.notes

        (requireActivity() as AppCompatActivity).supportActionBar?.title = medicine.name
        fragmentView.findViewById<EditText>(R.id.editMedicineName).apply {
            setText(medicine.name)
        }

        val subMenus = editMedicineSubmenusFactory.create(this, medicine)

        selectIconButton.setIcon(medicineIcons.getIconDrawable(iconId))

        setupMedicineSettings(subMenus)
        setupNotesButton(subMenus)
        setupOpenCalendarButton(subMenus)
        setupStockButton(subMenus)
        setupTagsButton(subMenus)

        setupAddReminderButton(medicine)

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

    private fun setupMedicineSettings(subMenus: EditMedicineSubmenus) {
        val openMedicineSettings =
            fragmentView.findViewById<MaterialButton>(R.id.openMedicineSettings)
        openMedicineSettings.setOnClickListener {
            subMenus.open(
                EditMedicineSubmenus.Submenu.SETTINGS,
                findNavController(openMedicineSettings)
            )
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
            subMenus.open(
                EditMedicineSubmenus.Submenu.STOCK_TRACKING,
                findNavController(openStockTracking)
            )
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

    private fun setupAddReminderButton(medicine: Medicine) {
        val fab = fragmentView.findViewById<ExtendedFloatingActionButton>(R.id.addReminder)
        fab.setOnClickListener { newReminderTypeDialogFactory.create(requireActivity(), medicine) }
    }

    private fun sortAndSubmitList(reminders: List<Reminder>) {
        adapter.submitList(linkedReminderAlgorithms.sortRemindersList(reminders))
    }

    private fun deleteItem(itemId: Long, adapterPosition: Int) {
        lifecycleScope.launch(ioDispatcher) {
            val reminder = reminderRepository.fetch(itemId.toInt())
            if (reminder != null) {
                withContext(mainDispatcher) {
                    linkedReminderHandlingFactory.create(reminder, lifecycleScope)
                        .deleteReminder(requireContext(), { }, {
                            adapter.notifyItemChanged(adapterPosition)
                        })
                }
            }
        }
    }

    private fun buildMedicine(): Medicine? {
        return medicine?.copy(
            name = fragmentView.findViewById<EditText>(R.id.editMedicineName).text.toString()
                .trim(),
            iconId = iconId,
            notes = notes,
        )
    }

    private fun collectUpdatedReminders(): List<Reminder> {
        val recyclerView = fragmentView.findViewById<RecyclerView>(R.id.reminderList)
        return (0..<recyclerView.size).map { i ->
            (recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as ReminderViewHolder).getUpdatedReminder()
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

}
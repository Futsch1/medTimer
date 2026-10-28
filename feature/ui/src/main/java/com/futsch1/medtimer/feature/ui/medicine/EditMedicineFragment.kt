package com.futsch1.medtimer.feature.ui.medicine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.MedicineIcons
import com.futsch1.medtimer.core.ui.ScreenTestTags
import com.futsch1.medtimer.core.ui.component.MedTimerTopAppBar
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.AppOptionsActions
import com.futsch1.medtimer.feature.ui.AppOptionsActionsFactory
import com.futsch1.medtimer.feature.ui.medicine.tags.TagsFragment
import com.maltaisn.icondialog.IconDialog
import com.maltaisn.icondialog.IconDialogSettings
import com.maltaisn.icondialog.data.Icon
import com.maltaisn.icondialog.pack.IconPack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@AndroidEntryPoint
class EditMedicineFragment : Fragment(), IconDialog.Callback {
    override val iconDialogIconPack: IconPack get() = medicineIcons.getIconPack()

    override fun onIconDialogIconsSelected(dialog: IconDialog, icons: List<Icon>) {
        icons.firstOrNull()?.let { editMedicineScreenViewModel.editIcon(it.id) }
    }

    private fun openIconDialog() {
        if (childFragmentManager.isStateSaved || childFragmentManager.findFragmentByTag("medicine_icon") != null) return
        IconDialog.newInstance(IconDialogSettings()).apply {
            selectedIconIds = listOf(editMedicineScreenViewModel.state.icon)
        }.show(childFragmentManager, "medicine_icon")
    }

    @Inject
    @Dispatcher(MedTimerDispatchers.IO)
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @Dispatcher(MedTimerDispatchers.Main)
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    lateinit var appOptionsActionsFactory: AppOptionsActionsFactory

    @Inject
    lateinit var linkedReminderHandlingFactory: LinkedReminderHandling.Factory

    @Inject
    lateinit var editMedicineSubmenusFactory: EditMedicineSubmenus.Factory

    @Inject
    lateinit var editMedicineActionsFactory: EditMedicineActions.Factory

    @Inject
    lateinit var medicineIcons: MedicineIcons

    @Inject
    lateinit var linkedReminderAlgorithms: LinkedReminderAlgorithms

    @Inject
    lateinit var medicineRepository: MedicineRepository

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var tagsFragmentFactory: TagsFragment.Factory

    private lateinit var appOptionsActions: AppOptionsActions
    private val editMedicineScreenViewModel: EditMedicineScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appOptionsActions = appOptionsActionsFactory.create(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val navController = findNavController()

        val editMedicineActions =
            editMedicineActionsFactory.create(editMedicineScreenViewModel, this@EditMedicineFragment, navController)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MedTimerTheme {
                    Column(Modifier.testTag(ScreenTestTags.MEDICINES)) {
                        MedTimerTopAppBar(
                            title = editMedicineScreenViewModel.state.name.ifEmpty { "..." },
                            actions = {
                                EditMedicineMenu(editMedicineActions)
                            },
                        )
                        EditMedicineScreen(
                            editMedicineScreenViewModel = editMedicineScreenViewModel,
                            onEditName = editMedicineScreenViewModel::editName,
                            onCreateReminder = editMedicineScreenViewModel::createReminder,
                            onReminderDelete = editMedicineScreenViewModel::deleteReminder,
                            onReminderEdit = { id: Int, type: ReminderType ->
                                val action =
                                    if (type == ReminderType.OUT_OF_STOCK || type == ReminderType.EXPIRATION_DATE) {
                                        EditMedicineFragmentDirections.actionEditMedicineFragmentToAdvancedReminderPreferencesStockExpirationFragment(
                                            id
                                        )
                                    } else {
                                        EditMedicineFragmentDirections.actionEditMedicineFragmentToAdvancedReminderPreferencesRootFragment(
                                            id
                                        )
                                    }
                                try {
                                    navController.navigate(action)
                                } catch (_: IllegalArgumentException) {
                                    // Intentionally empty (monkey test can cause this to fail)
                                }
                            },
                            onEditReminderAmount = editMedicineScreenViewModel::editReminderAmount,
                            onEditReminderTime = editMedicineScreenViewModel::editReminderTime,
                            onSelectIcon = ::openIconDialog,
                            onEditNotes = editMedicineScreenViewModel::editNotes,
                            onOpenSubmenu = { submenu: EditMedicineSubmenus.Submenu ->
                                editMedicineActions.open(submenu)
                            },
                            onOpenHelp = { appOptionsActions.openHelp("getting-started") }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appOptionsActions.onDestroy()
    }

}

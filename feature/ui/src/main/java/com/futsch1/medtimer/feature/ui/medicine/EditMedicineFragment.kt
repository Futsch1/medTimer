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
import androidx.fragment.app.activityViewModels
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
import com.futsch1.medtimer.feature.ui.AppOptionsMenuHost
import com.futsch1.medtimer.feature.ui.AppOptionsViewModel
import com.futsch1.medtimer.feature.ui.TagFilterViewModel
import com.futsch1.medtimer.feature.ui.medicine.dialogs.NewReminderTypeDialog
import com.futsch1.medtimer.feature.ui.medicine.tags.TagsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@AndroidEntryPoint
class EditMedicineFragment : Fragment() {
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
    lateinit var newReminderTypeDialogFactory: NewReminderTypeDialog.Factory

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
    private val appOptionsViewModel: AppOptionsViewModel by viewModels()
    private val tagFilterViewModel: TagFilterViewModel by activityViewModels()

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
                            title = editMedicineScreenViewModel.fragmentState.medicine?.name ?: "...",
                            actions = {
                                EditMedicineMenu(editMedicineActions)
                                AppOptionsMenuHost(
                                    fragment = this@EditMedicineFragment,
                                    actions = appOptionsActions,
                                    optionsViewModel = appOptionsViewModel,
                                    tagFilterViewModel = tagFilterViewModel,
                                    tagsFragmentFactory = tagsFragmentFactory,
                                )
                            },
                        )
                        EditMedicineScreen(
                            editMedicineScreenViewModel = editMedicineScreenViewModel,
                            onReminderAdd = {
                                val medicine =
                                    editMedicineScreenViewModel.fragmentState.medicine ?: return@EditMedicineScreen

                                newReminderTypeDialogFactory.create(
                                    requireActivity(),
                                    medicine,
                                    appOptionsActions
                                )
                            },
                            onReminderDelete = editMedicineScreenViewModel::deleteReminder,
                            onReminderEdit = { id, type ->
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
                            onEditAmount = editMedicineScreenViewModel::editAmount,
                            onEditTime = editMedicineScreenViewModel::editTime,
                            onEditIcon = editMedicineScreenViewModel::editIcon,
                            onEditNotes = editMedicineScreenViewModel::editNotes,
                            onOpenSubmenu = { submenu ->
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

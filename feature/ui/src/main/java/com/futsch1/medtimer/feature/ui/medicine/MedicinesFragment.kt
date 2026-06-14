package com.futsch1.medtimer.feature.ui.medicine

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.EntityEditOptionsMenu
import com.futsch1.medtimer.core.common.helpers.SimpleIdlingResource
import com.futsch1.medtimer.core.common.helpers.dpToPx
import com.futsch1.medtimer.core.common.helpers.showSoftKeyboard
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.BuildConfig
import com.futsch1.medtimer.feature.ui.OptionsMenuFactory
import com.futsch1.medtimer.feature.ui.R
import com.futsch1.medtimer.feature.ui.TagFilterViewModel
import com.futsch1.medtimer.feature.ui.helpers.DeleteHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MedicinesFragment : Fragment() {
    @Inject
    @Dispatcher(MedTimerDispatchers.IO)
    lateinit var dispatcher: CoroutineDispatcher

    @Inject
    @Dispatcher(MedTimerDispatchers.Main)
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    lateinit var medicineRepository: MedicineRepository

    @Inject
    lateinit var medicinesMenu: MedicinesMenu

    private lateinit var idlingResource: SimpleIdlingResource
    private val tagFilterViewModel: TagFilterViewModel by viewModels()
    private val medicinesViewModel: MedicinesViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<MedicinesViewModel.Factory> { factory ->
                factory.create(
                    tagFilterViewModel
                )
            }
        }
    )
    private lateinit var adapter: MedicineViewAdapter

    @Inject
    lateinit var optionsMenuFactory: OptionsMenuFactory

    @Inject
    lateinit var medicineViewAdapterFactory: MedicineViewAdapter.Factory
    private lateinit var optionsMenu: EntityEditOptionsMenu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        optionsMenu = optionsMenuFactory.create(
            this,
            NavHostFragment.findNavController(this),
            false,
            medicinesViewModel.tagFilterViewModel
        )

        adapter = medicineViewAdapterFactory.create(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().addMenuProvider(medicinesMenu, getViewLifecycleOwner())

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MedTimerTheme {
                    MedicinesScreen(medicinesViewModel)
                }
            }
        }

        /*
        val fragmentView = inflater.inflate(R.layout.fragment_medicines, container, false)
        // Medicine recycler
        val recyclerView = fragmentView.findViewById<RecyclerView>(R.id.medicineList)

        recyclerView.setAdapter(adapter)
        recyclerView.setLayoutManager(LinearLayoutManager(fragmentView.context))

        // Swipe to delete
        val itemTouchHelper = SwipeHelper.createSwipeHelper(
            requireContext(),
            { viewHolder ->
                deleteItem(
                    requireContext(),
                    viewHolder.itemId,
                    viewHolder.getBindingAdapterPosition()
                )
            },
            adapter
        )
        itemTouchHelper.attachToRecyclerView(recyclerView)

        postponeEnterTransition()

        setupAddMedicineButton(fragmentView)

        return fragmentView*/
    }

    override fun onDestroy() {
        super.onDestroy()
        idlingResource.destroy()
        optionsMenu.onDestroy()
    }

    private fun deleteItem(context: Context, itemId: Long, adapterPosition: Int) {
        DeleteHelper.deleteItem(
            context,
            com.futsch1.medtimer.core.ui.R.string.are_you_sure_delete_medicine,
            { lifecycleScope.launch { medicineRepository.delete(itemId.toInt()) } },
            { adapter.notifyItemChanged(adapterPosition) })
    }

    private fun setupAddMedicineButton(fragmentView: View) {
        val fab = fragmentView.findViewById<ExtendedFloatingActionButton>(R.id.addMedicine)
        fab.setOnClickListener { _: View? ->
            val textInputLayout = TextInputLayout(requireContext())
            val editText = TextInputEditText(requireContext())
            editText.setLayoutParams(
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            editText.setHint(com.futsch1.medtimer.core.ui.R.string.medicine_name)
            editText.setSingleLine()
            editText.setId(R.id.medicineName)
            if (!BuildConfig.DEBUG) {
                editText.postDelayed({ editText.showSoftKeyboard() }, 200)
            }

            textInputLayout.addView(editText)
            textInputLayout.setPadding(requireContext().resources.dpToPx(16f).toInt())

            val builder = getAlertBuilder(textInputLayout, editText)
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun getAlertBuilder(
        textInputLayout: TextInputLayout?,
        editText: TextInputEditText
    ): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(requireContext())
            .setView(textInputLayout)
            .setTitle(com.futsch1.medtimer.core.ui.R.string.add_medicine)
            .setPositiveButton(com.futsch1.medtimer.core.ui.R.string.ok) { _: DialogInterface?, _: Int ->
                val text = editText.getText() ?: return@setPositiveButton

                lifecycleScope.launch(dispatcher) {
                    val highestSortOrder = medicineRepository.getHighestSortOrder()
                    val medicine = Medicine.default()
                        .copy(name = text.toString().trim(), sortOrder = highestSortOrder)
                    val medicineId = medicineRepository.create(medicine)
                    withContext(mainDispatcher) { navigateToMedicineId(medicineId) }
                }
            }
            .setNegativeButton(com.futsch1.medtimer.core.ui.R.string.cancel) { dialog, _ -> dialog.dismiss() }
    }

    private fun navigateToMedicineId(medicineId: Int) {
        val navController = findNavController(this.requireView())
        val action = MedicinesFragmentDirections.actionMedicinesFragmentToEditMedicineFragment(
            medicineId
        )
        navController.navigate(action)
    }
}
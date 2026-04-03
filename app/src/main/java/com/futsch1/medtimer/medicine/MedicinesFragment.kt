package com.futsch1.medtimer.medicine

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.helpers.SimpleIdlingResource
import com.futsch1.medtimer.helpers.SwipeHelper
import com.futsch1.medtimer.helpers.dpToPx
import com.futsch1.medtimer.helpers.showSoftKeyboard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
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
    private val medicineViewModel: MedicineViewModel by viewModels()
    private lateinit var adapter: MedicineViewAdapter

    @Inject
    lateinit var optionsMenuFactory: OptionsMenu.Factory

    @Inject
    lateinit var medicineViewAdapterFactory: MedicineViewAdapter.Factory
    private lateinit var optionsMenu: OptionsMenu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        idlingResource = SimpleIdlingResource(MedicinesFragment::class.java.getName())
        idlingResource.setBusy()

        optionsMenu = optionsMenuFactory.create(
            this,
            NavHostFragment.findNavController(this),
            false,
            medicineViewModel
        )

        adapter = medicineViewAdapterFactory.create(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        requireActivity().addMenuProvider(medicinesMenu, getViewLifecycleOwner())

        // Connect view model to recycler view adapter
        viewLifecycleOwner.lifecycleScope.launch {
            medicineViewModel.medicines.collect { l: List<FullMedicine> ->
                adapter.submitList(l)
                medicinesMenu.medicines = l
                startPostponedEnterTransition()
                idlingResource.setIdle()
            }
        }

        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner())

        return fragmentView
    }

    override fun onDestroy() {
        super.onDestroy()
        idlingResource.destroy()
        optionsMenu.onDestroy()
    }

    private fun deleteItem(context: Context, itemId: Long, adapterPosition: Int) {
        DeleteHelper.deleteItem(
            context,
            R.string.are_you_sure_delete_medicine,
            { lifecycleScope.launch { medicineRepository.delete(itemId.toInt()) } },
            { adapter.notifyItemChanged(adapterPosition) })
    }

    private fun setupAddMedicineButton(fragmentView: View) {
        val fab = fragmentView.findViewById<ExtendedFloatingActionButton>(R.id.addMedicine)
        fab.setOnClickListener { _: View? ->
            val textInputLayout = TextInputLayout(requireContext())
            val editText = TextInputEditText(requireContext())
            editText.setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            editText.setHint(R.string.medicine_name)
            editText.setSingleLine()
            editText.setId(R.id.medicineName)
            editText.postDelayed({ editText.showSoftKeyboard() }, 200)

            textInputLayout.addView(editText)
            textInputLayout.setPadding(requireContext().resources.dpToPx(16f).toInt())

            val builder = getAlertBuilder(textInputLayout, editText)
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun getAlertBuilder(textInputLayout: TextInputLayout?, editText: TextInputEditText): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(requireContext())
            .setView(textInputLayout)
            .setTitle(R.string.add_medicine)
            .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                val text = editText.getText() ?: return@setPositiveButton

                lifecycleScope.launch(dispatcher) {
                    val highestSortOrder = medicineRepository.getHighestSortOrder()
                    val medicine = Medicine(text.toString().trim())
                    medicine.sortOrder = highestSortOrder + 1
                    val medicineId = medicineRepository.create(medicine).toInt()
                    withContext(mainDispatcher) { navigateToMedicineId(medicineId) }
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
    }

    private fun navigateToMedicineId(medicineId: Int) {
        val navController = findNavController(this.requireView())
        val action = MedicinesFragmentDirections.actionMedicinesFragmentToEditMedicineFragment(
            medicineId
        )
        navController.navigate(action)
    }
}
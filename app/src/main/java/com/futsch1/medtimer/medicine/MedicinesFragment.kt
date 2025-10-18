package com.futsch1.medtimer.medicine

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.helpers.SimpleIdlingResource
import com.futsch1.medtimer.helpers.SwipeHelper
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicinesFragment(val dispatcher: CoroutineDispatcher = Dispatchers.IO) : Fragment() {
    private var idlingResource: SimpleIdlingResource? = null
    private var medicineViewModel: MedicineViewModel? = null
    private var adapter: MedicineViewAdapter? = null
    private var optionsMenu: OptionsMenu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        idlingResource = SimpleIdlingResource(MedicinesFragment::class.java.getName())
        idlingResource!!.setBusy()

        // Get a new or existing ViewModel from the ViewModelProvider.
        medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]

        optionsMenu = OptionsMenu(
            this,
            medicineViewModel,
            NavHostFragment.findNavController(this), false
        )

        adapter = MedicineViewAdapter(requireActivity(), medicineViewModel!!.medicineRepository)
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
            { viewHolder: RecyclerView.ViewHolder? ->
                deleteItem(
                    requireContext(),
                    viewHolder!!.itemId,
                    viewHolder.getBindingAdapterPosition()
                )
            },
            adapter
        )
        itemTouchHelper.attachToRecyclerView(recyclerView)

        postponeEnterTransition()

        setupAddMedicineButton(fragmentView)

        val medicinesMenu = MedicinesMenu(medicineViewModel!!)
        requireActivity().addMenuProvider(medicinesMenu, getViewLifecycleOwner())

        // Connect view model to recycler view adapter
        medicineViewModel!!.medicines.observe(getViewLifecycleOwner(), Observer { l: List<FullMedicine> ->
            adapter!!.submitList(l)
            startPostponedEnterTransition()
            idlingResource!!.setIdle()
        })

        requireActivity().addMenuProvider(optionsMenu!!, getViewLifecycleOwner())

        return fragmentView
    }

    override fun onDestroy() {
        super.onDestroy()
        if (idlingResource != null) {
            idlingResource!!.destroy()
        }
        if (optionsMenu != null) {
            optionsMenu!!.onDestroy()
        }
    }

    private fun deleteItem(context: Context?, itemId: Long, adapterPosition: Int) {
        val deleteHelper = DeleteHelper(context)
        deleteHelper.deleteItem(
            R.string.are_you_sure_delete_medicine,
            { medicineViewModel!!.medicineRepository.deleteMedicine(itemId.toInt()) },
            { adapter!!.notifyItemChanged(adapterPosition) })
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
            textInputLayout.addView(editText)

            val builder = getAlertBuilder(textInputLayout, editText)
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun getAlertBuilder(textInputLayout: TextInputLayout?, editText: TextInputEditText): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context)
        builder.setView(textInputLayout)
        builder.setTitle(R.string.add_medicine)
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            val e = editText.getText()
            if (e != null) {
                lifecycleScope.launch(dispatcher) {
                    val highestSortOrder = medicineViewModel!!.medicineRepository.highestMedicineSortOrder
                    val medicine = Medicine(e.toString().trim { it <= ' ' })
                    medicine.sortOrder = highestSortOrder + 1
                    val medicineId = medicineViewModel!!.medicineRepository.insertMedicine(medicine).toInt()
                    requireActivity().runOnUiThread { navigateToMedicineId(medicineId) }
                }
            }
        }
        builder.setNegativeButton(R.string.cancel) { dialog: DialogInterface?, _: Int -> dialog!!.dismiss() }
        return builder
    }

    private fun navigateToMedicineId(medicineId: Int) {
        val navController = findNavController(this.requireView())
        val action = MedicinesFragmentDirections.actionMedicinesFragmentToEditMedicineFragment(
            medicineId
        )
        navController.navigate(action)
    }
}
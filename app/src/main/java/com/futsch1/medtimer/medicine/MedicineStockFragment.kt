package com.futsch1.medtimer.medicine

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.google.android.material.textfield.TextInputEditText

class MedicineStockFragment : Fragment() {
    private lateinit var fragmentView: View
    private val thread = HandlerThread("MedicineStock")
    private var medicineId: Int = -1
    private lateinit var medicineViewModel: MedicineViewModel
    private lateinit var medicine: Medicine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.thread.start()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.fragment_medicine_stock, container, false)
        postponeEnterTransition()

        medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        medicineId = MedicineStockFragmentArgs.fromBundle(requireArguments()).medicineId
        medicineViewModel.getLiveMedicine(medicineId).observe(
            viewLifecycleOwner
        ) { medicine: Medicine ->
            this.medicine = medicine
            this.setupViews()
        }

        val optionsMenu = OptionsMenu(
            this.requireContext(),
            MedicineViewModel(requireActivity().application),
            this,
            fragmentView
        )
        requireActivity().addMenuProvider(optionsMenu, viewLifecycleOwner)

        return fragmentView
    }

    @SuppressLint("SetTextI18n")
    private fun setupViews() {
        fragmentView.findViewById<TextInputEditText>(R.id.amountLeft)
            .setText(medicine.medicationAmount.toString())

        startPostponedEnterTransition()
    }

    override fun onDestroy() {
        super.onDestroy()
        thread.quitSafely()
    }

    override fun onStop() {
        super.onStop()
        if (this::medicine.isInitialized) {
            try {
                medicine.medicationAmount =
                    fragmentView.findViewById<TextInputEditText>(R.id.amountLeft).text.toString()
                        .toInt()
                medicineViewModel.updateMedicine(medicine)
            } catch (e: NumberFormatException) {
                // Empty for now
            }
        }
    }
}
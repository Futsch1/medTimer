package com.futsch1.medtimer.feature.ui.medicine.tags

import android.app.ActionBar.LayoutParams
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.feature.ui.R
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Edits the tags of one medicine, or the tag filter when no medicine is given. The data provider is
 * built from the arguments, so the system can restore the dialog after a configuration change.
 */
@AndroidEntryPoint
class TagsFragment : DialogFragment() {

    @Inject
    lateinit var tagDataFromMedicineFactory: TagDataFromMedicine.Factory

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_fragment_tags, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Providers collect on the view lifecycle, which exists only from here on.
        val tagDataProvider = createTagDataProvider()

        recyclerView = view.findViewById(R.id.tags)
        recyclerView.layoutManager = FlexboxLayoutManager(requireContext())
        recyclerView.adapter = tagDataProvider.getAdapter()

        view.findViewById<MaterialButton>(R.id.ok).setOnClickListener {
            dismiss()
        }

        setupAddTag(view, tagDataProvider)

        requireDialog().window!!.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        recyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            requireView().post { if (isAdded) capDialogHeight(requireView()) }
        }
    }

    private fun createTagDataProvider(): TagDataProvider {
        val args = requireArguments()
        return if (args.containsKey(ARG_MEDICINE_ID)) {
            tagDataFromMedicineFactory.create(this, args.getInt(ARG_MEDICINE_ID))
        } else {
            TagDataFromPreferences(this)
        }
    }

    private fun capDialogHeight(view: View) {
        val maxHeight = (resources.displayMetrics.heightPixels * 0.8f).toInt()
        if (view.height > maxHeight) {
            val nonRecyclerHeight = view.height - recyclerView.height
            val targetHeight = maxHeight - nonRecyclerHeight
            if (recyclerView.height != targetHeight) {
                recyclerView.updateLayoutParams { height = targetHeight }
            }
        }
        requireDialog().window!!.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private fun setupAddTag(view: View, tagDataProvider: TagDataProvider) {
        val addTagButton = view.findViewById<MaterialButton>(R.id.addTag)
        if (!tagDataProvider.canAddTag()) {
            addTagButton.visibility = View.GONE
            return
        }

        addTagButton.visibility = View.VISIBLE
        addTagButton.setOnClickListener {
            AddTagDialogFragment().show(childFragmentManager, AddTagDialogFragment.TAG)
        }
        childFragmentManager.setFragmentResultListener(AddTagDialogFragment.REQUEST_KEY, viewLifecycleOwner) { _, result ->
            val tagName = result.getString(AddTagDialogFragment.RESULT_NAME)
            if (!tagName.isNullOrBlank()) {
                tagDataProvider.addTag(tagName)
            }
        }
    }

    companion object {
        private const val ARG_MEDICINE_ID = "medicineId"

        fun forMedicine(medicineId: Int) = TagsFragment().apply { arguments = Bundle().apply { putInt(ARG_MEDICINE_ID, medicineId) } }

        fun forFilter() = TagsFragment().apply { arguments = Bundle() }
    }
}

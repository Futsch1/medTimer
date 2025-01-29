package com.futsch1.medtimer.medicine.tags

import android.app.ActionBar.LayoutParams
import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.helpers.InitIdlingResource
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.button.MaterialButton


class TagsFragment(
    private val medicineId: Int,
    private val editable: Boolean = false,
    private val selectable: Boolean = true
) :
    DialogFragment() {

    private val idlingResource = InitIdlingResource(TagsFragment::class.java.name)
    private lateinit var viewModel: MedicineWithTagsViewModel
    private lateinit var tagsAdapter: TagsAdapter
    private lateinit var tagsWithStateCollector: TagWithStateCollector

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        idlingResource.resetInitialized()

        val view = inflater.inflate(R.layout.dialog_fragment_tags, container, false)

        viewModel = ViewModelProvider(
            this,
            MedicineWithTagsViewModel.Factory(
                context?.applicationContext as Application,
                medicineId
            )
        )[MedicineWithTagsViewModel::class.java]
        tagsAdapter = TagsAdapter(viewModel).selectable(selectable)
        tagsWithStateCollector =
            TagWithStateCollector(tagsAdapter) { idlingResource.setInitialized() }

        val recyclerView = view.findViewById<RecyclerView>(R.id.tags)
        recyclerView.layoutManager = FlexboxLayoutManager(requireContext())
        recyclerView.adapter = tagsAdapter

        val okButton = view.findViewById<MaterialButton>(R.id.ok)
        okButton.setOnClickListener {
            dismiss()
        }

        setupAddTag(view)

        viewModel.tags.observe(this) {
            tagsWithStateCollector.tags = it
        }
        viewModel.medicineWithTags.observe(this) {
            tagsWithStateCollector.medicineWithTags = it
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog!!.window!!.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroy() {
        super.onDestroy()
        idlingResource.destroy()
    }

    private fun setupAddTag(view: View) {
        val addTagButton = view.findViewById<MaterialButton>(R.id.addTag)
        if (editable) {
            addTagButton.visibility = View.VISIBLE
            addTagButton.setOnClickListener {
                DialogHelper(requireContext())
                    .title(R.string.add_tag)
                    .hint(R.string.name)
                    .textSink { tagName: String? ->
                        if (!tagName.isNullOrBlank()) {
                            val tagId = viewModel.medicineRepository.insertTag(Tag(tagName))
                            viewModel.associateTag(tagId.toInt())
                            growWindow()
                        }
                    }
                    .show()
            }
        } else {
            addTagButton.visibility = View.GONE
        }
    }

    private fun growWindow() {
        dialog!!.window!!.setLayout(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
    }
}
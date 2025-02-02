package com.futsch1.medtimer.medicine.tags

import android.app.ActionBar.LayoutParams
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.DialogHelper
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.button.MaterialButton


class TagsFragment(
    private val tagDataProvider: TagDataProvider
) :
    DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_fragment_tags, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.tags)
        recyclerView.layoutManager = FlexboxLayoutManager(requireContext())
        recyclerView.adapter = tagDataProvider.getAdapter()

        val okButton = view.findViewById<MaterialButton>(R.id.ok)
        okButton.setOnClickListener {
            dismiss()
        }

        setupAddTag(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog!!.window!!.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private fun setupAddTag(view: View) {
        val addTagButton = view.findViewById<MaterialButton>(R.id.addTag)
        if (tagDataProvider.canAddTag()) {
            addTagButton.visibility = View.VISIBLE
            addTagButton.setOnClickListener {
                DialogHelper(requireContext())
                    .title(R.string.add_tag)
                    .hint(R.string.name)
                    .textSink { tagName: String? ->
                        if (!tagName.isNullOrBlank()) {
                            tagDataProvider.addTag(tagName)
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
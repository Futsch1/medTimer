package com.futsch1.medtimer.medicine.tags

import android.app.ActionBar.LayoutParams
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.DialogHelper
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.button.MaterialButton


class TagsFragment(private val editable: Boolean = false, selectable: Boolean = true) :
    DialogFragment() {
    private val tagAdapter =
        TagsAdapter(mutableListOf(TagWithState(Tag("test"), false)), selectable)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_fragment_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog!!.window!!.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        val recyclerView = view.findViewById<RecyclerView>(R.id.tags)
        recyclerView.layoutManager = FlexboxLayoutManager(requireContext())
        recyclerView.adapter = tagAdapter

        val okButton = view.findViewById<MaterialButton>(R.id.ok)
        okButton.setOnClickListener {
            dismiss()
        }

        setupAddTag(view)
    }

    private fun setupAddTag(view: View) {
        val addTagButton = view.findViewById<MaterialButton>(R.id.addTag)
        if (editable) {
            addTagButton.visibility = View.VISIBLE
            addTagButton.setOnClickListener {
                DialogHelper(requireContext())
                    .title(R.string.add_tag)
                    .hint(R.string.name)
                    .initialText("Blablabla")
                    .textSink { tagName: String? ->
                        if (!tagName.isNullOrBlank()) {
                            tagAdapter.tags.add(TagWithState(Tag(tagName), false))
                            tagAdapter.notifyItemInserted(tagAdapter.tags.size - 1)
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
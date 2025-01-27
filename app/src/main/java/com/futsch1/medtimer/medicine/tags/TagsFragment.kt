package com.futsch1.medtimer.medicine.tags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Tag
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.button.MaterialButton

class TagsFragment(val editable: Boolean) : DialogFragment() {
    private val tagAdapter =
        TagsAdapter(mutableListOf(TagWithState(Tag("test"), false))) // Initialize with empty list

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_fragment_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        val addTagLayout = view.findViewById<LinearLayout>(R.id.addTagLayout)
        if (editable) {
            addTagLayout.visibility = View.VISIBLE
            val addTagButton = view.findViewById<MaterialButton>(R.id.addTag)
            val addTagName = view.findViewById<EditText>(R.id.addTagName)
            addTagButton.setOnClickListener {
                if (addTagName.text.isNotBlank()) {
                    tagAdapter.tags.add(TagWithState(Tag(addTagName.text.toString()), false))
                    tagAdapter.notifyItemInserted(tagAdapter.tags.size - 1)
                }
            }
        } else {
            addTagLayout.visibility = View.GONE
        }
    }
}
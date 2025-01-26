package com.futsch1.medtimer.medicine.tags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.button.MaterialButton

class TagsFragment : DialogFragment() {
    private val tagAdapter = TagsAdapter(listOf()) // Initialize with empty list

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
    }
}
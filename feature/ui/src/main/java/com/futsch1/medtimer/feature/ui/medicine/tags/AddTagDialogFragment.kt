package com.futsch1.medtimer.feature.ui.medicine.tags

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.helpers.TextInputDialogBuilder

/** Asks for a new tag name and hands it to the parent fragment as a fragment result under [REQUEST_KEY]. */
class AddTagDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        TextInputDialogBuilder(requireContext())
            .title(R.string.add_tag)
            .hint(R.string.name)
            .textSink { tagName -> setFragmentResult(REQUEST_KEY, Bundle().apply { putString(RESULT_NAME, tagName) }) }
            .create()

    companion object {
        const val TAG = "addTag"
        const val REQUEST_KEY = "addTag"
        const val RESULT_NAME = "name"
    }
}

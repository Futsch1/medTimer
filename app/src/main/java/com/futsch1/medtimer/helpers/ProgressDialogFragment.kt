package com.futsch1.medtimer.helpers

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.futsch1.medtimer.R

class ProgressDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        return Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_progress)
        }
    }
}

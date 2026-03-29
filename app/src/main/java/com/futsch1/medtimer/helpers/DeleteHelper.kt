package com.futsch1.medtimer.helpers

import android.content.Context
import com.futsch1.medtimer.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DeleteHelper {
    fun deleteItem(context: Context, messageStringId: Int, yesClicked: () -> Unit, noClicked: () -> Unit) {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.confirm)
        builder.setMessage(messageStringId)
        builder.setPositiveButton(R.string.yes) { _, _ -> yesClicked() }
        builder.setNegativeButton(R.string.cancel) { _, _ -> noClicked() }
        builder.setOnCancelListener { _ -> noClicked() }
        builder.show()
    }
}

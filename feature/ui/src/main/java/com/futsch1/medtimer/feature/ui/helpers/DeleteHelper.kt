package com.futsch1.medtimer.feature.ui.helpers

import android.content.Context
import com.futsch1.medtimer.feature.ui.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DeleteHelper {
    fun deleteItem(context: Context, messageStringId: Int, yesClicked: () -> Unit, noClicked: () -> Unit) {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(com.futsch1.medtimer.core.ui.R.string.confirm)
        builder.setMessage(messageStringId)
        builder.setPositiveButton(com.futsch1.medtimer.core.ui.R.string.yes) { _, _ -> yesClicked() }
        builder.setNegativeButton(com.futsch1.medtimer.core.ui.R.string.cancel) { _, _ -> noClicked() }
        builder.setOnCancelListener { _ -> noClicked() }
        builder.show()
    }
}

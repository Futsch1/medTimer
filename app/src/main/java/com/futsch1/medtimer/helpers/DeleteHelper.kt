package com.futsch1.medtimer.helpers

import android.app.AlertDialog
import android.content.Context
import com.futsch1.medtimer.R

object DeleteHelper {
    fun deleteItem(context: Context, messageStringId: Int, yesClicked: () -> Unit, noClicked: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.confirm)
        builder.setMessage(messageStringId)
        builder.setPositiveButton(R.string.yes) { _, _ -> yesClicked() }
        builder.setNegativeButton(R.string.cancel) { _, _ -> noClicked() }
        builder.setOnCancelListener { _ -> noClicked() }
        builder.show()
    }
}

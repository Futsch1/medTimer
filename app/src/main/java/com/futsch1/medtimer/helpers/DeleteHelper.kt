package com.futsch1.medtimer.helpers

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import com.futsch1.medtimer.R

class DeleteHelper(private val context: Context?) {
    fun deleteItem(messageStringId: Int, yesClicked: ButtonCallback, noClicked: ButtonCallback) {
        if (context == null) {
            return
        }
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.confirm)
        builder.setMessage(messageStringId)
        builder.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int -> yesClicked.onButtonClick() }
        builder.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> noClicked.onButtonClick() }
        builder.setOnCancelListener { _: DialogInterface? -> noClicked.onButtonClick() }
        builder.show()
    }

    fun interface ButtonCallback {
        fun onButtonClick()
    }
}

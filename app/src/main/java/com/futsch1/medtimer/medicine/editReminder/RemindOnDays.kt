package com.futsch1.medtimer.medicine.editReminder

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.widget.Button
import com.futsch1.medtimer.R

class RemindOnDays(
    private val context: Context,
    private val button: Button,
    private val strings: Strings,
    private val daysList: Array<String>,
    private val selectedCallback: (Int) -> (Boolean),
    private val selectCallback: (Int, Boolean) -> (Unit)
) {
    private val builder: AlertDialog.Builder =
        AlertDialog.Builder(context)
            .setTitle(R.string.remind_on)
            .setCancelable(false)
    private val checkedItems = BooleanArray(daysList.size)

    init {
        setText()

        builder.setPositiveButton(R.string.ok) { _, _ ->
            closeAndApply()
        }

        builder.setNegativeButton(
            R.string.cancel
        ) { dialogInterface: DialogInterface, _ -> dialogInterface.dismiss() }

        button.setOnClickListener { _ ->
            for (i in daysList.indices) {
                checkedItems[i] = selectedCallback(i)
            }
            builder.setMultiChoiceItems(
                daysList, checkedItems
            ) { _, i: Int, b: Boolean ->
                checkedItems[i] = b
            }

            val isClearAll = checkedItems.count { v -> v } > checkedItems.size / 2
            builder.setNeutralButton(if (isClearAll) R.string.clear_all else R.string.select_all) { _, _ ->
                checkedItems.fill(!isClearAll)
                closeAndApply()
            }

            builder.show()
        }
    }

    private fun closeAndApply() {
        for (j in daysList.indices) {
            selectCallback(j, checkedItems[j])
        }
        setText()
    }

    private fun setText() {
        val checkedDays = ArrayList<String>()
        for (j in daysList.indices) {
            if (selectedCallback(j)) {
                checkedDays.add(daysList[j])
            }
        }

        if (checkedDays.size == daysList.size) {
            button.setText(strings.allSelectedString)
        } else if (checkedDays.isEmpty()) {
            button.setText(strings.noneSelectedString)
        } else {
            if (strings.partsSelectedPlaceholderString != null) {
                button.text = context.getString(
                    strings.partsSelectedPlaceholderString,
                    checkedDays.joinToString(", ")
                )
            } else {
                button.text = checkedDays.joinToString(", ")
            }
        }
    }

    data class Strings(
        val allSelectedString: Int, val partsSelectedPlaceholderString: Int? = null,
        val noneSelectedString: Int = R.string.never
    )
}
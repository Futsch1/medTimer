package com.futsch1.medtimer.medicine.editors

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
    selectedCallback: (Int) -> (Boolean)
) {
    private val builder: AlertDialog.Builder =
        AlertDialog.Builder(context)
            .setTitle(R.string.remind_on)
            .setCancelable(false)
    private val checkedItems = BooleanArray(daysList.size)

    init {
        for (i in daysList.indices) {
            checkedItems[i] = selectedCallback(i)
        }

        setText()

        builder.setPositiveButton(R.string.ok) { _, _ ->
            setText()
        }

        builder.setNegativeButton(
            R.string.cancel
        ) { dialogInterface: DialogInterface, _ -> dialogInterface.dismiss() }

        button.setOnClickListener { _ ->
            builder.setMultiChoiceItems(
                daysList, checkedItems
            ) { _, i: Int, b: Boolean ->
                checkedItems[i] = b
            }

            val isClearAll = checkedItems.count { v -> v } > checkedItems.size / 2
            builder.setNeutralButton(if (isClearAll) R.string.clear_all else R.string.select_all) { _, _ ->
                checkedItems.fill(!isClearAll)
                setText()
            }

            builder.show()
        }
    }

    fun getIndices(): ArrayList<Pair<Int, Boolean>> {
        val checkedIndices = ArrayList<Pair<Int, Boolean>>()
        for (i in daysList.indices) {
            checkedIndices.add(Pair(i, checkedItems[i]))
        }
        return checkedIndices
    }

    private fun setText() {
        val checkedDays = ArrayList<String>()
        for (j in daysList.indices) {
            if (checkedItems[j]) {
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
package com.futsch1.medtimer.feature.ui.medicine

import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.core.common.helpers.DatePickerDialogFactory
import com.futsch1.medtimer.core.common.helpers.LocaleContextAccessor
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.google.android.material.timepicker.TimeFormat
import java.time.LocalDate
import java.time.LocalTime

internal fun showMaterialTimePicker(
    context: Context,
    time: LocalTime,
    @StringRes titleText: Int? = null,
    force24Hour: Boolean = false,
    onTimeSelected: (LocalTime) -> Unit,
) {
    val factory = TimePickerDialogFactory(LocaleContextAccessor(context.applicationContext))
    val onPositiveButtonClick: (Int) -> Unit = { minutes ->
        onTimeSelected(LocalTime.of(minutes / 60, minutes % 60))
    }
    val picker = if (force24Hour) {
        factory.create(time, titleText, TimeFormat.CLOCK_24H, onPositiveButtonClick)
    } else {
        factory.create(time, titleText, onPositiveButtonClick = onPositiveButtonClick)
    }
    val activity = context.findFragmentActivity()
    picker.show(activity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
}

internal fun showMaterialDatePicker(
    context: Context,
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val picker = DatePickerDialogFactory().create(date) { selectedEpochDay ->
        onDateSelected(LocalDate.ofEpochDay(selectedEpochDay))
    }
    picker.show(context.findFragmentActivity().supportFragmentManager, DatePickerDialogFactory.DIALOG_TAG)
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> error("MaterialTimePicker requires a FragmentActivity context")
}

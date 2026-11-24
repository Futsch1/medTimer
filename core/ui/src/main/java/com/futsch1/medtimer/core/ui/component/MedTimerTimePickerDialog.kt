package com.futsch1.medtimer.core.ui.component

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.futsch1.medtimer.core.common.helpers.withPrimaryLocale
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import java.time.LocalTime

object PickerTestTags {
    const val TIME_PICKER = "time_picker_dialog"
    const val DATE_PICKER = "date_picker_dialog"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedTimerTimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes title: Int? = null,
) {
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context.withPrimaryLocale()) }
    val state = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute, is24Hour = is24Hour)
    var isPickerMode by rememberSaveable { mutableStateOf(true) }
    val dialFits = LocalWindowInfo.current.containerDpSize.height > TimePickerDialogDefaults.MinHeightForTimePicker
    val displayMode = if (isPickerMode && dialFits) TimePickerDisplayMode.Picker else TimePickerDisplayMode.Input

    TimePickerDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.testTag(PickerTestTags.TIME_PICKER),
        title = {
            if (title != null) {
                Text(stringResource(title))
            } else {
                TimePickerDialogDefaults.Title(displayMode = displayMode)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
        modeToggleButton = {
            if (dialFits) {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = { isPickerMode = !isPickerMode },
                    displayMode = displayMode,
                )
            }
        },
    ) {
        if (displayMode == TimePickerDisplayMode.Picker) {
            TimePicker(state = state)
        } else {
            TimeInput(state = state)
        }
    }
}

@MedTimerPreview
@Composable
private fun MedTimerTimePickerDialogPreview() {
    MedTimerTheme {
        MedTimerTimePickerDialog(initialTime = LocalTime.of(8, 30), onConfirm = {}, onDismissRequest = {})
    }
}

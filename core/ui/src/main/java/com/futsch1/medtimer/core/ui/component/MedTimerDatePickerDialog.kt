package com.futsch1.medtimer.core.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun MedTimerDatePickerDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes title: Int? = null,
) {
    // DatePickerState works in UTC midnight millis, independent of the device time zone.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.testTag(PickerTestTags.DATE_PICKER),
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(
            state = state,
            title = title?.let {
                { Text(stringResource(it), modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) }
            } ?: { DatePickerDefaults.DatePickerTitle(displayMode = state.displayMode) },
        )
    }
}

@MedTimerPreview
@Composable
private fun MedTimerDatePickerDialogPreview() {
    MedTimerTheme {
        MedTimerDatePickerDialog(initialDate = LocalDate.of(2026, 1, 8), onConfirm = {}, onDismissRequest = {})
    }
}

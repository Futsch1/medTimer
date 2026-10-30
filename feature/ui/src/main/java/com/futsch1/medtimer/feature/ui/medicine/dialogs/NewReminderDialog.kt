package com.futsch1.medtimer.feature.ui.medicine.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.Interval
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.medicine.EditTextButton
import com.futsch1.medtimer.feature.ui.medicine.EditValueButton
import com.futsch1.medtimer.feature.ui.medicine.NewReminderDraft
import com.futsch1.medtimer.feature.ui.medicine.NewReminderExpirationReminderType
import com.futsch1.medtimer.feature.ui.medicine.NewReminderState
import com.futsch1.medtimer.feature.ui.medicine.NewReminderStockReminderType
import com.futsch1.medtimer.feature.ui.medicine.PreviewNewReminderState
import com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences.EditIntervalDialog
import com.futsch1.medtimer.feature.ui.medicine.showMaterialDatePicker
import com.futsch1.medtimer.feature.ui.medicine.showMaterialTimePicker
import java.time.LocalDateTime
import java.time.LocalTime
import com.futsch1.medtimer.core.ui.R as CoreUiR

object NewReminderTestTags {
    const val DIALOG = "new_reminder_dialog"
    const val HELP_BUTTON = "new_reminder_help_button"
}

private val creationTypes = listOf(
    ReminderType.TIME_BASED,
    ReminderType.CONTINUOUS_INTERVAL,
    ReminderType.WINDOWED_INTERVAL,
    ReminderType.OUT_OF_STOCK,
    ReminderType.EXPIRATION_DATE,
)

@Composable
fun NewReminderFlowDialog(
    state: NewReminderState,
    medicineUnit: String,
    onCreate: () -> Unit,
    onDraftChange: (draft: NewReminderDraft) -> Unit,
    onDismiss: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val draft = state.draft
    val type = draft.selectedType
    AlertDialog(
        modifier = Modifier.testTag(NewReminderTestTags.DIALOG),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(type?.let(::titleFor) ?: CoreUiR.string.add_reminder)) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (type == null) {
                    ReminderTypeChooser(onSelect = { onDraftChange(draft.copy(selectedType = it)) })
                } else {
                    ReminderDetailsFields(
                        type = type,
                        medicineUnit = medicineUnit,
                        state = state,
                        draft = draft,
                        onDraftChange = onDraftChange,
                    )
                }
            }
        },
        confirmButton = {
            if (type == null) {
                TextButton(
                    onClick = onOpenHelp,
                    modifier = Modifier.testTag(NewReminderTestTags.HELP_BUTTON),
                ) { Text(stringResource(CoreUiR.string.reminder_types_help)) }
            } else {
                TextButton(onClick = onCreate) {
                    Text(stringResource(CoreUiR.string.create_reminder))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(CoreUiR.string.cancel))
            }
        },
    )
}

@Composable
private fun ReminderTypeChooser(onSelect: (ReminderType) -> Unit) {
    creationTypes.forEach { type ->
        Card(
            Modifier
                .fillMaxWidth()
                .clickable { onSelect(type) }) {
            Row(
                Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(iconFor(type)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
                Column {
                    Text(stringResource(titleFor(type)), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(helpFor(type)), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ReminderDetailsFields(
    type: ReminderType,
    medicineUnit: String,
    state: NewReminderState,
    draft: NewReminderDraft,
    onDraftChange: (NewReminderDraft) -> Unit,
) {
    when (type) {
        ReminderType.OUT_OF_STOCK -> StockReminderFields(medicineUnit, state, draft, onDraftChange)
        ReminderType.EXPIRATION_DATE -> ExpirationReminderFields(state, draft, onDraftChange)
        else -> ScheduledReminderFields(type, state, draft, onDraftChange)
    }
    if (state.invalid) Text(stringResource(CoreUiR.string.invalid_input), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun StockReminderFields(
    medicineUnit: String,
    state: NewReminderState,
    draft: NewReminderDraft,
    onDraftChange: (NewReminderDraft) -> Unit,
) {
    EditTextButton(
        label = stringResource(CoreUiR.string.reminder_threshold),
        value = "${draft.threshold} $medicineUnit",
        onConfirm = { onDraftChange(draft.copy(threshold = it)) },
        modifier = Modifier.fillMaxWidth(),
    )
    RadioChoiceGroup {
        RadioChoice(
            stringResource(CoreUiR.string.once_below_threshold),
            draft.stockReminderType == NewReminderStockReminderType.ONCE
        ) {
            onDraftChange(draft.copy(stockReminderType = NewReminderStockReminderType.ONCE))
        }
        RadioChoice(
            stringResource(CoreUiR.string.always_below_threshold),
            draft.stockReminderType == NewReminderStockReminderType.ALWAYS
        ) {
            onDraftChange(draft.copy(stockReminderType = NewReminderStockReminderType.ALWAYS))
        }
        RadioChoice(
            stringResource(CoreUiR.string.daily_below_threshold),
            draft.stockReminderType == NewReminderStockReminderType.DAILY
        ) {
            onDraftChange(draft.copy(stockReminderType = NewReminderStockReminderType.DAILY))
        }
    }
    if (draft.stockReminderType == NewReminderStockReminderType.DAILY) {
        TimeField(CoreUiR.string.time, draft.time, state.timeText) { onDraftChange(draft.copy(time = it)) }
    }
}

@Composable
private fun ExpirationReminderFields(
    state: NewReminderState,
    draft: NewReminderDraft,
    onDraftChange: (NewReminderDraft) -> Unit,
) {
    EditTextButton(
        label = stringResource(CoreUiR.string.remind_days_before_expiration),
        value = draft.expirationDays,
        onConfirm = { onDraftChange(draft.copy(expirationDays = it)) },
        modifier = Modifier.fillMaxWidth(),
    )
    RadioChoiceGroup {
        RadioChoice(
            stringResource(CoreUiR.string.once),
            draft.expirationReminderType == NewReminderExpirationReminderType.ONCE
        ) {
            onDraftChange(draft.copy(expirationReminderType = NewReminderExpirationReminderType.ONCE))
        }
        RadioChoice(
            stringResource(CoreUiR.string.daily),
            draft.expirationReminderType == NewReminderExpirationReminderType.DAILY
        ) {
            onDraftChange(draft.copy(expirationReminderType = NewReminderExpirationReminderType.DAILY))
        }
    }
    TimeField(CoreUiR.string.time, draft.time, state.timeText) { onDraftChange(draft.copy(time = it)) }
}

@Composable
private fun ScheduledReminderFields(
    type: ReminderType,
    state: NewReminderState,
    draft: NewReminderDraft,
    onDraftChange: (NewReminderDraft) -> Unit,
) {
    EditTextButton(
        label = stringResource(CoreUiR.string.dosage),
        value = draft.amount,
        onConfirm = { onDraftChange(draft.copy(amount = it)) },
        modifier = Modifier.fillMaxWidth(),
    )
    when (type) {
        ReminderType.TIME_BASED -> TimeField(CoreUiR.string.time, draft.time, state.timeText) {
            onDraftChange(draft.copy(time = it))
        }

        ReminderType.CONTINUOUS_INTERVAL, ReminderType.WINDOWED_INTERVAL -> {
            val context = LocalContext.current
            val maxIntervalMinutes =
                if (type == ReminderType.WINDOWED_INTERVAL) 24 * 60 else Interval.MAX_INTERVAL_MINUTES
            val intervalMinutes = draft.interval.toIntOrNull()
                ?.let { Interval(it, draft.intervalUnit).minutesValue } ?: (12 * 60)
            EditValueButton(
                label = stringResource(CoreUiR.string.interval),
                value = Interval(intervalMinutes, maxIntervalMinutes).toTranslatedString(context),
                onClick = {
                    val reminder = Reminder.default().copy(
                        time = ReminderTime(intervalMinutes, isDuration = true),
                        windowedInterval = type == ReminderType.WINDOWED_INTERVAL,
                    )
                    EditIntervalDialog(context, reminder) { updatedMinutes ->
                        val updatedInterval = Interval(updatedMinutes, maxIntervalMinutes)
                        onDraftChange(
                            draft.copy(
                                interval = updatedInterval.getValue().toString(),
                                intervalUnit = updatedInterval.getUnit(),
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (type == ReminderType.CONTINUOUS_INTERVAL) {
                EditValueButton(
                    label = stringResource(CoreUiR.string.interval_start_time),
                    value = state.intervalStartText,
                    onClick = {
                        showMaterialDatePicker(context, draft.intervalStart.toLocalDate()) { selectedDate ->
                            showMaterialTimePicker(context, draft.intervalStart.toLocalTime()) { selectedTime ->
                                onDraftChange(draft.copy(intervalStart = LocalDateTime.of(selectedDate, selectedTime)))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                RadioChoiceGroup {
                    RadioChoice(
                        stringResource(CoreUiR.string.interval_start_reminded),
                        !draft.intervalStartsFromProcessed,
                    ) { onDraftChange(draft.copy(intervalStartsFromProcessed = false)) }
                    RadioChoice(
                        stringResource(CoreUiR.string.interval_start_processed),
                        draft.intervalStartsFromProcessed,
                    ) { onDraftChange(draft.copy(intervalStartsFromProcessed = true)) }
                }
            } else {
                TimeField(CoreUiR.string.daily_start_time, draft.dailyStart, state.dailyStartText) {
                    onDraftChange(draft.copy(dailyStart = it))
                }
                TimeField(CoreUiR.string.daily_end_time, draft.dailyEnd, state.dailyEndText) {
                    onDraftChange(draft.copy(dailyEnd = it))
                }
            }
        }

        else -> Unit
    }
}

@Composable
private fun TimeField(label: Int, time: LocalTime, displayValue: String, onValueChange: (LocalTime) -> Unit) {
    val context = LocalContext.current
    EditValueButton(
        label = stringResource(label),
        value = displayValue,
        onClick = { showMaterialTimePicker(context, time, onTimeSelected = onValueChange) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RadioChoiceGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun RadioChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick)
        Text(text)
    }
}

private fun iconFor(type: ReminderType) = when (type) {
    ReminderType.TIME_BASED -> CoreUiR.drawable.calendar_week
    ReminderType.CONTINUOUS_INTERVAL -> CoreUiR.drawable.repeat
    ReminderType.WINDOWED_INTERVAL -> CoreUiR.drawable.interval
    ReminderType.OUT_OF_STOCK -> CoreUiR.drawable.box_seam
    ReminderType.EXPIRATION_DATE -> CoreUiR.drawable.ban
    else -> CoreUiR.drawable.calendar_week
}

private fun titleFor(type: ReminderType) = when (type) {
    ReminderType.TIME_BASED -> CoreUiR.string.time_based_reminder
    ReminderType.CONTINUOUS_INTERVAL -> CoreUiR.string.continuous_interval_reminder
    ReminderType.WINDOWED_INTERVAL -> CoreUiR.string.windowed_interval_reminder
    ReminderType.OUT_OF_STOCK -> CoreUiR.string.out_of_stock_reminder
    ReminderType.EXPIRATION_DATE -> CoreUiR.string.expiration_reminder
    else -> CoreUiR.string.add_reminder
}

private fun helpFor(type: ReminderType) = when (type) {
    ReminderType.TIME_BASED -> CoreUiR.string.time_based_reminder_help
    ReminderType.CONTINUOUS_INTERVAL -> CoreUiR.string.continuous_interval_reminder_help
    ReminderType.WINDOWED_INTERVAL -> CoreUiR.string.windowed_interval_reminder_help
    ReminderType.OUT_OF_STOCK -> CoreUiR.string.out_of_stock_reminder_help
    else -> CoreUiR.string.expiration_date_reminder_help
}


@MedTimerPreview
@Composable
private fun NewReminderFlowDialogPreview() {
    val state = PreviewNewReminderState().apply {
        draft = NewReminderDraft(selectedType = ReminderType.TIME_BASED, amount = "1")
    }
    MedTimerTheme {
        NewReminderFlowDialog(
            state = state,
            medicineUnit = "tablet",
            onDraftChange = { state.draft = it },
            onCreate = {},
            onDismiss = {},
            onOpenHelp = {},
        )
    }
}

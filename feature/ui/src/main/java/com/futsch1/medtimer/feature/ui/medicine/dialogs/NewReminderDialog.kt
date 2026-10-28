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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.Interval
import com.futsch1.medtimer.core.ui.IntervalUnit
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.medicine.EditTextButton
import com.futsch1.medtimer.feature.ui.medicine.EditValueButton
import com.futsch1.medtimer.feature.ui.medicine.NewReminderData
import com.futsch1.medtimer.feature.ui.medicine.NewReminderExpirationFrequency
import com.futsch1.medtimer.feature.ui.medicine.NewReminderStockFrequency
import com.futsch1.medtimer.feature.ui.medicine.showMaterialTimePicker
import com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences.EditIntervalDialog
import java.time.Instant
import java.time.LocalDate
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
    medicineId: Int,
    medicineAmount: Double,
    medicineUnit: String,
    onCreate: (NewReminderData) -> Unit,
    onDismiss: () -> Unit,
    onOpenHelp: () -> Unit,
    initialType: ReminderType? = null,
) {
    var selectedType by rememberSaveable(initialType) { mutableStateOf(initialType) }
    var amount by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("08:00") }
    var interval by rememberSaveable { mutableStateOf("12") }
    var intervalUnit by rememberSaveable { mutableStateOf(IntervalUnit.HOURS) }
    var intervalStartsFromProcessed by rememberSaveable { mutableStateOf(false) }
    var startDateTime by rememberSaveable { mutableStateOf(LocalDate.now().toString() + " 08:00") }
    var dailyStart by rememberSaveable { mutableStateOf("08:00") }
    var dailyEnd by rememberSaveable { mutableStateOf("23:00") }
    var threshold by rememberSaveable(medicineId) { mutableStateOf(MedicineHelper.formatAmount(medicineAmount, "")) }
    var stockRule by rememberSaveable { mutableStateOf(NewReminderStockFrequency.ONCE) }
    var expirationDays by rememberSaveable { mutableStateOf("0") }
    var expirationRule by rememberSaveable { mutableStateOf(NewReminderExpirationFrequency.ONCE) }
    var invalid by rememberSaveable { mutableStateOf(false) }

    val type = selectedType
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
                    ReminderTypeChooser(onSelect = { selectedType = it })
                } else {
                    ReminderDetailsFields(
                        type = type,
                        medicineUnit = medicineUnit,
                        amount = amount,
                        onAmountChange = { amount = it },
                        time = time,
                        onTimeChange = { time = it },
                        interval = interval,
                        onIntervalChange = { interval = it },
                        intervalUnit = intervalUnit,
                        onIntervalUnitChange = { intervalUnit = it },
                        intervalStartsFromProcessed = intervalStartsFromProcessed,
                        onIntervalStartsFromProcessedChange = { intervalStartsFromProcessed = it },
                        startDateTime = startDateTime,
                        onStartDateTimeChange = { startDateTime = it },
                        dailyStart = dailyStart,
                        onDailyStartChange = { dailyStart = it },
                        dailyEnd = dailyEnd,
                        onDailyEndChange = { dailyEnd = it },
                        threshold = threshold,
                        onThresholdChange = { threshold = it },
                        stockRule = stockRule,
                        onStockRuleChange = { stockRule = it },
                        expirationDays = expirationDays,
                        onExpirationDaysChange = { expirationDays = it },
                        expirationRule = expirationRule,
                        onExpirationRuleChange = { expirationRule = it },
                        invalid = invalid,
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
                TextButton(onClick = {
                    val result = createReminderData(
                        type = type,
                        medicineId = medicineId,
                        amount = amount,
                        time = time,
                        interval = interval,
                        intervalUnit = intervalUnit,
                        intervalStartsFromProcessed = intervalStartsFromProcessed,
                        startDateTime = startDateTime,
                        dailyStart = dailyStart,
                        dailyEnd = dailyEnd,
                        threshold = threshold,
                        stockRule = stockRule,
                        expirationDays = expirationDays,
                        expirationRule = expirationRule,
                    )
                    if (result != null) onCreate(result) else invalid = true
                }) { Text(stringResource(CoreUiR.string.create_reminder)) }
            }
        },
        dismissButton = {
            TextButton(
                onClick = if (type == null || initialType != null) onDismiss else ({ selectedType = null; invalid = false }),
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
    amount: String,
    onAmountChange: (String) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    interval: String,
    onIntervalChange: (String) -> Unit,
    intervalUnit: IntervalUnit,
    onIntervalUnitChange: (IntervalUnit) -> Unit,
    intervalStartsFromProcessed: Boolean,
    onIntervalStartsFromProcessedChange: (Boolean) -> Unit,
    startDateTime: String,
    onStartDateTimeChange: (String) -> Unit,
    dailyStart: String,
    onDailyStartChange: (String) -> Unit,
    dailyEnd: String,
    onDailyEndChange: (String) -> Unit,
    threshold: String,
    onThresholdChange: (String) -> Unit,
    stockRule: NewReminderStockFrequency,
    onStockRuleChange: (NewReminderStockFrequency) -> Unit,
    expirationDays: String,
    onExpirationDaysChange: (String) -> Unit,
    expirationRule: NewReminderExpirationFrequency,
    onExpirationRuleChange: (NewReminderExpirationFrequency) -> Unit,
    invalid: Boolean,
) {
    when (type) {
        ReminderType.OUT_OF_STOCK -> StockReminderFields(
            medicineUnit, threshold, onThresholdChange, stockRule, onStockRuleChange, time, onTimeChange,
        )

        ReminderType.EXPIRATION_DATE -> ExpirationReminderFields(
            expirationDays, onExpirationDaysChange, expirationRule, onExpirationRuleChange, time, onTimeChange,
        )

        else -> ScheduledReminderFields(
            type, amount, onAmountChange, time, onTimeChange, interval, onIntervalChange,
            intervalUnit, onIntervalUnitChange, intervalStartsFromProcessed,
            onIntervalStartsFromProcessedChange, startDateTime, onStartDateTimeChange,
            dailyStart, onDailyStartChange, dailyEnd, onDailyEndChange,
        )
    }
    if (invalid) Text(stringResource(CoreUiR.string.invalid_input), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun StockReminderFields(
    medicineUnit: String,
    threshold: String,
    onThresholdChange: (String) -> Unit,
    rule: NewReminderStockFrequency,
    onRuleChange: (NewReminderStockFrequency) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
) {
    EditTextButton(
        label = stringResource(CoreUiR.string.reminder_threshold),
        value = "$threshold $medicineUnit",
        onConfirm = onThresholdChange,
        modifier = Modifier.fillMaxWidth(),
    )
    RadioChoiceGroup {
        RadioChoice(stringResource(CoreUiR.string.once_below_threshold), rule == NewReminderStockFrequency.ONCE) {
            onRuleChange(NewReminderStockFrequency.ONCE)
        }
        RadioChoice(stringResource(CoreUiR.string.always_below_threshold), rule == NewReminderStockFrequency.ALWAYS) {
            onRuleChange(NewReminderStockFrequency.ALWAYS)
        }
        RadioChoice(stringResource(CoreUiR.string.daily_below_threshold), rule == NewReminderStockFrequency.DAILY) {
            onRuleChange(NewReminderStockFrequency.DAILY)
        }
    }
    if (rule == NewReminderStockFrequency.DAILY) TimeField(CoreUiR.string.time, time, onTimeChange)
}

@Composable
private fun ExpirationReminderFields(
    days: String,
    onDaysChange: (String) -> Unit,
    rule: NewReminderExpirationFrequency,
    onRuleChange: (NewReminderExpirationFrequency) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
) {
    EditTextButton(
        label = stringResource(CoreUiR.string.remind_days_before_expiration),
        value = "$days ${stringResource(CoreUiR.string.days_string)}",
        onConfirm = onDaysChange,
        modifier = Modifier.fillMaxWidth(),
    )
    RadioChoiceGroup {
        RadioChoice(stringResource(CoreUiR.string.once), rule == NewReminderExpirationFrequency.ONCE) {
            onRuleChange(NewReminderExpirationFrequency.ONCE)
        }
        RadioChoice(stringResource(CoreUiR.string.daily), rule == NewReminderExpirationFrequency.DAILY) {
            onRuleChange(NewReminderExpirationFrequency.DAILY)
        }
    }
    TimeField(CoreUiR.string.time, time, onTimeChange)
}

@Composable
private fun ScheduledReminderFields(
    type: ReminderType,
    amount: String,
    onAmountChange: (String) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    interval: String,
    onIntervalChange: (String) -> Unit,
    intervalUnit: IntervalUnit,
    onIntervalUnitChange: (IntervalUnit) -> Unit,
    startsFromProcessed: Boolean,
    onStartsFromProcessedChange: (Boolean) -> Unit,
    startDateTime: String,
    onStartDateTimeChange: (String) -> Unit,
    dailyStart: String,
    onDailyStartChange: (String) -> Unit,
    dailyEnd: String,
    onDailyEndChange: (String) -> Unit,
) {
    EditTextButton(
        label = stringResource(CoreUiR.string.dosage),
        value = amount,
        onConfirm = onAmountChange,
        modifier = Modifier.fillMaxWidth(),
    )
    when (type) {
        ReminderType.TIME_BASED -> TimeField(CoreUiR.string.time, time, onTimeChange)
        ReminderType.CONTINUOUS_INTERVAL, ReminderType.WINDOWED_INTERVAL -> {
            val context = LocalContext.current
            val maxIntervalMinutes =
                if (type == ReminderType.WINDOWED_INTERVAL) 24 * 60 else Interval.MAX_INTERVAL_MINUTES
            val intervalMinutes = interval.toIntOrNull()?.let { Interval(it, intervalUnit).minutesValue } ?: (12 * 60)
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
                        onIntervalChange(updatedInterval.getValue().toString())
                        onIntervalUnitChange(updatedInterval.getUnit())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (type == ReminderType.CONTINUOUS_INTERVAL) {
                EditTextButton(
                    label = stringResource(CoreUiR.string.interval_start_time),
                    value = startDateTime,
                    onConfirm = onStartDateTimeChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                RadioChoiceGroup {
                    RadioChoice(
                        stringResource(CoreUiR.string.interval_start_reminded),
                        !startsFromProcessed
                    ) { onStartsFromProcessedChange(false) }
                    RadioChoice(
                        stringResource(CoreUiR.string.interval_start_processed),
                        startsFromProcessed
                    ) { onStartsFromProcessedChange(true) }
                }
            } else {
                TimeField(CoreUiR.string.daily_start_time, dailyStart, onDailyStartChange)
                TimeField(CoreUiR.string.daily_end_time, dailyEnd, onDailyEndChange)
            }
        }

        else -> Unit
    }
}

@SuppressWarnings("kotlin:S107")
private fun createReminderData(
    type: ReminderType,
    medicineId: Int,
    amount: String,
    time: String,
    interval: String,
    intervalUnit: IntervalUnit,
    intervalStartsFromProcessed: Boolean,
    startDateTime: String,
    dailyStart: String,
    dailyEnd: String,
    threshold: String,
    stockRule: NewReminderStockFrequency,
    expirationDays: String,
    expirationRule: NewReminderExpirationFrequency,
): NewReminderData? = runCatching {
    val timeMinutes = parseTime(time)
    when (type) {
        ReminderType.TIME_BASED -> NewReminderData(medicineId, type, amount.trim(), timeMinutes)
        ReminderType.CONTINUOUS_INTERVAL -> NewReminderData(
            medicineId = medicineId,
            type = type,
            amount = amount.trim(),
            timeMinutes = timeMinutes,
            intervalMinutes = intervalMinutes(interval, intervalUnit, Interval.MAX_INTERVAL_MINUTES),
            intervalStart = LocalDateTimeParser.parse(startDateTime),
            intervalStartsFromProcessed = intervalStartsFromProcessed,
        )

        ReminderType.WINDOWED_INTERVAL -> NewReminderData(
            medicineId = medicineId,
            type = type,
            amount = amount.trim(),
            timeMinutes = timeMinutes,
            intervalMinutes = intervalMinutes(interval, intervalUnit, 24 * 60),
            dailyStartTime = LocalTime.parse(dailyStart),
            dailyEndTime = LocalTime.parse(dailyEnd),
        )

        ReminderType.OUT_OF_STOCK -> NewReminderData(
            medicineId = medicineId,
            type = type,
            amount = "",
            timeMinutes = timeMinutes,
            stockThreshold = MedicineHelper.parseAmount(threshold) ?: error("threshold"),
            stockFrequency = stockRule,
        )

        ReminderType.EXPIRATION_DATE -> NewReminderData(
            medicineId = medicineId,
            type = type,
            amount = "",
            timeMinutes = timeMinutes,
            expirationDaysBefore = expirationDays.toLong(),
            expirationFrequency = expirationRule,
        )

        else -> error("Unsupported reminder type")
    }
}.getOrNull()

@Composable
private fun TimeField(label: Int, value: String, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    val time = runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.of(8, 0))
    EditValueButton(
        label = stringResource(label),
        value = value,
        onClick = {
            showMaterialTimePicker(context, time) { selectedTime ->
                onValueChange(
                    "${selectedTime.hour.toString().padStart(2, '0')}:${
                        selectedTime.minute.toString().padStart(2, '0')
                    }"
                )
            }
        },
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

private fun parseTime(value: String): Int = LocalTime.parse(value).let { it.hour * 60 + it.minute }

private fun intervalMinutes(value: String, unit: IntervalUnit, maxMinutes: Int): Int {
    val amount = value.toInt().coerceAtLeast(1)
    return Interval(amount, unit).minutesValue.coerceAtMost(maxMinutes)
}

private object LocalDateTimeParser {
    fun parse(value: String): Instant = java.time.LocalDateTime.parse(value.replace(' ', 'T'))
        .atZone(java.time.ZoneId.systemDefault()).toInstant()
}

@MedTimerPreview
@Composable
private fun NewReminderFlowDialogPreview() {
    MedTimerTheme {
        NewReminderFlowDialog(
            medicineId = 1,
            medicineAmount = 1.0,
            medicineUnit = "tablet",
            onCreate = {},
            onDismiss = {},
            onOpenHelp = {},
        )
    }
}

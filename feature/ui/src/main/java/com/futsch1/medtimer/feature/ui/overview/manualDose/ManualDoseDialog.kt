package com.futsch1.medtimer.feature.ui.overview.manualDose

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futsch1.medtimer.core.common.helpers.LocaleContextAccessor
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.ui.rememberMedicineIcon
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.component.MedicineSwatch
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.helpers.TextInputDialogBuilder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ManualDoseDialog(
    viewModel: ManualDoseViewModel,
    onSelectMedicineEntry: (ManualDoseMedicineEntry) -> Unit,
    onSelectAmount: (String) -> Unit,
    onLogManualDose: (Instant) -> Unit,
    onStepBack: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = ManualDoseState())

    Dialog(
        onDismissRequest = onDismissDialog,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        DialogContent(
            state = state,
            onSelectMedicineEntry = onSelectMedicineEntry,
            onSelectAmount = onSelectAmount,
            onLogManualDose = onLogManualDose,
            onStepBack = onStepBack,
            onDismissDialog = onDismissDialog,
        )
    }
}

@Composable
private fun DialogContent(
    state: ManualDoseState,
    onSelectMedicineEntry: (ManualDoseMedicineEntry) -> Unit,
    onSelectAmount: (String) -> Unit,
    onLogManualDose: (Instant) -> Unit,
    onStepBack: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .widthIn(max = 520.dp)
            .heightIn(max = 680.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)) {
            Header(
                state = state,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state.step) {
                    ManualDoseStep.MEDICINE -> {
                        SectionLabel(R.string.medicine)
                        MedicineList(
                            medicineEntries = state.medicineEntries,
                            modifier = Modifier.weight(1f, fill = false),
                            onSelectMedicineEntry = onSelectMedicineEntry,
                        )
                        RecentChoice(
                            label = state.lastCustomMedicineEntry.label,
                            onClick = { onSelectMedicineEntry(state.lastCustomMedicineEntry) },
                        )
                        CustomButton(R.string.medicine_name, Modifier.fillMaxWidth()) { name ->
                            onSelectMedicineEntry(ManualDoseMedicineEntry(name, null))
                        }
                    }

                    ManualDoseStep.AMOUNT -> {
                        SectionLabel(R.string.amount)
                        AmountList(
                            amounts = state.amounts,
                            modifier = Modifier.weight(1f, fill = false),
                            onSelectAmount = onSelectAmount,
                        )
                            RecentChoice(
                                label = state.lastCustomDoseAmount,
                                onClick = { onSelectAmount(state.lastCustomDoseAmount) },
                            )
                        CustomButton(R.string.amount, Modifier.fillMaxWidth(), onSelectAmount)
                    }

                    ManualDoseStep.TIME -> {
                        Text(
                            text = stringResource(R.string.time),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        SelectedDoseSummary(state)
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onLogManualDose(Instant.now())
                                onDismissDialog()
                            },
                        ) {
                            Text(stringResource(R.string.take_now))
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val pickerFactory = TimePickerDialogFactory(LocaleContextAccessor(context))
                                pickerFactory.create(LocalTime.now()) { minutes ->
                                    // TODO: Pass the selected date from the ViewModel.
                                    val instant = TimeHelper.instantFromDateAndMinutes(minutes, LocalDate.now())
                                    onLogManualDose(instant)
                                    onDismissDialog()
                                }
                            },
                        ) {
                            Text(stringResource(R.string.enter_time))
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismissDialog) {
                    Text(stringResource(R.string.cancel))
                }
                if (state.step != ManualDoseStep.MEDICINE) {
                    OutlinedButton(onClick = onStepBack) {
                        Text(stringResource(R.string.back))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: ManualDoseState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.log_additional_dose),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ManualDoseStep.entries.forEachIndexed { index, step ->
                val current = step == state.step
                val complete = index < state.step.ordinal
                val label = when (step) {
                    ManualDoseStep.MEDICINE -> stringResource(R.string.medicine)
                    ManualDoseStep.AMOUNT -> stringResource(R.string.amount)
                    ManualDoseStep.TIME -> stringResource(R.string.time)
                }
                HeaderStepItem(Modifier.weight(1f), label, current, complete, index)
            }
        }
        if (state.step == ManualDoseStep.AMOUNT) {
            Text(
                text = state.selectedMedicine?.label.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeaderStepItem(modifier: Modifier, label: String, current: Boolean, complete: Boolean, index: Int) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = when {
            current -> MaterialTheme.colorScheme.primaryContainer
            complete -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HeaderIcon(complete, current, index)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (current) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun HeaderIcon(complete: Boolean, current: Boolean, index: Int) {
    if (complete) {
        Icon(
            painter = painterResource(R.drawable.check2_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp),
        )
    } else {
        Surface(
            shape = CircleShape,
            color = if (current) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
            modifier = Modifier.size(20.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (current) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectedDoseSummary(state: ManualDoseState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = state.selectedMedicine?.label.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            state.selectedAmount?.takeIf(String::isNotBlank)?.let { amount ->
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(@StringRes label: Int) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun RecentChoice(label: String, onClick: () -> Unit) {
    if (label.isNotBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.recent_custom),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
fun CustomButton(
    @StringRes text: Int,
    modifier: Modifier = Modifier,
    onEntered: (String) -> Unit,
) {
    val context = LocalContext.current
    OutlinedButton(
        modifier = modifier,
        onClick = {
            TextInputDialogBuilder(context).title(R.string.log_additional_dose)
                .hint(text)
                .textSink(onEntered::invoke)
                .show()
        },
    ) {
        Text("+  ${stringResource(R.string.custom)}")
    }
}

@Composable
fun MedicineList(
    medicineEntries: ImmutableList<ManualDoseMedicineEntry>,
    modifier: Modifier = Modifier,
    onSelectMedicineEntry: (ManualDoseMedicineEntry) -> Unit,
) {
    LazyColumn(
        modifier = modifier.heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(medicineEntries, key = { entry -> entry.medicine?.id ?: "custom" }) { entry ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectMedicineEntry(entry) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    entry.medicine?.let { medicine ->
                        MedicineSwatch(
                            icon = rememberMedicineIcon(medicine.iconId),
                            color = medicine.color.takeIf { medicine.useColor },
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                    Text(
                        text = entry.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun AmountList(
    amounts: ImmutableList<String>,
    modifier: Modifier = Modifier,
    onSelectAmount: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(amounts, key = { it }) { amount ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectAmount(amount) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(amount, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

fun previewState(step: ManualDoseStep): ManualDoseState = ManualDoseState(
    medicineEntries = listOf(
        ManualDoseMedicineEntry("Vitamin X 500 mg", Medicine.default().copy(id = 1)),
        ManualDoseMedicineEntry("Medicine A", Medicine.default().copy(id = 2)),
        ManualDoseMedicineEntry("Medicine B", Medicine.default().copy(id = 3)),
    ).toImmutableList(),
    lastCustomMedicineEntry = ManualDoseMedicineEntry("Medicine C", null),
    amounts = listOf("1 tablet", "2 tablets").toImmutableList(),
    lastCustomDoseAmount = "5 ml",
    selectedMedicine = ManualDoseMedicineEntry("Vitamin X 500 mg", Medicine.default().copy(id = 1)),
    selectedAmount = "1 tablet",
    step = step,
)

@MedTimerPreview
@Composable
fun ManualDosePreviewMedicine() {
    ManualDosePreview(ManualDoseStep.MEDICINE)
}

@MedTimerPreview
@Composable
fun ManualDosePreviewAmount() {
    ManualDosePreview(ManualDoseStep.AMOUNT)
}

@MedTimerPreview
@Composable
fun ManualDosePreviewTime() {
    ManualDosePreview(ManualDoseStep.TIME)
}

@Composable
private fun ManualDosePreview(step: ManualDoseStep) {
    MedTimerTheme {
        Surface {
            DialogContent(
                state = previewState(step),
                onSelectMedicineEntry = {},
                onSelectAmount = {},
                onLogManualDose = {},
                onStepBack = {},
                onDismissDialog = {},
            )
        }
    }
}

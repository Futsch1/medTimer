package com.futsch1.medtimer.feature.ui.overview.manualDose

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futsch1.medtimer.core.common.helpers.LocaleContextAccessor
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.ui.R
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
    onStepBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = ManualDoseState())

    Dialog(
        onDismissRequest = {},
        DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        ManualDoseDialogContent(
            state,
            onSelectMedicineEntry,
            onSelectAmount,
            onLogManualDose,
            onStepBack
        )
    }
}

@Composable
fun ManualDoseDialogContent(
    state: ManualDoseState,
    onSelectMedicineEntry: (ManualDoseMedicineEntry) -> Unit,
    onSelectAmount: (String) -> Unit,
    onLogManualDose: (Instant) -> Unit,
    onStepBack: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column {
            ManualDoseHeader(state)
            when (state.step) {
                ManualDoseStep.MEDICINE -> {
                    ManualDoseMedicineList(state.medicineEntries, onSelectMedicineEntry)
                    CustomButton(R.string.medicine_name) {
                        onSelectMedicineEntry(
                            ManualDoseMedicineEntry(
                                it,
                                null
                            )
                        )
                    }
                }

                ManualDoseStep.AMOUNT -> {
                    ManualDoseAmountList(state.amounts, onSelectAmount)
                    CustomButton(R.string.amount, onSelectAmount)
                }

                ManualDoseStep.TIME -> {
                    Button(modifier = Modifier.padding(4.dp), onClick = {
                        onLogManualDose(Instant.now())
                    }) {
                        Text(stringResource(R.string.take_now))
                    }
                    Button(modifier = Modifier.padding(4.dp), onClick = {
                        val localDateTime = LocalTime.now()
                        val timePickerDialogFactory = TimePickerDialogFactory(LocaleContextAccessor(context))
                        timePickerDialogFactory.create(localDateTime) { minutes: Int ->
                            // TODO: Date is passed on creation via view model
                            val remindedInstant: Instant =
                                TimeHelper.instantFromDateAndMinutes(minutes, LocalDate.now())
                            onLogManualDose(remindedInstant)
                        }
                    }) {
                        Text(stringResource(R.string.enter_time))
                    }
                }
            }
            Button(modifier = Modifier.padding(4.dp), onClick = {
                onStepBack()
            }) {
                Text(stringResource(R.string.back))
            }
        }
    }
}

@Composable
fun ManualDoseHeader(state: ManualDoseState) {
    Column {
        Text(stringResource(R.string.log_additional_dose), style = MaterialTheme.typography.titleLarge)

        Row {
            if (state.step != ManualDoseStep.MEDICINE) {
                Icon(
                    painterResource(R.drawable.check2_circle),
                    contentDescription = null
                )
            }
            Text(stringResource(R.string.medicine))
            if (state.step == ManualDoseStep.TIME) {
                Icon(
                    painterResource(R.drawable.check2_circle),
                    contentDescription = null
                )
            }
            Text(stringResource(R.string.amount))
            Text(stringResource(R.string.time))
        }
        Row {
            if (state.step != ManualDoseStep.MEDICINE) {
                Text(state.selectedMedicine?.label ?: "")
            }
            if (state.step == ManualDoseStep.TIME) {
                Text(state.selectedAmount ?: "")
            }
        }
    }
}

@Composable
fun CustomButton(
    @StringRes text: Int,
    onEntered: (String) -> Unit
) {
    val context = LocalContext.current

    Button(modifier = Modifier.padding(4.dp), onClick = {
        TextInputDialogBuilder(context).title(R.string.log_additional_dose)
            .hint(text)
            .textSink { name: String ->
                onEntered(name)
            }.show()
    }) {
        Text(stringResource(R.string.custom))
    }
}

@Composable
fun ManualDoseMedicineList(
    medicineEntries: ImmutableList<ManualDoseMedicineEntry>,
    onSelectMedicineEntry: (ManualDoseMedicineEntry) -> Unit
) {
    LazyColumn {
        items(medicineEntries, key = { entry -> entry.medicine?.id ?: "custom" }) { entry ->
            Row(
                modifier = Modifier
                    .clickable { onSelectMedicineEntry(entry) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Text(entry.label)
            }
        }
    }
}

@Composable
fun ManualDoseAmountList(
    amounts: ImmutableList<String>,
    onSelectAmount: (String) -> Unit
) {
    LazyColumn {
        items(amounts) { entry ->
            Row(
                modifier = Modifier
                    .clickable { onSelectAmount(entry) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Text(entry)
            }
        }
    }
}

fun previewState(step: ManualDoseStep): ManualDoseState {
    return ManualDoseState(
        medicineEntries = listOf(
            ManualDoseMedicineEntry("First medicine", Medicine.default()),
            ManualDoseMedicineEntry("Second medicine", Medicine.default().copy(id = 2))
        ).toImmutableList(),
        amounts = listOf("1 tablet", "2 tablets").toImmutableList(),
        selectedMedicine = ManualDoseMedicineEntry("First medicine", Medicine.default()),
        selectedAmount = "1 tablet",
        step = step
    )
}


@MedTimerPreview
@Composable
fun ManualDosePreviewMedicine() {
    MedTimerTheme {
        Surface {
            ManualDoseDialogContent(
                previewState(ManualDoseStep.MEDICINE),
                onSelectMedicineEntry = {},
                onSelectAmount = {},
                onLogManualDose = {},
                onStepBack = {}
            )
        }
    }
}


@MedTimerPreview
@Composable
fun ManualDosePreviewAmount() {
    MedTimerTheme {
        Surface {
            ManualDoseDialogContent(
                previewState(ManualDoseStep.AMOUNT),
                onSelectMedicineEntry = {},
                onSelectAmount = {},
                onLogManualDose = {},
                onStepBack = {}
            )
        }
    }
}


@MedTimerPreview
@Composable
fun ManualDosePreviewTime() {
    MedTimerTheme {
        Surface {
            ManualDoseDialogContent(
                previewState(ManualDoseStep.TIME),
                onSelectMedicineEntry = {},
                onSelectAmount = {},
                onLogManualDose = {},
                onStepBack = {}
            )
        }
    }
}

package com.futsch1.medtimer.feature.ui.overview.manualDose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.helpers.TextInputDialogBuilder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant

@Composable
fun ManualDoseDialog(
    viewModel: ManualDoseViewModel,
    onSelectMedicineEntry: (ManualDoseMedicineEntry) -> Unit,
    onSelectAmount: (String) -> Unit,
    onLogManualDose: (Instant) -> Unit
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
            onLogManualDose
        )
    }
}


@Composable
fun ManualDoseDialogContent(
    state: ManualDoseState,
    onSelectMedicineEntry: (ManualDoseMedicineEntry) -> Unit,
    onSelectAmount: (String) -> Unit,
    onLogManualDose: (Instant) -> Unit
) {
    val context = LocalContext.current
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row {
            Column {
                Button(modifier = Modifier.padding(4.dp), onClick = {
                    TextInputDialogBuilder(context).title(com.futsch1.medtimer.core.ui.R.string.log_additional_dose)
                        .hint(R.string.medicine_name)
                        .textSink { name: String ->
                            onSelectMedicineEntry(ManualDoseMedicineEntry(name, null))
                        }.show()
                }) {
                    Text(stringResource(R.string.custom))
                }
                ManualDoseMedicineList(state.medicineEntries, onSelectMedicineEntry)
            }

            if (state.selectedMedicine != null) {
                Column {
                    Button(modifier = Modifier.padding(4.dp), onClick = {
                        TextInputDialogBuilder(context).title(com.futsch1.medtimer.core.ui.R.string.log_additional_dose)
                            .hint(R.string.amount)
                            .textSink { amount: String ->
                                onSelectAmount(amount)
                            }.show()
                    }) {
                        Text(stringResource(R.string.custom))
                    }
                    ManualDoseAmountList(state.amounts) { entry ->
                        onSelectAmount(entry)
                    }
                }
            }
        }
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


@MedTimerPreview
@Composable
fun ManualDosePreview() {
    val state = ManualDoseState(
        medicineEntries = listOf(
            ManualDoseMedicineEntry("First medicine", Medicine.default()),
            ManualDoseMedicineEntry("Second medicine", Medicine.default().copy(id = 2))
        ).toImmutableList(),
        amounts = listOf("1 tablet", "2 tablets").toImmutableList(),
        selectedMedicine = ManualDoseMedicineEntry("First medicine", Medicine.default()),
        selectedAmount = "1 tablet"
    )

    MedTimerTheme {
        Surface {
            ManualDoseDialogContent(
                state,
                onSelectMedicineEntry = {},
                onSelectAmount = {},
                onLogManualDose = {}
            )
        }
    }
}

package com.futsch1.medtimer.feature.ui.overview.manualDose

import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futsch1.medtimer.core.common.helpers.LocaleContextAccessor
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.component.MedicineSwatch
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.rememberMedicineIcon
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalTime

private tailrec fun Context.findFragmentActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> error("Manual dose time picker requires a FragmentActivity host")
}

object ManualDoseTestTags {
    const val DIALOG = "manual_dose_dialog"
    const val MEDICINE_SEARCH = "manual_dose_medicine_search"
    const val MEDICINE_LIST = "manual_dose_medicine_list"
    const val MEDICINE_ENTRY = "manual_dose_medicine_entry"
    const val AMOUNT_LIST = "manual_dose_amount_list"
    const val AMOUNT_ENTRY = "manual_dose_amount_entry"
    const val RECENT_DOSE = "manual_dose_recent_dose"
    const val CUSTOM_BUTTON = "manual_dose_custom_button"
    const val CUSTOM_INPUT = "manual_dose_custom_input"
    const val CUSTOM_CONFIRM = "manual_dose_custom_confirm"
    const val CUSTOM_CANCEL = "manual_dose_custom_cancel"
    const val CUSTOM_ERROR = "manual_dose_custom_error"
    const val SELECTED_DOSE_SUMMARY = "manual_dose_selected_summary"
    const val ENTER_TIME = "manual_dose_enter_time"
    const val CANCEL = "manual_dose_cancel"
    const val BACK = "manual_dose_back"
}

@Composable
fun ManualDoseDialog(
    viewModel: ManualDoseViewModel,
    onSearch: (String) -> Unit,
    onSelectMedicineEntry: (ManualDoseMedicineEntry) -> Unit,
    onSelectAmount: (String) -> Unit,
    onLogManualDose: (LocalTime?) -> Unit,
    onLogRecentDose: (ManualDoseMedicineWithAmountEntry) -> Unit,
    onStepBack: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = ManualDoseState())

    Dialog(
        onDismissRequest = onDismissDialog,
        properties = DialogProperties(
            dismissOnBackPress = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        DialogContent(
            state = state,
            searchQuery = viewModel.searchQuery,
            onSearch = onSearch,
            onSelectMedicineEntry = onSelectMedicineEntry,
            onSelectAmount = onSelectAmount,
            onLogManualDose = onLogManualDose,
            onLogRecentDose = onLogRecentDose,
            onStepBack = onStepBack,
            onDismissDialog = onDismissDialog,
        )
    }
}

@Composable
private fun DialogContent(
    state: ManualDoseState,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onSelectMedicineEntry: (ManualDoseMedicineEntry) -> Unit,
    onSelectAmount: (String) -> Unit,
    onLogManualDose: (LocalTime?) -> Unit,
    onLogRecentDose: (ManualDoseMedicineWithAmountEntry) -> Unit,
    onStepBack: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    BackHandler {
        if (state.step == ManualDoseStep.MEDICINE) onDismissDialog() else onStepBack()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .widthIn(max = 520.dp)
            .heightIn(max = 680.dp)
            .testTag(ManualDoseTestTags.DIALOG),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)) {
            Header(
                state = state,
                modifier = Modifier.padding(horizontal = 24.dp),
                onStepBack = onStepBack
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .then(
                        if (state.step == ManualDoseStep.MEDICINE) {
                            Modifier.verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DoseStepContent(
                    state = state,
                    searchQuery = searchQuery,
                    onSearch = onSearch,
                    onSelectMedicineEntry = onSelectMedicineEntry,
                    onSelectAmount = onSelectAmount,
                    onLogManualDose = onLogManualDose,
                    onLogRecentDose = onLogRecentDose,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismissDialog,
                    modifier = Modifier.testTag(ManualDoseTestTags.CANCEL),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                if (state.step != ManualDoseStep.MEDICINE) {
                    OutlinedButton(
                        onClick = onStepBack,
                        modifier = Modifier.testTag(ManualDoseTestTags.BACK),
                    ) {
                        Text(stringResource(R.string.back))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.DoseStepContent(
    state: ManualDoseState,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onSelectMedicineEntry: (ManualDoseMedicineEntry) -> Unit,
    onSelectAmount: (String) -> Unit,
    onLogManualDose: (LocalTime?) -> Unit,
    onLogRecentDose: (ManualDoseMedicineWithAmountEntry) -> Unit,
) {
    val context = LocalContext.current
    when (state.step) {
        ManualDoseStep.MEDICINE -> {
            Column {
                if (state.lastCustomDoses.isNotEmpty()) {
                    RecentDoses(doses = state.lastCustomDoses, onLogRecentDose = onLogRecentDose)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                SectionLabel(R.string.choose_medicine)
                if (state.showSearch) MedicineSearchField(searchQuery, onSearch)
                CustomButton(
                    R.string.medicine_name,
                    Modifier.fillMaxWidth(),
                    requireNonBlank = true,
                ) { name -> onSelectMedicineEntry(ManualDoseMedicineEntry(name, null)) }
                MedicineList(
                    medicineEntries = state.medicineEntries,
                    modifier = Modifier.heightIn(max = 300.dp),
                    onSelectMedicineEntry = onSelectMedicineEntry,
                )
            }
        }

        ManualDoseStep.AMOUNT -> {
            SectionLabel(R.string.amount)
            OutlinedButton(onClick = { onSelectAmount("") }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.no_amount))
            }
            CustomButton(R.string.amount, Modifier.fillMaxWidth(), onEntered = onSelectAmount)
            AmountList(
                amounts = state.amounts,
                modifier = Modifier.weight(1f, fill = false),
                onSelectAmount = onSelectAmount,
            )
        }

        ManualDoseStep.TIME -> {
            Text(
                text = stringResource(R.string.time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SelectedDoseSummary(state)
            Button(modifier = Modifier.fillMaxWidth(), onClick = { onLogManualDose(null) }) {
                Text(stringResource(R.string.log_at_current_time))
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().testTag(ManualDoseTestTags.ENTER_TIME),
                onClick = {
                    val pickerFactory = TimePickerDialogFactory(LocaleContextAccessor(context))
                    pickerFactory.create(LocalTime.now()) { minutes ->
                        onLogManualDose(LocalTime.ofSecondOfDay(minutes * 60L))
                    }.show(
                        context.findFragmentActivity().supportFragmentManager,
                        TimePickerDialogFactory.DIALOG_TAG,
                    )
                },
            ) {
                Text(stringResource(R.string.log_at_another_time))
            }
        }
    }
}

@Composable
private fun Header(state: ManualDoseState, modifier: Modifier = Modifier, onStepBack: () -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.log_additional_dose),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "${stringResource(R.string.time)}: ${state.selectedDateTime.orEmpty()}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GuidedSteps(state, onStepBack)
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
private fun headerStepLabel(step: ManualDoseStep): String = when (step) {
    ManualDoseStep.MEDICINE -> stringResource(R.string.medicine)
    ManualDoseStep.AMOUNT -> stringResource(R.string.amount)
    ManualDoseStep.TIME -> stringResource(R.string.time)
}

@Composable
private fun GuidedSteps(state: ManualDoseState, onStepBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom=4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ManualDoseStep.entries.forEachIndexed { index, step ->
            val complete = index < state.step.ordinal
            HeaderStepItem(
                modifier = Modifier.weight(1f),
                label = headerStepLabel(step),
                current = step == state.step,
                complete = complete,
                index = index,
                enabled = complete,
                onClick = headerStepClick(step, state.step, onStepBack),
            )
        }
    }
}

private fun headerStepClick(
    step: ManualDoseStep,
    currentStep: ManualDoseStep,
    onStepBack: () -> Unit,
): () -> Unit = {
    when (step) {
        ManualDoseStep.MEDICINE -> when (currentStep) {
            ManualDoseStep.AMOUNT -> onStepBack()
            ManualDoseStep.TIME -> repeat(2) { onStepBack() }
            ManualDoseStep.MEDICINE -> Unit
        }

        ManualDoseStep.AMOUNT -> if (currentStep == ManualDoseStep.TIME) onStepBack()
        ManualDoseStep.TIME -> Unit
    }
}

@Composable
private fun HeaderStepItem(
    modifier: Modifier,
    label: String,
    current: Boolean,
    complete: Boolean,
    index: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = when {
            current -> MaterialTheme.colorScheme.primaryContainer
            complete -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        onClick = onClick,
        enabled = enabled,
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
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ManualDoseTestTags.SELECTED_DOSE_SUMMARY),
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
private fun RecentDoses(
    doses: ImmutableList<ManualDoseMedicineWithAmountEntry>,
    onLogRecentDose: (ManualDoseMedicineWithAmountEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.quick_log),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        doses.forEach { dose ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ManualDoseTestTags.RECENT_DOSE),
                onClick = { onLogRecentDose(dose) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dose.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    if (dose.amount.isNotBlank()) {
                        Text(
                            text = dose.amount,
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomButton(
    @StringRes text: Int,
    modifier: Modifier = Modifier,
    requireNonBlank: Boolean = false,
    onEntered: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    OutlinedButton(
        modifier = modifier.testTag(ManualDoseTestTags.CUSTOM_BUTTON),
        onClick = { value = ""; showError = false; showDialog = true },
    ) {
        Text(stringResource(R.string.custom))
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.log_additional_dose)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it; showError = false },
                        label = { Text(stringResource(text)) },
                        isError = showError,
                        modifier = Modifier.testTag(ManualDoseTestTags.CUSTOM_INPUT),
                    )
                    if (showError) {
                        Text(
                            text = stringResource(R.string.medicine_name_required),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(ManualDoseTestTags.CUSTOM_ERROR),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(ManualDoseTestTags.CUSTOM_CONFIRM),
                    onClick = {
                        val entered = value.trim()
                        if (requireNonBlank && entered.isEmpty()) {
                            showError = true
                        } else {
                            onEntered(entered)
                            showDialog = false
                        }
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag(ManualDoseTestTags.CUSTOM_CANCEL),
                    onClick = { showDialog = false },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun MedicineSearchField(query: String, onSearch: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onSearch,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ManualDoseTestTags.MEDICINE_SEARCH),
        label = { Text(stringResource(R.string.search)) },
        leadingIcon = { Icon(painterResource(R.drawable.search), contentDescription = null) },
        singleLine = true,
    )
}

@Composable
fun MedicineList(
    medicineEntries: ImmutableList<ManualDoseMedicineEntry>,
    modifier: Modifier = Modifier,
    onSelectMedicineEntry: (ManualDoseMedicineEntry) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .testTag(ManualDoseTestTags.MEDICINE_LIST),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(medicineEntries, key = { entry -> entry.medicine?.id ?: entry.label }) { entry ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ManualDoseTestTags.MEDICINE_ENTRY)
                    .clickable { onSelectMedicineEntry(entry) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
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
                    Icon(
                        painter = painterResource(R.drawable.chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
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
        modifier = modifier
            .heightIn(max = 300.dp)
            .testTag(ManualDoseTestTags.AMOUNT_LIST),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(amounts, key = { it }) { amount ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ManualDoseTestTags.AMOUNT_ENTRY)
                    .clickable { onSelectAmount(amount) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
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
        ManualDoseMedicineEntry("Vitamin X 500 mg", Medicine.default().copy(id = 1, useColor = true)),
        ManualDoseMedicineEntry("Medicine A", Medicine.default().copy(id = 2)),
        ManualDoseMedicineEntry("Medicine B", Medicine.default().copy(id = 3)),
    ).toImmutableList(),
    lastCustomDoses = listOf(ManualDoseMedicineWithAmountEntry("Medicine C", null, "5 mg")).toImmutableList(),
    amounts = listOf("1 tablet", "2 tablets").toImmutableList(),
    selectedMedicine = ManualDoseMedicineEntry("Vitamin X 500 mg", Medicine.default().copy(id = 1)),
    selectedAmount = "1 tablet",
    selectedDateTime = "08/01/2026 11:30 AM",
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
                searchQuery = "",
                onSearch = {},
                onSelectMedicineEntry = {},
                onSelectAmount = {},
                onLogManualDose = {},
                onLogRecentDose = {},
                onStepBack = {},
                onDismissDialog = {},
            )
        }
    }
}

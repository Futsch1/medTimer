package com.futsch1.medtimer.feature.ui.medicine.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ScannedPackagesScreen(
    items: ImmutableList<MedicineLabelItem>,
    onEditQuantity: (text: String, medicineId: Int, quantity: Double) -> Unit = { _, _, _ -> },
    onForget: (text: String) -> Unit = {},
    onForgetAll: () -> Unit = {}
) {
    var showForgetAllDialog by remember { mutableStateOf(false) }

    if (showForgetAllDialog) {
        AlertDialog(
            onDismissRequest = { showForgetAllDialog = false },
            title = { Text(stringResource(R.string.confirm)) },
            text = { Text(stringResource(R.string.scanned_packages_forget_all_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showForgetAllDialog = false
                    onForgetAll()
                }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showForgetAllDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Text(
                text = stringResource(R.string.scanned_packages_help),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.scanned_packages_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    items(items, key = { it.text }) { item ->
                        ScannedPackageCard(item, onEditQuantity, onForget)
                    }
                    item {
                        TextButton(
                            onClick = { showForgetAllDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text(stringResource(R.string.scanned_packages_forget_all))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedPackageCard(
    item: MedicineLabelItem,
    onEditQuantity: (text: String, medicineId: Int, quantity: Double) -> Unit,
    onForget: (text: String) -> Unit
) {
    var showQuantityDialog by remember { mutableStateOf(false) }
    var showForgetDialog by remember { mutableStateOf(false) }

    if (showQuantityDialog) {
        QuantityEditDialog(
            initialQuantity = item.quantity,
            onConfirm = { quantity ->
                showQuantityDialog = false
                onEditQuantity(item.text, item.medicineId, quantity)
            },
            onDismiss = { showQuantityDialog = false }
        )
    }

    if (showForgetDialog) {
        AlertDialog(
            onDismissRequest = { showForgetDialog = false },
            title = { Text(stringResource(R.string.confirm)) },
            text = { Text(stringResource(R.string.scanned_packages_forget_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showForgetDialog = false
                    onForget(item.text)
                }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showForgetDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.medicineName, fontWeight = FontWeight.Bold)
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.quantity?.let {
                        stringResource(R.string.scanned_packages_quantity, it)
                    } ?: stringResource(R.string.scanned_packages_quantity_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = { showQuantityDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.pencil),
                    contentDescription = stringResource(R.string.scanned_packages_edit_quantity)
                )
            }
            IconButton(onClick = { showForgetDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.trash),
                    contentDescription = stringResource(R.string.scanned_packages_forget)
                )
            }
        }
    }
}

@Composable
private fun QuantityEditDialog(
    initialQuantity: Double?,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember {
        mutableStateOf(initialQuantity?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "")
    }
    val quantity = text.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scanned_packages_edit_quantity)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.ocr_ask_quantity)) },
                isError = quantity == null || quantity <= 0
            )
        },
        confirmButton = {
            TextButton(
                enabled = quantity != null && quantity > 0,
                onClick = { quantity?.let(onConfirm) }
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@MedTimerPreview
@Composable
private fun ScannedPackagesScreenPreview() {
    MedTimerTheme {
        ScannedPackagesScreen(
            items = persistentListOf(
                MedicineLabelItem("depakin chrono 500mg tab riv 30cpr", 1, "Depakin Chrono 500mg", 30.0),
                MedicineLabelItem("eutirox 100mg compresse", 2, "Eutirox 100mg", null)
            )
        )
    }
}

package com.futsch1.medtimer.feature.ui.medicine.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.R as CoreUiR
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme

@Composable
fun NotesDialog(notes: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf(notes) }
    AlertDialog(
        modifier = Modifier.testTag(com.futsch1.medtimer.feature.ui.medicine.EditMedicineTestTags.NOTES_DIALOG),
        onDismissRequest = onDismiss,
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(stringResource(CoreUiR.string.notes)) },
                minLines = 15,
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft); onDismiss() }) { Text(stringResource(CoreUiR.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
}

@MedTimerPreview
@Composable
private fun NotesDialogPreview() {
    MedTimerTheme {
        NotesDialog(
            notes = "Take with food.\n\nKeep refrigerated after opening.",
            onSave = {},
            onDismiss = {},
        )
    }
}

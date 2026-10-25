package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.R


@Composable
fun SetupGuidanceCard(
    message: String,
    testTag: String,
    buttonText: String,
    onContinue: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    Card(Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            TextButton(
                modifier = Modifier.testTag(testTag),
                onClick = onContinue
            ) { Text(buttonText) }
            TextButton(onClick = onOpenHelp) { Text(stringResource(R.string.setup_guidance_getting_started)) }
        }
    }
}

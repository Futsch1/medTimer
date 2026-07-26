package com.futsch1.medtimer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.core.ui.R as CoreUiR

/**
 * The shell's dismissible warning cards. [MainViewModel] gates both on release builds, so the
 * previews below are the only way to see them without building a release APK.
 */
@Composable
fun Warnings(
    state: MainScreenState,
    onDismissBatteryWarning: () -> Unit,
    onDismissExactRemindersWarning: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        AnimatedVisibility(
            visible = state.showBatteryOptimizationWarning,
            enter = EnterTransition.None,
            exit = dismissTransition,
        ) {
            WarningCard(
                titleRes = CoreUiR.string.battery_optimization_warning_title,
                summaryRes = CoreUiR.string.battery_optimization_warning_summary,
                onDismiss = onDismissBatteryWarning,
            )
        }
        AnimatedVisibility(
            visible = state.showExactRemindersWarning,
            enter = EnterTransition.None,
            exit = dismissTransition,
        ) {
            WarningCard(
                titleRes = CoreUiR.string.exact_reminders_warning_title,
                summaryRes = CoreUiR.string.exact_reminders_warning_summary,
                onDismiss = onDismissExactRemindersWarning,
            )
        }
    }
}

private const val FADE_MS = 150
private const val COLLAPSE_MS = 200

private val dismissTransition: ExitTransition
    get() = fadeOut(tween(FADE_MS)) +
            shrinkVertically(
                animationSpec = tween(COLLAPSE_MS, delayMillis = FADE_MS),
                shrinkTowards = Alignment.Top,
            )

@Composable
private fun WarningCard(
    titleRes: Int,
    summaryRes: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = stringResource(summaryRes),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = stringResource(CoreUiR.string.ok),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

private class PreviewMainScreenState(
    override val showBatteryOptimizationWarning: Boolean,
    override val showExactRemindersWarning: Boolean,
) : MainScreenState

@Preview(name = "Warnings — both")
@Composable
private fun WarningsBothPreview() {
    MedTimerTheme {
        Warnings(
            state = PreviewMainScreenState(showBatteryOptimizationWarning = true, showExactRemindersWarning = true),
            onDismissBatteryWarning = {},
            onDismissExactRemindersWarning = {},
        )
    }
}

@Preview(name = "Warnings — exact reminders only")
@Composable
private fun WarningsExactRemindersPreview() {
    MedTimerTheme {
        Warnings(
            state = PreviewMainScreenState(showBatteryOptimizationWarning = false, showExactRemindersWarning = true),
            onDismissBatteryWarning = {},
            onDismissExactRemindersWarning = {},
        )
    }
}

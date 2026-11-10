package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.core.ui.time.rememberFormattedTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lines the header text up with the text inside the cards: past the state button column, which the
 * rail runs through, and then the card's own content padding.
 */
private val GROUP_HEADER_START_PADDING = EVENT_STATE_BUTTON_SIZE + EVENT_STATE_BUTTON_MARGIN * 2 + 12.dp

/** Matches a touch target, so the header keeps one height whatever it holds. */
private val GROUP_HEADER_MIN_HEIGHT = 48.dp

/**
 * The time shared by the events of one group of two or more events, shown above the group's card.
 * A one-event group has no header.
 */
@Composable
fun OverviewGroupHeader(
    timestamp: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = GROUP_HEADER_MIN_HEIGHT)
            .padding(start = GROUP_HEADER_START_PADDING),
    ) {
        Text(
            text = rememberFormattedTime(Instant.ofEpochSecond(timestamp)),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(OverviewTestTags.GROUP_TIME),
        )
    }
}

@MedTimerPreview
@Composable
private fun OverviewGroupHeaderPreview() {
    MedTimerTheme {
        Surface {
            OverviewGroupHeader(
                timestamp = LocalDate.of(2026, 5, 28).atTime(9, 0).atZone(ZoneId.systemDefault()).toEpochSecond(),
            )
        }
    }
}

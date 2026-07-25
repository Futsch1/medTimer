package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.core.ui.R as CoreUiR

private val FILTERS = listOf(
    Triple(OverviewFilter.TAKEN, CoreUiR.drawable.check2_circle, CoreUiR.string.taken),
    Triple(OverviewFilter.SKIPPED, CoreUiR.drawable.x_circle, CoreUiR.string.skipped),
    Triple(OverviewFilter.RAISED, CoreUiR.drawable.bell, CoreUiR.string.raised),
    Triple(OverviewFilter.SCHEDULED, CoreUiR.drawable.alarm, CoreUiR.string.scheduled),
)

@Composable
fun OverviewFilterRow(
    activeFilters: Set<OverviewFilter>,
    onToggleFilter: (OverviewFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FILTERS.forEach { (filter, iconRes, labelRes) ->
            val label = stringResource(labelRes)
            FilterChip(
                selected = filter in activeFilters,
                onClick = { onToggleFilter(filter) },
                label = { Text(label) },
                leadingIcon = { Icon(painterResource(iconRes), contentDescription = label) },
                modifier = Modifier.testTag(OverviewTestTags.filter(filter)),
            )
        }
    }
}

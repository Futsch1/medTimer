package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.list.SelectionListController
import com.futsch1.medtimer.core.ui.rememberMedicineIcon
import com.futsch1.medtimer.feature.ui.overview.actions.ActionsFactory
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import kotlinx.collections.immutable.ImmutableList

/** Caps row width on wide/landscape screens so event rows stay readable instead of stretching edge to edge. */
private val EVENT_LIST_MAX_WIDTH = 540.dp

@Composable
fun OverviewEventList(
    events: ImmutableList<OverviewEvent>,
    selection: SelectionListController<OverviewEvent>,
    listState: LazyListState,
    onEventClick: (OverviewEvent) -> Unit,
    onEnterSelectionMode: (OverviewEvent) -> Unit,
    onAction: (Button, OverviewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val railColor = MaterialTheme.colorScheme.primary
    val railAnchors = remember { mutableStateMapOf<Int, Float>() }
    var listTop by remember { mutableFloatStateOf(0f) }
    val overscrollEffect = rememberOverscrollEffect()

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(EVENT_SPACING),
        overscrollEffect = overscrollEffect?.withoutVisualEffect(),
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .wrapContentWidth(Alignment.Start)
            .widthIn(max = EVENT_LIST_MAX_WIDTH)
            .padding(horizontal = 4.dp)
            .onGloballyPositioned { listTop = it.positionInRoot().y }
            .overscroll(overscrollEffect)
            .drawBehind { drawRailPills(railAnchors.values, listTop, railColor) }
            .testTag(OverviewTestTags.EVENT_LIST),
    ) {
        items(events, key = { it.id }) { event ->
            DisposableEffect(event.id) {
                onDispose { railAnchors.remove(event.id) }
            }
            OverviewEventItem(
                content = event.content,
                state = event.state,
                color = event.color,
                icon = rememberMedicineIcon(event.icon),
                isSelected = selection.isSelected(event),
                isInSelectionMode = selection.isInSelectionMode,
                actions = remember(event) { ActionsFactory().createActions(event) },
                onClick = { onEventClick(event) },
                onToggleSelection = { selection.toggleSelection(event) },
                onEnterSelectionMode = { onEnterSelectionMode(event) },
                onAction = { button -> onAction(button, event) },
                modifier = Modifier.animateItem(),
                onRailAnchor = { railAnchors[event.id] = it },
            )
        }
    }
}

/**
 * One pill per gap between adjacent events, spanning the corridor between the two neighbouring
 * state buttons. A taller item centres its button further from the gap, so the pill facing it
 * stretches toward it.
 *
 * [anchors] are button centres in root coordinates, reported by the items themselves; sorting
 * them recovers the visual order without assuming anything about composition order.
 */
private fun DrawScope.drawRailPills(anchors: Collection<Float>, listTop: Float, color: Color) {
    if (anchors.size < 2) return
    val centers = anchors.map { it - listTop }.sorted()
    val width = RAIL_PILL_WIDTH.toPx()
    val inset = RAIL_INSET.toPx()
    val buttonRadius = EVENT_STATE_BUTTON_SIZE.toPx() / 2
    for (i in 0 until centers.lastIndex) {
        val top = centers[i] + buttonRadius + inset
        val bottom = centers[i + 1] - buttonRadius - inset
        if (bottom <= top) continue
        drawRoundRect(
            color = color,
            topLeft = Offset(RAIL_CENTER_X.toPx() - width / 2, top),
            size = Size(width, bottom - top),
            cornerRadius = CornerRadius(width / 2),
        )
    }
}

package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.overview.actions.Actions
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import com.futsch1.medtimer.feature.ui.overview.model.OverviewState
import com.futsch1.medtimer.feature.ui.overview.model.getImage
import com.futsch1.medtimer.feature.ui.overview.model.toString

@Composable
internal fun EventStateButton(
    state: OverviewState,
    actions: Actions?,
    onAction: (Button) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showActions by remember { mutableStateOf(false) }
    var anchorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    IconButton(
        onClick = { showActions = true },
        modifier = modifier
            .testTag(OverviewTestTags.EVENT_STATE_BUTTON)
            .padding(EVENT_STATE_BUTTON_MARGIN)
            .size(EVENT_STATE_BUTTON_SIZE)
            .onGloballyPositioned { anchorCoordinates = it },
    ) {
        EventStateButtonFace(state, Modifier.fillMaxSize())
    }

    ArcActionMenu(
        expanded = showActions,
        buttons = actions?.visibleButtons.orEmpty(),
        anchorCoordinates = anchorCoordinates,
        anchorContent = { EventStateButtonFace(state, Modifier.clearAndSetSemantics {}) },
        onDismissRequest = { showActions = false },
        onAction = { button ->
            showActions = false
            onAction(button)
        },
    )
}

/**
 * The button's visual only, with no click handling, so [ArcActionMenu] can restate it on top of
 * the scrim while the real button sits dimmed underneath in the host window.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EventStateButtonFace(state: OverviewState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val slideSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()
    val fadeSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()

    Box(
        modifier
            .size(EVENT_STATE_BUTTON_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                slideInHorizontally(slideSpec) { it } + fadeIn(fadeSpec) togetherWith
                        slideOutHorizontally(slideSpec) { -it } + fadeOut(fadeSpec) using
                        SizeTransform(clip = false)
            },
            label = "event_state",
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) { animatedState ->
            Icon(
                painter = painterResource(animatedState.getImage()),
                contentDescription = animatedState.toString(context),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Preview(name = "EventStateButton — Pending")
@Composable
private fun EventStateButtonPendingPreview() {
    EventStateButtonPreview(OverviewState.PENDING)
}

@Preview(name = "EventStateButton — Raised")
@Composable
private fun EventStateButtonRaisedPreview() {
    EventStateButtonPreview(OverviewState.RAISED)
}

@Preview(name = "EventStateButton — Taken")
@Composable
private fun EventStateButtonTakenPreview() {
    EventStateButtonPreview(OverviewState.TAKEN)
}

@Preview(name = "EventStateButton — Skipped")
@Composable
private fun EventStateButtonSkippedPreview() {
    EventStateButtonPreview(OverviewState.SKIPPED)
}

@Preview(name = "EventStateButton — Location")
@Composable
private fun EventStateButtonLocationPreview() {
    EventStateButtonPreview(OverviewState.LOCATION)
}

@Composable
private fun EventStateButtonPreview(state: OverviewState) {
    MedTimerTheme {
        Surface {
            EventStateButton(
                state = state,
                actions = null,
                onAction = {},
            )
        }
    }
}

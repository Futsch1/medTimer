package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.getIcon
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.core.ui.time.formatDuration
import com.futsch1.medtimer.core.ui.time.rememberFormattedDate
import com.futsch1.medtimer.core.ui.time.rememberFormattedTime
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEventContent
import com.futsch1.medtimer.feature.ui.overview.model.StockChange
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.futsch1.medtimer.core.ui.R as CoreUiR

private val DETAIL_ICON_SIZE = 16.dp

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EventContent(content: OverviewEventContent, modifier: Modifier = Modifier) {
    val time = rememberFormattedTime(content.time, content.useRelativeTime)
    val takenTime =
        content.takenTime?.let { rememberFormattedTime(it, content.useRelativeTime, sameDayAs = content.time) }
    val expirationDate = content.expirationDate?.let { rememberFormattedDate(it) }
    val interval = content.interval?.let {
        "(" + stringResource(CoreUiR.string.interval_time, formatDuration(it)) + ")"
    }

    val fadeSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    Column(modifier.animateContentSize(MaterialTheme.motionScheme.slowSpatialSpec())) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Crossfade(content.reminderType, animationSpec = fadeSpec) {
                    Icon(
                        painter = painterResource(it.getIcon()),
                        contentDescription = null,
                        modifier = Modifier.size(DETAIL_ICON_SIZE),
                    )
                }
                Crossfade(time, animationSpec = fadeSpec, modifier = Modifier.weight(1.0f, fill = false)) { Text(it) }
                OptionalDetail(takenTime, modifier = Modifier.weight(1.0f)) { animatedTakenTime ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.arrow_right),
                            contentDescription = null,
                            modifier = Modifier.size(DETAIL_ICON_SIZE),
                        )
                        Text(animatedTakenTime)
                    }
                }
            }
            OptionalDetail(interval) { Text(it) }
            OptionalDetail(expirationDate) { Text(it) }
        }
        OptionalDetail(content.stock) { stock ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.box_seam),
                    contentDescription = null,
                    modifier = Modifier.size(DETAIL_ICON_SIZE),
                )
                Text(MedicineHelper.formatAmount(stock.before, stock.unit))
                if (stock.after != stock.before) {
                    Icon(
                        painter = painterResource(CoreUiR.drawable.arrow_right),
                        contentDescription = null,
                        modifier = Modifier.size(DETAIL_ICON_SIZE),
                    )
                    Text(MedicineHelper.formatAmount(stock.after, stock.unit))
                }
            }
        }
        Crossfade(content.medicineName to content.dose, animationSpec = fadeSpec) { (medicineName, dose) ->
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(medicineName)
                    }
                    if (dose.isNotEmpty()) append(" ($dose)")
                },
            )
        }
    }
}

/**
 * A detail that comes and goes:
 * it reveals downwards and hides upwards, so a part that wraps onto another line still reads as a vertical change.
 * The last non-null value is kept, so the exit animation has something to draw.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T : Any> OptionalDetail(value: T?, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    val fadeSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    val sizeSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntSize>()
    val offsetSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()
    var lastValue by remember { mutableStateOf(value) }
    if (value != null) lastValue = value

    AnimatedVisibility(
        visible = value != null,
        enter = expandVertically(sizeSpec) + slideInVertically(offsetSpec) { -it } + fadeIn(fadeSpec),
        exit = shrinkVertically(sizeSpec) + slideOutVertically(offsetSpec) { -it } + fadeOut(fadeSpec),
        modifier = modifier
    ) {
        lastValue?.let { Crossfade(it, animationSpec = fadeSpec) { current -> content(current) } }
    }
}

private val PREVIEW_TIME: Instant = LocalDate.of(2026, 5, 28).atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant()

@MedTimerPreview
@Composable
private fun MinimalEventContentPreview() {
    MedTimerTheme {
        Surface {
            EventContent(
                content = OverviewEventContent(
                    reminderType = ReminderType.CONTINUOUS_INTERVAL,
                    time = PREVIEW_TIME,
                    medicineName = "Ibuprofen",
                    dose = "",
                ),
            )
        }
    }
}

@MedTimerPreview
@Composable
private fun TakenWithIntervalEventContentPreview() {
    MedTimerTheme {
        Surface {
            EventContent(
                content = OverviewEventContent(
                    reminderType = ReminderType.TIME_BASED,
                    time = PREVIEW_TIME,
                    medicineName = "Vitamin D + Vitamin C + Vitamin E",
                    dose = "1 tablet",
                    takenTime = PREVIEW_TIME.plus(Duration.ofMinutes(42)),
                    interval = Duration.ofMinutes(150),
                ),
            )
        }
    }
}

@MedTimerPreview
@Composable
private fun StockChangeEventContentPreview() {
    MedTimerTheme {
        Surface {
            EventContent(
                content = OverviewEventContent(
                    reminderType = ReminderType.CONTINUOUS_INTERVAL,
                    time = PREVIEW_TIME,
                    medicineName = "Paracetamol",
                    dose = "500 mg",
                    stock = StockChange(before = 12.0, after = 11.0, unit = "pcs"),
                ),
            )
        }
    }
}

@MedTimerPreview
@Composable
private fun ExpirationEventContentPreview() {
    MedTimerTheme {
        Surface {
            EventContent(
                content = OverviewEventContent(
                    reminderType = ReminderType.EXPIRATION_DATE,
                    time = PREVIEW_TIME,
                    medicineName = "Amoxicillin",
                    dose = "250 mg",
                    expirationDate = LocalDate.of(2026, 12, 1),
                ),
            )
        }
    }
}

@MedTimerPreview
@Composable
private fun RelativeTimeEventContentPreview() {
    MedTimerTheme {
        Surface {
            EventContent(
                content = OverviewEventContent(
                    reminderType = ReminderType.CONTINUOUS_INTERVAL,
                    time = PREVIEW_TIME,
                    medicineName = "Ibuprofen",
                    dose = "",
                    useRelativeTime = true,
                    takenTime = PREVIEW_TIME.plus(Duration.ofMinutes(42)),
                ),
            )
        }
    }
}

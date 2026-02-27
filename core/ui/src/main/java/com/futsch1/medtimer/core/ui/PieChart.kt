package com.futsch1.medtimer.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.cos
import kotlin.math.sin

data class PieSegment(
    val value: Float,
    val color: Color,
    val label: String = "",
    val labelColor: Color = Color.Unspecified,
    val legendLabel: String = ""
)

@Composable
fun PieChart(
    segments: ImmutableList<PieSegment>,
    modifier: Modifier = Modifier,
    emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    labelStyle: TextStyle = MaterialTheme.typography.labelSmall,
    showLegend: Boolean = false
) {
    val total = remember(segments) { segments.sumOf { it.value.toDouble() }.toFloat() }
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
        ) {
            if (total == 0f) {
                drawArc(
                    color = emptyColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )
                return@Canvas
            }

            var startAngle = -90f
            for (segment in segments) {
                val sweep = (segment.value / total) * 360f
                drawArc(
                    color = segment.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )

                if (segment.label.isNotEmpty() && segment.value > 0f) {
                    drawSegmentLabel(
                        textMeasurer = textMeasurer,
                        text = segment.label,
                        midAngleDeg = startAngle + sweep / 2f,
                        color = segment.labelColor,
                        style = labelStyle
                    )
                }

                startAngle += sweep
            }
        }

        if (showLegend) {
            Spacer(modifier = Modifier.size(16.dp))
            PieLegend(segments = segments)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PieLegend(segments: ImmutableList<PieSegment>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (segment in segments) {
            if (segment.legendLabel.isNotEmpty()) {
                LegendItem(color = segment.color, label = segment.legendLabel)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun DrawScope.drawSegmentLabel(
    textMeasurer: TextMeasurer,
    text: String,
    midAngleDeg: Float,
    color: Color,
    style: TextStyle
) {
    val midAngleRad = Math.toRadians(midAngleDeg.toDouble())
    val radius = size.minDimension / 2f * 0.6f
    val cx = size.width / 2f + (radius * cos(midAngleRad)).toFloat()
    val cy = size.height / 2f + (radius * sin(midAngleRad)).toFloat()

    val measuredText = textMeasurer.measure(text, style)
    drawText(
        textLayoutResult = measuredText,
        color = color,
        topLeft = Offset(
            cx - measuredText.size.width / 2f,
            cy - measuredText.size.height / 2f
        )
    )
}

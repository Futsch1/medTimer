package com.futsch1.medtimer.feature.ui.impl.medicine

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.ui.platform.LocalResources

@Composable
fun MedicineCard(
    medicine: MedicineScreenItem,
    onMedicineEdit: (id: Int) -> Unit,
    isDragging: Boolean = false,
    @SuppressLint("ModifierParameter")
    dragHandleModifier: Modifier = Modifier.Companion
) {
    val cardColors = if (medicine.color != null) {
        val bg = Color(medicine.color)
        CardDefaults.cardColors(containerColor = bg, contentColor = contentColorFor(bg))
    } else {
        CardDefaults.cardColors()
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .testTag(MedicineTestTags.MEDICINE_ITEM),
        colors = cardColors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            draggedElevation = if (isDragging) 8.dp else 2.dp
        ),
        onClick = { onMedicineEdit(medicine.id) }
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (medicine.icon != null) {
                        Icon(
                            bitmap = medicine.icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    Column {
                        MedicineHeader(medicine)
                        Text(
                            text = pluralStringResource(
                                id = R.plurals.sum_reminders,
                                count = medicine.reminderTimes.size,
                                medicine.reminderTimes.size
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (medicine.inactive) {
                            Text(
                                text = stringResource(R.string.inactive),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (medicine.reminderTimes.isNotEmpty()) {
                            Text(
                                text = medicine.reminderTimes.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                MedicineTags(medicine.tags)
            }
            Icon(
                painter = painterResource(R.drawable.grip_horizontal),
                contentDescription = stringResource(R.string.move_medicine),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp)
                    .then(dragHandleModifier)
            )
        }
    }
}


@Composable
private fun MedicineHeader(medicine: MedicineScreenItem) {
    Text(
        modifier = Modifier.testTag(MedicineTestTags.MEDICINE_NAME),
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(medicine.name)
            }
            if (medicine.stockState.stockString != null) {
                append(" (")
                append(medicine.stockState.stockString)
                if (medicine.stockState.stockWarning) {
                    append(" ")
                    withStyle(SpanStyle(color = Color(0xFFCC0000), fontWeight = FontWeight.Bold)) {
                        append("⚠")
                    }
                }
                if (medicine.stockState.stockRunOutDate != null) {
                    append(", ${medicine.stockState.stockRunOutDate}")
                }
                append(")")
            }
        }
    )
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
private fun MedicineTags(tags: List<String>) {
    if (tags.isEmpty()) return
    val visibleTags = tags.take(MAX_VISIBLE_TAGS)
    val overflowCount = tags.size - visibleTags.size
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            wrap(FlexWrap.Wrap)
            alignItems(FlexAlignItems.Start)
        }
    ) {
        visibleTags.forEach { tag ->
            FilterChip(
                label = { Text(text = tag) },
                selected = true,
                onClick = {},
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        if (overflowCount > 0) {
            FilterChip(
                label = { Text(text = stringResource(R.string.more_tags, overflowCount)) },
                selected = true,
                onClick = {},
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}


private const val MAX_VISIBLE_TAGS = 5

@Composable
private fun contentColorFor(backgroundColor: Color): Color {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val bg = backgroundColor.toArgb() or -0x1000000
    return if (ColorUtils.calculateContrast(onSurface.toArgb(), bg) >=
        ColorUtils.calculateContrast(onPrimary.toArgb(), bg)
    ) onSurface else onPrimary
}


@MedTimerPreview
@Composable
fun MedicineCardPreview() {
    val medicineScreenItem = MedicineScreenItem(
        id = 2,
        name = "Test colored",
        reminderTimes = persistentListOf("8:00", "12:00"),
        tags = persistentListOf("Tag"),
        stockState = StockState("5 left", true, "08/01/25"),
        color = Color.LightGray.toArgb(),
        icon = ResourcesCompat.getDrawable(LocalResources.current, R.drawable.capsule, LocalContext.current.theme)
            ?.toBitmap()
    )
    MedTimerTheme {
        Surface {
            MedicineCard(medicineScreenItem, {})
        }
    }
}

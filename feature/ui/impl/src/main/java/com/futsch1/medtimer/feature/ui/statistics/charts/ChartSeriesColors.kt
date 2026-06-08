package com.futsch1.medtimer.feature.ui.statistics.charts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Assigns a display color (ARGB int) to each Charts series. A series whose medicine opted into a
 * custom color keeps it; the rest cycle through [PALETTE] in order, advancing only when no custom
 * color applies. Compose [Color] is a pure value type (no Android framework), so this stays
 * table-testable on the JVM while keeping IDE color-swatch previews.
 */
object ChartSeriesColors {

    fun assign(seriesNames: List<String>, medicineColorsByName: Map<String, Int>): List<Int> {
        var fallbackIndex = 0
        return seriesNames.map { name ->
            medicineColorsByName[name] ?: PALETTE[fallbackIndex++ % PALETTE.size].toArgb()
        }
    }

    // Compose Color literals so Android Studio renders a color swatch in the gutter for each entry.
    val PALETTE: List<Color> = listOf(
        Color(0xFF003F5C),
        Color(0xFF2F4B7C),
        Color(0xFF665191),
        Color(0xFFA05195),
        Color(0xFFD45087),
        Color(0xFFF95D6A),
        Color(0xFFFF7C43),
        Color(0xFFFFA600),
        Color(0xFF004C6D),
        Color(0xFF295D7D),
        Color(0xFF436F8E),
        Color(0xFF5B829F),
        Color(0xFF7295B0),
        Color(0xFF89A8C2),
        Color(0xFFA1BCD4),
        Color(0xFFB8D0E6),
        Color(0xFFD0E5F8),
    )
}

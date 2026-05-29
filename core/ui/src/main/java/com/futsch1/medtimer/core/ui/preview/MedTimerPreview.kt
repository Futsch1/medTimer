package com.futsch1.medtimer.core.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview annotation for screen-level composables: light, dark, and the minimum supported font
 * scale. Apply to a composable that renders fabricated state (the stateless overload).
 */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Min font scale", showBackground = true, fontScale = 0.85f)
annotation class MedTimerPreview

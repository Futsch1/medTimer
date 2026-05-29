package com.futsch1.medtimer.core.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Multipreview: light, dark, and minimum font scale. Apply to stateless Screen overloads. */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Min font scale", showBackground = true, fontScale = 0.85f)
annotation class MedTimerPreview

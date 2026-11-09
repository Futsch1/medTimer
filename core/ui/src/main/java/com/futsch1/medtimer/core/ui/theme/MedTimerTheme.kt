package com.futsch1.medtimer.core.ui.theme

import android.content.Context
import android.os.Build
import android.util.TypedValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.futsch1.medtimer.core.ui.R

@Composable
fun MedTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val alternativeTheme = !context.usesDynamicColors()
    val useDynamicColor = dynamicColor ?: !alternativeTheme
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme && alternativeTheme -> AlternativeDarkColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialExpressiveTheme(colorScheme = colorScheme, content = content)
}

internal fun Context.usesDynamicColors(): Boolean {
    val value = TypedValue()
    return !theme.resolveAttribute(R.attr.medtimerDynamicColor, value, true) || value.data != 0
}

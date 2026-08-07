package com.futsch1.medtimer.core.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.ScreenTestTags

/**
 * The app's single top bar shape. Every screen renders its own instance.
 *
 * The app shell pads the top system-bar inset for the whole content area, so the bar must not pad it
 * again — hence the fixed empty [WindowInsets].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedTimerTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        modifier = modifier.testTag(ScreenTestTags.TOP_APP_BAR),
        navigationIcon = {
            if (onNavigateUp != null) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.navigate_up),
                    )
                }
            }
        },
        actions = actions,
        colors = colors,
        windowInsets = WindowInsets(0),
    )
}

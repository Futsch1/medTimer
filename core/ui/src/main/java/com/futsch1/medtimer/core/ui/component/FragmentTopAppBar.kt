package com.futsch1.medtimer.core.ui.component

import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme

/**
 * Stacks a Compose top bar above a fragment's own view, for the screens that are still Views.
 *
 * [title] defaults to the navigation destination's label, so the ~15 preference destinations need no
 * per-screen configuration. A custom title is composable so it can be populated asynchronously.
 */
fun Fragment.withTopAppBar(
    contentView: View?,
    title: @Composable () -> String? = { null },
    actions: @Composable RowScope.() -> Unit = {},
): View {
    val navController = findNavController()
    val canNavigateUp = navController.previousBackStackEntry != null

    val topBar = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MedTimerTheme {
                val barTitle = title() ?: navController.currentDestination?.label?.toString().orEmpty()
                MedTimerTopAppBar(
                    title = barTitle,
                    onNavigateUp = if (canNavigateUp) ({ navController.navigateUp() }) else null,
                    actions = actions,
                )
            }
        }
    }

    return LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        addView(topBar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        contentView?.let { addView(it, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)) }
    }
}

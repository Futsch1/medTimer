package com.futsch1.medtimer.core.ui.component

import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme

/**
 * Stacks a Compose top bar above a fragment's own view, for the screens that are still Views.
 *
 * [title] defaults to the navigation destination's label, so the ~15 preference destinations need no
 * per-screen configuration. Both values are read once: a screen's bar never outlives its destination.
 */
fun Fragment.withTopAppBar(
    contentView: View?,
    title: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
): View {
    val navController = findNavController()
    val barTitle = title ?: navController.currentDestination?.label?.toString().orEmpty()
    val canNavigateUp = navController.previousBackStackEntry != null

    val topBar = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MedTimerTheme {
                MedTimerTopAppBar(
                    modifier = Modifier.semantics { testTagsAsResourceId = true },
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

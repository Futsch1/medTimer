package com.futsch1.medtimer.feature.ui

import android.content.Intent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.futsch1.medtimer.core.common.helpers.safeStartActivity
import com.futsch1.medtimer.feature.ui.medicine.tags.TagDataFromPreferences
import com.futsch1.medtimer.feature.ui.medicine.tags.TagsFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.futsch1.medtimer.core.ui.R as CoreUiR

private const val APP_URL = "https://github.com/Futsch1/medTimer"

/**
 * Wires the shared options menu to a hosting fragment. The three top-level screens each render their
 * own bar but present the same options, so the callbacks are assembled once here rather than three
 * times over.
 *
 * Dialogs stay in the view layer and are launched from the fragment's context — no composable needs
 * to reach for a `FragmentActivity`.
 */
@Composable
fun RowScope.AppOptionsMenuHost(
    fragment: Fragment,
    actions: AppOptionsActions,
    optionsViewModel: AppOptionsViewModel,
    tagFilterViewModel: TagFilterViewModel,
    tagsFragmentFactory: TagsFragment.Factory,
    showTagFilter: Boolean = true,
) {
    val context = LocalContext.current
    val hasAnyTags by tagFilterViewModel.hasAnyTags.collectAsStateWithLifecycle()
    val tagsSelected by tagFilterViewModel.tagsSelected.collectAsStateWithLifecycle()

    AppOptionsMenu(
        actions = actions,
        hasTags = showTagFilter && hasAnyTags,
        tagsSelected = tagsSelected,
        onOpenTagFilter = {
            tagsFragmentFactory.create(TagDataFromPreferences(fragment))
                .show(fragment.parentFragmentManager, "tags")
        },
        onOpenSettings = {
            runCatching { fragment.findNavController().navigate(R.id.action_global_preferencesFragment) }
        },
        onClearEvents = {
            MaterialAlertDialogBuilder(context)
                .setTitle(CoreUiR.string.confirm)
                .setMessage(CoreUiR.string.are_you_sure_delete_events)
                .setCancelable(false)
                .setPositiveButton(CoreUiR.string.yes) { _, _ -> optionsViewModel.clearEvents() }
                .setNegativeButton(CoreUiR.string.cancel) { _, _ -> }
                .show()
        },
        onExportEvents = { isCSV ->
            optionsViewModel.exportEvents(isCSV, tagFilterViewModel, fragment.parentFragmentManager)
        },
        onExportMedicines = { isCSV ->
            optionsViewModel.exportMedicines(isCSV, tagFilterViewModel, fragment.parentFragmentManager)
        },
    ) {
        safeStartActivity(context, Intent(Intent.ACTION_VIEW, APP_URL.toUri()))
    }
}

package com.futsch1.medtimer.feature.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.futsch1.medtimer.core.ui.R as CoreUiR

object AppOptionsTestTags {
    const val OVERFLOW = "app_options_overflow"
    const val TAG_FILTER = "app_options_tag_filter"
    const val SETTINGS = "app_options_settings"
    const val GENERATE_TEST_DATA = "app_options_generate_test_data"
    const val GENERATE_TEST_DATA_AND_EVENTS = "app_options_generate_test_data_and_events"
    const val SHOW_INTRO = "app_options_show_intro"
    const val EXPORT_EVENTS_PDF = "app_options_export_events_pdf"
    const val EXPORT_EVENTS_CSV = "app_options_export_events_csv"
    const val EXPORT_MEDICINES_PDF = "app_options_export_medicines_pdf"
    const val EXPORT_MEDICINES_CSV = "app_options_export_medicines_csv"
    const val CLEAR_EVENTS = "app_options_clear_events"
    const val BACKUP_CREATE = "app_options_backup_create"
    const val BACKUP_RESTORE = "app_options_backup_restore"
    const val BACKUP_AUTOMATIC = "app_options_backup_automatic"
}

/**
 * The options shared by the three top-level screens: the tag filter as a visible action, and
 * everything else behind the overflow.
 *
 * Submenus that were nested `<menu>` elements in XML are rendered as labelled sections separated by
 * dividers — nesting real submenus inside a [DropdownMenu] would need a second anchored popup for
 * three groups of two or three items.
 */
@Composable
fun RowScope.AppOptionsMenu(
    actions: AppOptionsActions,
    hasTags: Boolean,
    tagsSelected: Boolean,
    onOpenTagFilter: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearEvents: () -> Unit,
    onExportEvents: (isCSV: Boolean) -> Unit,
    onExportMedicines: (isCSV: Boolean) -> Unit,
    onOpenAppUrl: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    if (hasTags) {
        IconButton(onClick = onOpenTagFilter, modifier = Modifier.testTag(AppOptionsTestTags.TAG_FILTER)) {
            Icon(
                painter = painterResource(
                    if (tagsSelected) CoreUiR.drawable.tag_fill else CoreUiR.drawable.tag
                ),
                contentDescription = stringResource(CoreUiR.string.filter),
            )
        }
    }

    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag(AppOptionsTestTags.OVERFLOW)) {
            Icon(
                painter = painterResource(CoreUiR.drawable.three_dots_vertical),
                contentDescription = stringResource(CoreUiR.string.more_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            @Composable
            fun item(labelRes: Int, iconRes: Int?, testTag: String? = null, onClick: () -> Unit) {
                MenuItem(labelRes, iconRes, testTag) {
                    expanded = false
                    onClick()
                }
            }

            item(CoreUiR.string.tab_settings, CoreUiR.drawable.gear, AppOptionsTestTags.SETTINGS, onOpenSettings)

            HorizontalDivider()
            MenuSectionLabel(CoreUiR.string.event_data)
            item(CoreUiR.string.export_pdf, CoreUiR.drawable.filetype_pdf, AppOptionsTestTags.EXPORT_EVENTS_PDF) { onExportEvents(false) }
            item(CoreUiR.string.export_csv, CoreUiR.drawable.filetype_csv, AppOptionsTestTags.EXPORT_EVENTS_CSV) { onExportEvents(true) }
            item(CoreUiR.string.clear_events, CoreUiR.drawable.trash, AppOptionsTestTags.CLEAR_EVENTS, onClearEvents)

            HorizontalDivider()
            MenuSectionLabel(CoreUiR.string.medicine_data)
            item(CoreUiR.string.export_pdf, CoreUiR.drawable.filetype_pdf, AppOptionsTestTags.EXPORT_MEDICINES_PDF) { onExportMedicines(false) }
            item(CoreUiR.string.export_csv, CoreUiR.drawable.filetype_csv, AppOptionsTestTags.EXPORT_MEDICINES_CSV) { onExportMedicines(true) }

            HorizontalDivider()
            item(CoreUiR.string.automatic_backup, CoreUiR.drawable.gear, AppOptionsTestTags.BACKUP_AUTOMATIC, actions::configureAutomaticBackup)
            item(CoreUiR.string.backup, CoreUiR.drawable.box_arrow_down, AppOptionsTestTags.BACKUP_CREATE, actions::createBackup)
            item(CoreUiR.string.restore, CoreUiR.drawable.box_arrow_in_down, AppOptionsTestTags.BACKUP_RESTORE, actions::restoreBackup)

            if (actions.isDebugBuild) {
                HorizontalDivider()
                item(CoreUiR.string.generate_test_data, CoreUiR.drawable.x_circle, AppOptionsTestTags.GENERATE_TEST_DATA) {
                    actions.generateTestData(withEvents = false)
                }
                item(
                    CoreUiR.string.generate_test_data_and_events,
                    CoreUiR.drawable.x_circle,
                    AppOptionsTestTags.GENERATE_TEST_DATA_AND_EVENTS,
                ) {
                    actions.generateTestData(withEvents = true)
                }
                item(CoreUiR.string.show_intro, null, AppOptionsTestTags.SHOW_INTRO, actions::showAppIntro)
            }

            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(CoreUiR.string.version, actions.versionName)) },
                onClick = {},
                enabled = false,
                leadingIcon = { Icon(painterResource(CoreUiR.drawable.info_circle), contentDescription = null) },
            )
            item(CoreUiR.string.app_url, CoreUiR.drawable.link, onClick = onOpenAppUrl)
        }
    }
}

@Composable
private fun MenuItem(labelRes: Int, iconRes: Int?, testTag: String?, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(labelRes)) },
        onClick = onClick,
        modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier,
        leadingIcon = iconRes?.let { { Icon(painterResource(it), contentDescription = null) } },
    )
}

@Composable
private fun MenuSectionLabel(labelRes: Int) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
    )
}

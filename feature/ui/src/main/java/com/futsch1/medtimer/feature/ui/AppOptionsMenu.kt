package com.futsch1.medtimer.feature.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.R as CoreUiR

object AppOptionsTestTags {
    /** Scopes the entries to this menu; each one is then selected by its own label. */
    const val MENU = "app_options_menu"
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
fun AppOptionsMenu(
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
        IconButton(onClick = onOpenTagFilter) {
            Icon(
                painter = painterResource(
                    if (tagsSelected) CoreUiR.drawable.tag_fill else CoreUiR.drawable.tag
                ),
                contentDescription = stringResource(CoreUiR.string.filter),
            )
        }
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(CoreUiR.drawable.three_dots_vertical),
                contentDescription = stringResource(CoreUiR.string.more_options),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag(AppOptionsTestTags.MENU),
        ) {
            @Composable
            fun item(labelRes: Int, iconRes: Int?, descriptionRes: Int? = null, onClick: () -> Unit) {
                MenuItem(labelRes, iconRes, descriptionRes) {
                    expanded = false
                    onClick()
                }
            }

            item(CoreUiR.string.tab_settings, CoreUiR.drawable.gear, onClick = onOpenSettings)

            HorizontalDivider()
            MenuSectionLabel(CoreUiR.string.event_data)
            item(CoreUiR.string.export_pdf, CoreUiR.drawable.filetype_pdf, CoreUiR.string.export_events_pdf) { onExportEvents(false) }
            item(CoreUiR.string.export_csv, CoreUiR.drawable.filetype_csv, CoreUiR.string.export_events_csv) { onExportEvents(true) }
            item(CoreUiR.string.clear_events, CoreUiR.drawable.trash, onClick = onClearEvents)

            HorizontalDivider()
            MenuSectionLabel(CoreUiR.string.medicine_data)
            item(CoreUiR.string.export_pdf, CoreUiR.drawable.filetype_pdf, CoreUiR.string.export_medicines_pdf) { onExportMedicines(false) }
            item(CoreUiR.string.export_csv, CoreUiR.drawable.filetype_csv, CoreUiR.string.export_medicines_csv) { onExportMedicines(true) }

            HorizontalDivider()
            item(CoreUiR.string.automatic_backup, CoreUiR.drawable.gear, onClick = actions::configureAutomaticBackup)
            item(CoreUiR.string.backup, CoreUiR.drawable.box_arrow_down, onClick = actions::createBackup)
            item(CoreUiR.string.restore, CoreUiR.drawable.box_arrow_in_down, onClick = actions::restoreBackup)

            if (actions.isDebugBuild) {
                HorizontalDivider()
                item(CoreUiR.string.generate_test_data, CoreUiR.drawable.x_circle) {
                    actions.generateTestData(withEvents = false)
                }
                item(CoreUiR.string.generate_test_data_and_events, CoreUiR.drawable.x_circle) {
                    actions.generateTestData(withEvents = true)
                }
                item(CoreUiR.string.show_intro, null, onClick = actions::showAppIntro)
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

/**
 * [descriptionRes] overrides the spoken name for the entries whose label alone does not say what they
 * act on - the two "Export as ..." pairs, one under each data section.
 */
@Composable
private fun MenuItem(labelRes: Int, iconRes: Int?, descriptionRes: Int?, onClick: () -> Unit) {
    val description = descriptionRes?.let { stringResource(it) }
    DropdownMenuItem(
        text = { Text(stringResource(labelRes)) },
        onClick = onClick,
        modifier = if (description != null) {
            Modifier.semantics { contentDescription = description }
        } else {
            Modifier
        },
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

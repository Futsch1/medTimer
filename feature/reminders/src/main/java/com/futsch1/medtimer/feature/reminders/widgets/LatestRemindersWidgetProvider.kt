package com.futsch1.medtimer.feature.reminders.widgets

import android.content.Context
import android.text.SpannableStringBuilder
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.feature.reminders.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class LatestRemindersWidgetProvider @Inject constructor() : WidgetProvider() {

    @Inject
    lateinit var latestRemindersLineProvider: LatestRemindersLineProvider

    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    override fun getWidgetImpl(context: Context): WidgetImpl {
        val lineProvider = if (preferencesDataSource.preferences.value.disableWidget) {
            WidgetLineProvider { line, _ ->
                if (line == 0) SpannableStringBuilder(context.getString(com.futsch1.medtimer.core.ui.R.string.widget_disabled_privacy))
                else SpannableStringBuilder()
            }
        } else {
            latestRemindersLineProvider
        }
        return WidgetImpl(
            context,
            lineProvider,
            WidgetIds(
                R.id.latestReminderWidget,
                R.layout.latest_reminders_widget,
                R.layout.latest_reminders_widget_small
            )
        )
    }
}

package com.futsch1.medtimer.widgets

import android.content.Context
import android.text.SpannableStringBuilder
import com.futsch1.medtimer.R
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NextRemindersWidgetProvider @Inject constructor() : WidgetProvider() {
    @Inject
    lateinit var nextRemindersLineProvider: NextRemindersLineProvider

    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    override fun getWidgetImpl(context: Context): WidgetImpl {
        val lineProvider = if (preferencesDataSource.preferences.value.disableWidget) {
            WidgetLineProvider { line, _ ->
                if (line == 0) SpannableStringBuilder(context.getString(R.string.widget_disabled_privacy))
                else SpannableStringBuilder()
            }
        } else {
            nextRemindersLineProvider
        }
        return WidgetImpl(
            context,
            lineProvider,
            WidgetIds(
                R.id.nextReminderWidget,
                R.layout.next_reminders_widget,
                R.layout.next_reminders_widget_small
            )
        )
    }
}

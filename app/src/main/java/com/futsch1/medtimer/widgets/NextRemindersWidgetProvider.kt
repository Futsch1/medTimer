package com.futsch1.medtimer.widgets

import android.content.Context
import com.futsch1.medtimer.R
import javax.inject.Inject

class NextRemindersWidgetProvider @Inject constructor(val nextRemindersLineProvider: NextRemindersLineProvider) : WidgetProvider() {
    override fun getWidgetImpl(context: Context): WidgetImpl {
        return WidgetImpl(
            context,
            nextRemindersLineProvider,
            WidgetIds(
                R.id.nextReminderWidget,
                R.layout.next_reminders_widget,
                R.layout.next_reminders_widget_small
            )
        )
    }
}

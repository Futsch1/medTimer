package com.futsch1.medtimer.feature.ui.medicine

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
internal fun rememberPreviewTimeFormatter(): TimeFormatter {
    val context = LocalContext.current
    return remember(context) {
        val preferences = context.getSharedPreferences("time_formatter_preview", Context.MODE_PRIVATE)
        TimeFormatter(
            context,
            PreferencesDataSource(preferences, CoroutineScope(Dispatchers.Unconfined), Gson()),
        )
    }
}

package com.futsch1.medtimer.feature.ui.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.util.AttributeSet
import androidx.preference.DialogPreference
import androidx.preference.Preference

@SuppressLint("ResourceType")
class RingtonePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.dialogPreferenceStyle
) : DialogPreference(context, attrs, defStyleAttr) {
    var ringtoneType: Int = RingtoneManager.TYPE_ALARM
        private set

    var showDefault: Boolean = true
        private set

    var showSilent: Boolean = false
        private set

    private var defaultRingtone: Uri? = null

    var ringtone: Uri?
        get() = getPersistedString(defaultRingtone?.toString())?.takeIf(String::isNotEmpty)?.let(Uri::parse)
        private set(value) {
            persistString(value?.toString() ?: "")
            notifyChanged()
        }

    init {
        context.obtainStyledAttributes(
            attrs,
            intArrayOf(android.R.attr.ringtoneType, android.R.attr.showDefault, android.R.attr.showSilent)
        ).use { typedArray ->
            ringtoneType = typedArray.getInt(0, RingtoneManager.TYPE_ALARM)
            showDefault = typedArray.getBoolean(1, true)
            showSilent = typedArray.getBoolean(2, false)
        }
        summaryProvider = SummaryProvider<Preference> { preference ->
            (preference as RingtonePreference).ringtone?.let {
                RingtoneManager.getRingtone(context, it)?.getTitle(context)
            }
                ?: context.getString(android.R.string.unknownName)
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        defaultRingtone = (defaultValue as? String)?.takeIf(String::isNotEmpty)?.let(Uri::parse)
    }

    internal fun setRingtone(value: Uri?) {
        if (callChangeListener(value)) {
            ringtone = value
        }
    }
}

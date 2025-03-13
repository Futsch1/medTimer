package com.futsch1.medtimer.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.google.android.material.color.MaterialColors
import com.maltaisn.icondialog.pack.IconDrawableLoader
import com.maltaisn.icondialog.pack.IconPack
import com.maltaisn.icondialog.pack.IconPackLoader


class MedicineIcons(context: Context) {
    private val defaultDrawable = AppCompatResources.getDrawable(context, R.drawable.capsule)!!
    private var iconColor =
        MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            0
        )

    init {
        IconPackLoader(context).load(R.xml.icon_pack)
        if (pack == null) {
            pack = IconPackLoader(context).load(R.xml.icon_pack)
            pack!!.loadDrawables(IconDrawableLoader(context))
        }
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        if (iconColor != 0) {
            sharedPref.edit { putInt("icon_color", iconColor) }
        } else {
            iconColor = sharedPref.getInt("icon_color", Color.BLACK)
        }
    }

    companion object {
        private var pack: IconPack? = null
    }

    fun getIconDrawable(id: Int): Drawable {
        return pack!!.getIcon(id)?.drawable ?: defaultDrawable
    }

    fun getIconBitmap(id: Int): Bitmap {
        val drawable = getIconDrawable(id)

        val bit = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)

        val canvas = Canvas(bit)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        DrawableCompat.setTint(
            drawable,
            iconColor
        )
        drawable.draw(canvas)

        return bit
    }

    fun getIconPack(): IconPack {
        return pack!!
    }
}
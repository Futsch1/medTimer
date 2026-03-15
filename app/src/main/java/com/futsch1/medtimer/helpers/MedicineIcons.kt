package com.futsch1.medtimer.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.reminders.PersistentDataEntryPoint
import com.maltaisn.icondialog.pack.IconDrawableLoader
import com.maltaisn.icondialog.pack.IconPack
import com.maltaisn.icondialog.pack.IconPackLoader
import dagger.hilt.android.EntryPointAccessors
import kotlin.math.ceil


class MedicineIcons(context: Context) {
    private val defaultDrawable = AppCompatResources.getDrawable(context, R.drawable.capsule)!!
    private var iconColor =
        context.getMaterialColor(
            com.google.android.material.R.attr.colorOnSurface,
            0
        )
    private val persistentDataDataSource: PersistentDataDataSource by lazy {
        // Bridge from non-Hilt to Hilt code
        EntryPointAccessors.fromApplication(context, PersistentDataEntryPoint::class.java).getPersistentDataDataSource()
    }

    init {
        IconPackLoader(context).load(R.xml.icon_pack)
        if (pack == null) {
            pack = IconPackLoader(context).load(R.xml.icon_pack)
            pack!!.loadDrawables(IconDrawableLoader(context))
        }
        // TODO: To remove this, the changes would be really big. Keep this one shared preferences instance for the moment.
        if (iconColor != 0) {
            persistentDataDataSource.setIconColor(iconColor)
        } else {
            iconColor = persistentDataDataSource.data.value.iconColor
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

    fun getIconsBitmap(ids: List<Int>): Bitmap? {
        if (ids.isEmpty()) {
            return null
        }
        if (ids.size == 1) {
            return getIconBitmap(ids[0])
        }

        val iconBitmaps = ids.map { getIconBitmap(it) }
        val iconWidth = iconBitmaps.maxOfOrNull { it.width }!!
        val iconHeight = iconBitmaps.maxOfOrNull { it.height }!!

        val numIcons = ids.size
        val numCols = ceil(kotlin.math.sqrt(numIcons.toDouble())).toInt()
        val numRows = ceil(numIcons.toDouble() / numCols).toInt()

        val resultBitmap = createBitmap(numCols * iconWidth, numRows * iconHeight)
        val canvas = Canvas(resultBitmap)

        for ((index, bitmap) in iconBitmaps.withIndex()) {
            val row = index / numCols
            val col = index % numCols
            canvas.drawBitmap(bitmap, (col * iconWidth).toFloat(), (row * iconHeight).toFloat(), null)
        }

        return resultBitmap
    }

    fun getIconPack(): IconPack {
        return pack!!
    }
}
package com.futsch1.medtimer.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.futsch1.medtimer.R
import com.google.android.material.color.MaterialColors
import com.maltaisn.icondialog.pack.IconDrawableLoader
import com.maltaisn.icondialog.pack.IconPack
import com.maltaisn.icondialog.pack.IconPackLoader


class MedicineIcons private constructor(context: Context) {
    private val pack: IconPack = IconPackLoader(context).load(R.xml.icon_pack)
    private val defaultDrawable = AppCompatResources.getDrawable(context, R.drawable.capsule)!!
    private val iconColor =
        MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, 0)

    init {
        pack.loadDrawables(IconDrawableLoader(context))
    }

    companion object {
        private var instance: MedicineIcons? = null

        @JvmStatic
        fun init(context: Context) {
            instance = MedicineIcons(context)
        }

        @JvmStatic
        fun getIconPack(): IconPack {
            return instance!!.pack
        }

        @JvmStatic
        fun getIconDrawable(id: Int): Drawable {
            return instance!!.pack.getIcon(id)?.drawable ?: instance!!.defaultDrawable
        }

        @JvmStatic
        fun getIconBitmap(id: Int): Bitmap {
            val drawable = getIconDrawable(id)

            val bit = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bit)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            DrawableCompat.setTint(
                drawable,
                instance!!.iconColor
            )
            drawable.draw(canvas)

            return bit
        }
    }
}
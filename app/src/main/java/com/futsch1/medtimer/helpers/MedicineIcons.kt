package com.futsch1.medtimer.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.futsch1.medtimer.R
import com.maltaisn.icondialog.pack.IconDrawableLoader
import com.maltaisn.icondialog.pack.IconPack
import com.maltaisn.icondialog.pack.IconPackLoader

class MedicineIcons private constructor(context: Context) {
    private val pack: IconPack = IconPackLoader(context).load(R.xml.icon_pack)

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
        fun getIconDrawable(id: Int): Drawable? {
            return instance!!.pack.getIcon(id)?.drawable
        }

        @JvmStatic
        fun getIconBitmap(id: Int): Bitmap {
            val drawable = instance!!.pack.getIcon(id)?.drawable

            val bit = Bitmap.createBitmap(
                drawable!!.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bit)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bit
        }
    }
}
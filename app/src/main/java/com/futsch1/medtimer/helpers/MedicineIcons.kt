package com.futsch1.medtimer.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
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
        fun toImageView(imageView: ImageView, id: Int) {
            if (id != 0) {
                imageView.setImageDrawable(getIconDrawable(id))
                imageView.visibility = View.VISIBLE
            } else {
                imageView.visibility = View.GONE
            }
        }
    }
}
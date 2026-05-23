package com.futsch1.medtimer.core.common

import android.annotation.SuppressLint
import android.view.Menu
import androidx.appcompat.view.menu.MenuBuilder
import java.lang.reflect.InvocationTargetException

@SuppressLint("RestrictedApi")
fun enableOptionalIcons(menu: Menu) {
    if (menu is MenuBuilder) {
        try {
            val m = menu.javaClass.getDeclaredMethod(
                "setOptionalIconsVisible", Boolean::class.java
            )
            m.invoke(menu, true)
        } catch (_: NoSuchMethodException) {
            // Intentionally empty
        } catch (_: IllegalAccessException) {
            // Intentionally empty
        } catch (_: InvocationTargetException) {
            // Intentionally empty
        }
    }
}

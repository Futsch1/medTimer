package com.futsch1.medtimer

import android.Manifest.permission
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.google.android.material.color.MaterialColors

class MedTimerAppIntro : AppIntro() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addSlide(
            AppIntroFragment.createInstance(
                getString(R.string.app_name),
                "This is a demo example in java of AppIntro library, with a custom background on each slide!",
                R.drawable.capsule,
                backgroundColorRes = getColorRes(com.google.android.material.R.attr.colorSurface),
                descriptionColorRes = getColorRes(com.google.android.material.R.attr.colorOnSurface),
                titleColorRes = getColorRes(com.google.android.material.R.attr.colorOnSurface)
            )
        )

        addSlide(
            AppIntroFragment.createInstance(
                getString(R.string.custom),
                "This is a demo example in java of AppIntro library, with a custom background on each slide!",
                R.drawable.capsule,
                backgroundColorRes = getColorRes(com.google.android.material.R.attr.colorSurface),
                descriptionColorRes = getColorRes(com.google.android.material.R.attr.colorOnSurface),
                titleColorRes = getColorRes(com.google.android.material.R.attr.colorOnSurface)
            )
        )

        askForPermissions(
            arrayOf(permission.POST_NOTIFICATIONS),
            1,
            true
        )
        this.isSystemBackButtonLocked = true
        this.isButtonsEnabled = true
        this.isImmersive = false

        this.setColorDoneText(
            MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE
            )
        )
        this.setNextArrowColor(
            MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE
            )
        )
        this.setColorSkipButton(
            MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE
            )
        )
        this.setBarColor(
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorPrimary,
                Color.WHITE
            )
        )
    }

    private fun getColorRes(colorAttributeResInt: Int): Int {
        val typedValue = TypedValue()
        if (this.getTheme().resolveAttribute(colorAttributeResInt, typedValue, true)) {
            return typedValue.resourceId
        }
        return 0
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        finish()
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        finish()
    }
}

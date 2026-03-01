package com.futsch1.medtimer

import android.Manifest.permission
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.google.android.material.color.MaterialColors

class MedTimerAppIntro : AppIntro() {
    private val requestPostNotificationPermission = RequestPostNotificationPermission(this)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Applying fix mentioned here: https://github.com/AppIntro/AppIntro/issues/1263#issuecomment-2661393509
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        WindowCompat.setDecorFitsSystemWindows(window, false)

        addSlide(
            getString(R.string.intro_welcome),
            getString(R.string.intro_welcome_description),
            R.mipmap.logo
        )

        addSlide(
            getString(R.string.tab_medicine),
            getString(R.string.intro_medicine_description),
            R.drawable.intro_medicine
        )

        addSlide(
            getString(R.string.reminders),
            getString(R.string.intro_reminder_description),
            R.drawable.intro_reminder
        )

        addSlide(
            getString(R.string.show_notifications),
            getString(R.string.intro_notification_description),
            R.drawable.intro_notification
        )

        addSlide(
            getString(R.string.tab_overview),
            getString(R.string.intro_overview_description),
            R.drawable.intro_overview
        )

        addSlide(
            getString(R.string.analysis),
            getString(R.string.intro_analysis_description),
            R.drawable.intro_analysis
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askForPermissions(
                arrayOf(permission.POST_NOTIFICATIONS),
                1,
                true
            )
        }
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
                androidx.appcompat.R.attr.colorPrimary,
                Color.WHITE
            )
        )

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBarsInsets.top, 0, systemBarsInsets.bottom)
            insets
        }

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false // Or true if you want light status bar icons
        // Make navigation bar transparent
        windowInsetsController.isAppearanceLightNavigationBars = false
    }

    private fun addSlide(title: String, description: String, image: Int) {
        addSlide(
            AppIntroFragment.createInstance(
                title,
                description,
                image,
                backgroundColorRes = getColorRes(com.google.android.material.R.attr.colorSurface),
                descriptionColorRes = getColorRes(com.google.android.material.R.attr.colorOnSurface),
                titleColorRes = getColorRes(com.google.android.material.R.attr.colorOnSurface)
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
        requestPostNotificationPermission.requestPermission()
        finish()
    }
}

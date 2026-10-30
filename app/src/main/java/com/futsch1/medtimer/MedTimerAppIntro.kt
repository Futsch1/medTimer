package com.futsch1.medtimer

import android.Manifest.permission
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.futsch1.medtimer.core.common.helpers.getMaterialColor
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.RequestPostNotificationPermission
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MedTimerAppIntro : AppIntro() {
    @Inject
    lateinit var persistentDataDataSource: PersistentDataDataSource
    private val requestNotificationPermission =
        RequestPostNotificationPermission(this) { persistentDataDataSource.setShowNotifications(false) }

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
            getString(R.string.intro_setup),
            getString(R.string.intro_setup_description),
            R.drawable.intro_medicine
        )

        addSlide(
            getString(R.string.tab_overview),
            getString(R.string.intro_overview_orientation_description),
            R.drawable.intro_overview
        )

        addSlide(
            getString(R.string.intro_notifications),
            getString(R.string.intro_notifications_description),
            R.drawable.intro_notification
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
            this.getMaterialColor(
                com.google.android.material.R.attr.colorOnPrimaryContainer, Color.BLACK
            )
        )
        this.setNextArrowColor(
            this.getMaterialColor(
                com.google.android.material.R.attr.colorOnPrimaryContainer, Color.BLACK
            )
        )
        this.setColorSkipButton(
            this.getMaterialColor(
                com.google.android.material.R.attr.colorOnPrimaryContainer, Color.BLACK
            )
        )
        this.setBarColor(
            this.getMaterialColor(
                com.google.android.material.R.attr.colorPrimaryContainer,
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
        if (this.theme.resolveAttribute(colorAttributeResInt, typedValue, true)) {
            return typedValue.resourceId
        }
        return 0
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        finish()
    }

    override fun onSlideChanged(oldFragment: Fragment?, newFragment: Fragment?) {
        super.onSlideChanged(oldFragment, newFragment)
        val description = newFragment?.view?.findViewById<TextView>(com.github.appintro.R.id.description) ?: return
        description.post {
            val linkText = getString(R.string.intro_getting_started_link)
            val start = description.text.indexOf(linkText)
            if (start < 0 || (description.text as? Spanned)
                    ?.getSpans(0, description.length(), ClickableSpan::class.java)
                    ?.isNotEmpty() == true
            ) {
                return@post
            }

            description.text = SpannableString(description.text).apply {
                setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) = openGettingStarted()
                    },
                    start,
                    start + linkText.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            description.movementMethod = LinkMovementMethod.getInstance()
            description.highlightColor = Color.TRANSPARENT
            description.setOnClickListener { openGettingStarted() }
        }
    }

    private fun openGettingStarted() {
        HelpReaderDialogFragment.newInstance("getting-started")
            .show(supportFragmentManager, "help")
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        requestNotificationPermission.requestPermission()
        finish()
    }
}

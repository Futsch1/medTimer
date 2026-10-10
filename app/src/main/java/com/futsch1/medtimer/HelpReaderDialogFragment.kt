package com.futsch1.medtimer

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.toDrawable
import androidx.webkit.WebViewAssetLoader
import com.futsch1.medtimer.core.ui.R
import kotlin.math.roundToInt

/** Full-screen offline guide shown over the current destination; Back returns to contents before closing. */
class HelpReaderDialogFragment : AppCompatDialogFragment() {
    private lateinit var webView: WebView
    private var webViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        setStyle(STYLE_NORMAL, R.style.Theme_MedTimer)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = super.onCreateDialog(savedInstanceState).apply {
        setOnKeyListener { _, keyCode, event ->
            if (keyCode != KeyEvent.KEYCODE_BACK) {
                false
            } else {
                if (event.action == KeyEvent.ACTION_UP) navigateBack()
                true
            }
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val surfaceColor = TypedValue().let { value ->
            requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, value, true)
            value.data
        }
        val toolbar = Toolbar(requireContext()).apply {
            title = getString(R.string.help)
            setBackgroundColor(surfaceColor)
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationContentDescription(R.string.back)
            setNavigationOnClickListener { navigateBack() }
        }
        webViewDestroyed = false
        webView = WebView(requireContext()).apply {
            settings.javaScriptEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            settings.blockNetworkLoads = true
            settings.textZoom = (100 * resources.configuration.fontScale).roundToInt()
            importantForAccessibility = WebView.IMPORTANT_FOR_ACCESSIBILITY_YES
        }

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(surfaceColor)
            addView(toolbar, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            addView(webView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .build()
        webView.webViewClient = HelpGuideWebViewClient(
            assetLoader = assetLoader,
            onRendererGone = { view ->
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
                webViewDestroyed = true
                dismissAllowingStateLoss()
            },
            openExternalLink = ::openExternalLink
        )

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = navigateBack()
        })

        val topic = arguments?.getString(ARG_TOPIC).orEmpty()
        val page = topic.takeIf(TOPIC_PATTERN::matches)?.let { "$it.html" } ?: "index.html"
        webView.loadUrl("$ASSET_ORIGIN$ASSET_PATH$page")
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }

    private fun navigateBack() {
        val currentPath = webView.url?.let(Uri::parse)?.path
        val openedDirectlyOnTopic = !arguments?.getString(ARG_TOPIC).isNullOrBlank()
        val atContents = currentPath == "$ASSET_PATH$CONTENTS_PAGE"
        if (webView.canGoBack() && (!atContents || openedDirectlyOnTopic)) {
            webView.goBack()
        } else {
            dismiss()
        }
    }

    override fun onDestroyView() {
        if (!webViewDestroyed) webView.destroy()
        webViewDestroyed = false
        super.onDestroyView()
    }

    private fun openExternalLink(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            // No browser is available; keep the user in the offline guide.
        }
    }

    companion object {
        private const val ARG_TOPIC = "topic"
        private const val ASSET_HOST = "appassets.androidplatform.net"
        private const val ASSET_PATH = "/assets/"
        private const val CONTENTS_PAGE = "index.html"
        private const val ASSET_ORIGIN = "https://$ASSET_HOST"
        private val TOPIC_PATTERN = Regex("[a-z0-9-]+")

        fun newInstance(topic: String? = null) = HelpReaderDialogFragment().apply {
            arguments = Bundle().apply { topic?.let { putString(ARG_TOPIC, it) } }
        }
    }
}


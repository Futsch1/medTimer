package com.futsch1.medtimer.feature.ui.medicine.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScannedPackagesFragment : Fragment() {
    private val viewModel: ScannedPackagesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MedTimerTheme {
                    val items by viewModel.items.collectAsStateWithLifecycle()
                    ScannedPackagesScreen(
                        items = items,
                        onEditQuantity = viewModel::updateQuantity,
                        onForget = viewModel::forget,
                        onForgetAll = viewModel::forgetAll
                    )
                }
            }
        }
    }
}

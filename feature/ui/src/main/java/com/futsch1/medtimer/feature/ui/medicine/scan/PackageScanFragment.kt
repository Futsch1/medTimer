package com.futsch1.medtimer.feature.ui.medicine.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.ui.R as UiR
import com.futsch1.medtimer.feature.ui.R
import com.futsch1.medtimer.feature.ui.medicine.ocr.PackageTextRecognizer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Full-screen live scanner: keeps the camera preview running and OCRs frames, matching recognized
 * package text against known medicines and refilling stock automatically - no shutter press
 * needed. As soon as one package is recognized (or its ambiguity is resolved via a dialog),
 * analysis pauses and a "Next" button appears; tapping it resumes scanning for another package.
 * Stays open so several distinct packages can be scanned one after the other in the same session;
 * back/up navigation closes it.
 */
@AndroidEntryPoint
class PackageScanFragment : Fragment() {
    @Inject
    lateinit var textRecognizer: PackageTextRecognizer

    @Inject
    lateinit var packageMatcherFactory: PackageMatcher.Factory

    @Inject
    @Dispatcher(MedTimerDispatchers.IO)
    lateinit var ioDispatcher: CoroutineDispatcher

    private lateinit var packageMatcher: PackageMatcher
    private val analyzing = AtomicBoolean(false)
    private val scanningPaused = AtomicBoolean(false)

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), UiR.string.package_scan_camera_permission_denied, Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packageMatcher = packageMatcherFactory.create(this) { message -> showRecognizedFeedback(message) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_package_scan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!textRecognizer.isSupported) {
            findNavController().popBackStack()
            return
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val previewView = view?.findViewById<PreviewView>(R.id.cameraPreview) ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), ::analyzeFrame) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (_: IllegalStateException) {
                // View lifecycle already destroyed by the time the future resolved; nothing to bind to.
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun showRecognizedFeedback(message: String) {
        scanningPaused.set(true)
        val banner = view?.findViewById<TextView>(R.id.recognizedBanner) ?: return
        val nextButton = view?.findViewById<Button>(R.id.nextButton) ?: return
        banner.text = "✓ $message"
        banner.visibility = View.VISIBLE
        nextButton.visibility = View.VISIBLE
        nextButton.setOnClickListener {
            banner.visibility = View.GONE
            nextButton.visibility = View.GONE
            packageMatcher.resumeScanning()
            scanningPaused.set(false)
        }
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        if (scanningPaused.get() || !analyzing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch(ioDispatcher) {
            try {
                val blocks = textRecognizer.recognize(imageProxy)
                if (blocks.isNotEmpty()) {
                    packageMatcher.handleBlocks(blocks)
                }
            } finally {
                imageProxy.close()
                analyzing.set(false)
            }
        }
    }
}

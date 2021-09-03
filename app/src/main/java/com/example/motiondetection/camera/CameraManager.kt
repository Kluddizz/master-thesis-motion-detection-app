package com.example.motiondetection.camera

import android.content.Context
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.motiondetection.utils.ImageHelper.Companion.toBitmap

class CameraManager(
    private val context: Context,
    private val previewView: PreviewView
) :
    CameraXConfig.Provider,
    ImageAnalysis.Analyzer {

    var listener: CameraListener? = null
    private var isDetecting: Boolean = false

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }

    override fun analyze(image: ImageProxy) {
        val bitmap = image.toBitmap()
        image.close()

        if (!isDetecting) {
            isDetecting = true

            Thread {
                bitmap?.let { listener?.onCameraFrame(it) }
                isDetecting = false
            }.start()
        }
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val executor = ContextCompat.getMainExecutor(context)
        imageAnalysis.setAnalyzer(executor, this)

        preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, imageAnalysis, preview)
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }
}
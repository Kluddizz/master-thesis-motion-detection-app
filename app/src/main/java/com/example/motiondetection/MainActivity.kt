package com.example.motiondetection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.camera.view.PreviewView
import com.example.motiondetection.camera.CameraListener
import com.example.motiondetection.camera.CameraManager
import com.example.motiondetection.detection.MotionDetector
import com.example.motiondetection.detection.MotionNet
import com.example.motiondetection.detection.models.Person
import com.example.motiondetection.ui.PersonOverlay
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var imageView: PersonOverlay
    private lateinit var scoreTextView: TextView
    private lateinit var inferenceSpeedTextView: TextView
    private lateinit var cameraManager: CameraManager
    private lateinit var detector: MotionDetector
    private lateinit var labels: List<String>

    companion object {
        const val REQUEST_CODE_CAMERA_PERMISSION = 10
    }

    private fun loadModel(modelFile: String) : MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        labels = readLabels("labels.txt")
        previewView = findViewById(R.id.previewView)
        imageView = findViewById(R.id.imageView)
        scoreTextView = findViewById(R.id.scoreTextView)
        inferenceSpeedTextView = findViewById(R.id.inferenceSpeedTextView)

        cameraManager = CameraManager(this, previewView)

        val poseModel = loadModel("movenet_lightning.tflite")
        val motionModel = loadModel("motionnet.tflite")
        detector = MotionNet(poseModel, motionModel)

        when {
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCameraPreview()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                requestCameraPermission()
            }
            else -> {
                requestCameraPermission()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCameraPreview()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun readLabels(fileName: String) : List<String> {
        return assets.open(fileName).bufferedReader().use { it.readLines() }
    }

    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_CAMERA_PERMISSION)
    }

    private val detectionCallback = object : MotionDetector.DetectionCallback {
        override fun invoke(bitmap: Bitmap, person: Person, motion: Int, inferenceSpeed: Long) {
            runOnUiThread {
                imageView.drawPerson(person)
                scoreTextView.text = labels[motion]
                inferenceSpeedTextView.text = "${inferenceSpeed}ms (${1000 / inferenceSpeed} FPS)"
            }
        }
    }

    private val cameraListener = object : CameraListener {
        override fun onCameraFrame(bitmap: Bitmap) {
            detector.detect(bitmap, detectionCallback)
        }
    }

    private fun startCameraPreview() {
        cameraManager.listener = cameraListener
        cameraManager.startCamera()
    }
}
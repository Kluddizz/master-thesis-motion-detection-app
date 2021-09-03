package com.example.motiondetection.camera

import android.graphics.Bitmap

interface CameraListener {
    fun onCameraFrame(bitmap: Bitmap)
}
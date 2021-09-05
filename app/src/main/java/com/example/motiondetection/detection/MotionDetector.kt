package com.example.motiondetection.detection

import android.graphics.Bitmap
import com.example.motiondetection.detection.models.Person
import com.example.motiondetection.utils.ArrayHelper.Companion.leftShift
import kotlin.system.measureTimeMillis

abstract class MotionDetector {
    private var personBuffer = Array(20) { Person(17) }

    interface DetectionCallback {
        fun invoke(bitmap: Bitmap, person: Person, motion: Int, inferenceSpeed: Long)
    }

    fun detect(bitmap: Bitmap, detectionCallback: DetectionCallback?) {
        val person : Person
        val motion: Int

        val inferenceSpeedMs = measureTimeMillis {
            person = detectSinglePose(bitmap)
            personBuffer = personBuffer.leftShift(1)
            personBuffer[personBuffer.lastIndex] = person
            motion = detectMotion(personBuffer)
        }

        detectionCallback?.invoke(bitmap, person, motion, inferenceSpeedMs)
    }

    protected abstract fun detectMotion(persons: Array<Person>) : Int
    protected abstract fun detectSinglePose(bitmap: Bitmap) : Person

}
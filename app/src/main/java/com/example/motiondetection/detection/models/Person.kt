package com.example.motiondetection.detection.models

open class Person(numberKeyPoints: Int) {
    var keyPoints: Array<KeyPoint> = Array(numberKeyPoints) { KeyPoint(0.0f, 0.0f, 0.0f) }
}
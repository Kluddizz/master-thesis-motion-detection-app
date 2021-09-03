package com.example.motiondetection.detection.models

open class KeyPoint(
    var x: Float,
    var y: Float,
    var score: Float
) {
    override fun toString(): String {
        return "{${x}, ${y}, ${score}}"
    }
}
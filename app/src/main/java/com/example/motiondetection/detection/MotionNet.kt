package com.example.motiondetection.detection

import android.graphics.Bitmap
import android.util.Log
import com.example.motiondetection.detection.models.KeyPoint
import com.example.motiondetection.detection.models.Person
import com.example.motiondetection.utils.ImageHelper.Companion.rotate
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import com.example.motiondetection.utils.ArrayHelper.Companion.argmax
import java.nio.MappedByteBuffer

class MotionNet(poseModel: MappedByteBuffer, motionModel: MappedByteBuffer) : MotionDetector() {
    private var motionInterpreter: Interpreter = Interpreter(motionModel)
    private var poseInterpreter: Interpreter = Interpreter(poseModel)
    private var poseInputShape: IntArray = poseInterpreter.getInputTensor(0).shape()
    private var poseOutputShape: IntArray = poseInterpreter.getOutputTensor(0).shape()

    private fun processInputImage(bitmap: Bitmap, inputWidth: Int, inputHeight: Int): TensorImage? {
        val imageProcessor = ImageProcessor.Builder().apply {
            add(ResizeOp(inputWidth, inputHeight, ResizeOp.ResizeMethod.BILINEAR))
        }.build()

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)

        return imageProcessor.process(tensorImage)
    }

    override fun detectMotion(persons: Array<Person>): Int {
        val input = Array(persons.size) { Array(17) { FloatArray(3) { 0.0f } } }

        for (i in persons.indices) {
            val person = persons[i]

            for (j in person.keyPoints.indices) {
                input[i][j][0] = person.keyPoints[j].y
                input[i][j][1] = person.keyPoints[j].x
                input[i][j][2] = person.keyPoints[j].score
            }
        }

        val output = arrayOf(FloatArray(3) { 0.0f })
        motionInterpreter.run(arrayOf(input), output)

        val predictedLabel = output[0].argmax()

        predictedLabel?.let { return it }
        return 0
    }

    override fun detectSinglePose(bitmap: Bitmap): Person {
        val rotatedBitmap = bitmap.rotate(90.0f)

        val inputWidth = poseInputShape[1]
        val inputHeight = poseInputShape[2]
        val inputTensor = processInputImage(rotatedBitmap, inputWidth, inputHeight)
        val outputTensor = TensorBuffer.createFixedSize(poseOutputShape, DataType.FLOAT32)
        val numberKeyPoints = poseOutputShape[2]
        val person = Person(numberKeyPoints)

        inputTensor?.let { input ->
            poseInterpreter.run(input.tensorBuffer.buffer, outputTensor.buffer)
            val output = outputTensor.floatArray

            val keyPoints = mutableListOf<KeyPoint>()

            for (i in 0 until numberKeyPoints) {
                // Read out the buffer (3 components: x, y, score).
                val x = output[i * 3 + 1]
                val y = output[i * 3 + 0]
                val score = output[i * 3 + 2]

                // Create key point and add it to a temporary list.
                val keyPoint = KeyPoint(x, y, score)
                keyPoints.add(keyPoint)
            }

            person.keyPoints = keyPoints.toTypedArray()
        }

        return person
    }
}
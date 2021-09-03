package com.example.motiondetection.utils

class ArrayHelper {
    companion object {
        fun <T> Array<T>.leftShift(d: Int): Array<T> {
            val newList = this.copyOf()
            var shift = d
            if (shift > size) shift %= size
            forEachIndexed { index, value ->
                val newIndex = (index + (size - shift)) % size
                newList[newIndex] = value
            }
            return newList
        }

        fun FloatArray.argmax(): Int? {
            return withIndex().maxByOrNull { it.value }?.index
        }
    }
}
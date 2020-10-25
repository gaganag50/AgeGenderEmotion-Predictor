package com.gagan.agepredictor.ext

import org.opencv.core.Mat

fun Mat.maxProb(): Pair<Int, Double> {
    var index = 0
    var maxConfidence = 0.0
    for (j in 0 until this.cols()) {
        val d = this.get(0, j)[0]
        if (maxConfidence < d) {
            maxConfidence = d
            index = j
        }
    }
    return Pair(index, maxConfidence)
}
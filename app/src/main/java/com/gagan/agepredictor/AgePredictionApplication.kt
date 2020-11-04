package com.gagan.agepredictor

import android.app.Application
import org.opencv.android.OpenCVLoader

class AgePredictionApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        OpenCVLoader.initDebug()
    }
    companion object{
        val TAG = "GAGANGAGAN"
    }
}
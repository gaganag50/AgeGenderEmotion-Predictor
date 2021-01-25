package com.gagan.agepredictor.appdata

import android.graphics.Bitmap

data class ItemDetected(var face: Bitmap, var ageBucket: String, var gender: String, var emotion: String)
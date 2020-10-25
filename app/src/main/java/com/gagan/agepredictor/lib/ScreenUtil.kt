package com.gagan.agepredictor.lib

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager

object ScreenUtil {
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = Point()
        wm.defaultDisplay.getSize(p)
        return p.x
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    fun getScreenHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = Point()
        wm.defaultDisplay.getSize(p)
        return p.y
    }

    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }
}
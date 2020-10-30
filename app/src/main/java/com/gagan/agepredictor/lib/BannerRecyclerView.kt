package com.gagan.agepredictor.lib

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

class BannerRecyclerView : RecyclerView {
    private var mOnPageChangeListeners: MutableList<OnPageChangeListener>? = null
    private var mOnPageChangeListener: OnPageChangeListener? = null

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
    }

    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        var velocityX = velocityX
        var velocityY = velocityY
        if (mEnableLimitVelocity) {
            velocityX = solveVelocity(velocityX)
            velocityY = solveVelocity(velocityY)
        }
        return super.fling(velocityX, velocityY)
    }

    private fun solveVelocity(velocity: Int): Int {
        return if (velocity > 0) {
            Math.min(velocity, FLING_MAX_VELOCITY)
        } else {
            Math.max(velocity, -FLING_MAX_VELOCITY)
        }
    }





    interface OnPageChangeListener {
        fun onPageSelected(position: Int)
    }

    fun dispatchOnPageSelected(position: Int) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener!!.onPageSelected(position)
        }
        if (mOnPageChangeListeners != null) {
            var i = 0
            val z = mOnPageChangeListeners!!.size
            while (i < z) {
                val listener = mOnPageChangeListeners!![i]
                listener.onPageSelected(position)
                i++
            }
        }
    }

    companion object {
        private const val FLING_SCALE_DOWN_FACTOR = 0.5f //Deceleration factor
        private const val FLING_MAX_VELOCITY = 8000 // Maximum clockwise sliding speed
        private var mEnableLimitVelocity = true
        fun ismEnableLimitVelocity(): Boolean {
            return mEnableLimitVelocity
        }

        fun setmEnableLimitVelocity(mEnableLimitVelocity: Boolean) {
            Companion.mEnableLimitVelocity = mEnableLimitVelocity
        }
    }
}
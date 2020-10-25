package com.gagan.agepredictor.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout


class VpSwipeRefreshLayout(context: Context, attrs: AttributeSet?) :
    SwipeRefreshLayout(context, attrs) {
    private var startY = 0f
    private var startX = 0f

    // Record viewPager Whether to drag the mark
    private var mIsVpDragger = false
    private val mTouchSlop: Int
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                //Record the position of the finger
                startY = ev.y
                startX = ev.x
                // Initialization tag
                mIsVpDragger = false
            }
            MotionEvent.ACTION_MOVE -> {
                // in case viewpager
                //Is being dragged, so don’t intercept its events, directly
                // return false；
                if (mIsVpDragger) {
                    return false
                }

                // Get current finger position
                val endY = ev.y
                val endX = ev.x
                val distanceX = Math.abs(endX - startX)
                val distanceY = Math.abs(endY - startY)
                // If the X-axis displacement is greater than the Y-axis displacement, then hand
                // the event to viewPager deal with。
                if (distanceX > mTouchSlop && distanceX > distanceY) {
                    mIsVpDragger = true
                    return false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->                 // Initialization tag
                mIsVpDragger = false
        }
        // If the Y-axis displacement is greater than the X-axis, the event is given to swipeRefreshLayout deal with。
        return super.onInterceptTouchEvent(ev)
    }

    init {
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }
}
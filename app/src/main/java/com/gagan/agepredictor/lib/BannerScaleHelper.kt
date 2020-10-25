package com.gagan.agepredictor.lib

import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class BannerScaleHelper {
    private var mRecyclerView: BannerRecyclerView? = null
    private var mContext: Context? = null
    private var mScale = 0.9f
    private var mPagePadding =
        BannerAdapterHelper.sPagePadding
    private var mShowLeftCardWidth = BannerAdapterHelper.sShowLeftCardWidth
    private var mCardWidth
            = 0
    private var mOnePageWidth
            = 0
    private var mCardGalleryWidth = 0
    private var mFirstItemPos = 0
    private var mCurrentItemOffset = 0
    private val mLinearSnapHelper = CardLinearSnapHelper()
    private var mLastPos = 0
    fun attachToRecyclerView(mRecyclerView: BannerRecyclerView?) {
        if (mRecyclerView == null) {
            return
        }
        this.mRecyclerView = mRecyclerView
        mContext = mRecyclerView.context
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mLinearSnapHelper.mNoNeedToScroll = currentItem == 0 ||
                            currentItem == mRecyclerView.adapter!!.itemCount - 2
                    if (mLinearSnapHelper.finalSnapDistance[0] == 0
                        && mLinearSnapHelper.finalSnapDistance[1] == 0
                    ) {
                        mCurrentItemOffset = 0
                        mLastPos = currentItem
                        mRecyclerView.dispatchOnPageSelected(mLastPos)
                    }
                } else {
                    mLinearSnapHelper.mNoNeedToScroll = false
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                mCurrentItemOffset += dx
                onScrolledChangedCallback()
            }
        })
        initWidth()
        mLinearSnapHelper.attachToRecyclerView(mRecyclerView)
    }


    private fun initWidth() {
        mRecyclerView!!.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mRecyclerView!!.viewTreeObserver.removeGlobalOnLayoutListener(this)
                mCardGalleryWidth = mRecyclerView!!.width
                mCardWidth = mCardGalleryWidth - ScreenUtil.dip2px(
                    mContext!!,
                    (2 * (mPagePadding + mShowLeftCardWidth)).toFloat()
                )
                mOnePageWidth = mCardWidth
                scrollToPosition(mFirstItemPos)
            }
        })
    }

    fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        if (mRecyclerView == null) {
            return
        }
        if (smoothScroll) {
            mRecyclerView!!.smoothScrollToPosition(item)
        } else {
            scrollToPosition(item)
        }
    }

    fun scrollToPosition(pos: Int) {
        if (mRecyclerView == null) {
            return
        }
        //mRecyclerView.getLayoutManager()).scrollToPositionWithOffset 方法不会回调  RecyclerView.OnScrollListener 的onScrollStateChanged方法,是瞬间跳到指定位置
        //mRecyclerView.smoothScrollToPosition 方法会回调  RecyclerView.OnScrollListener 的onScrollStateChanged方法 并且是自动居中，有滚动过程的滑动到指定位置
        (mRecyclerView!!.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
            pos,
            ScreenUtil.dip2px(mContext!!, (mPagePadding + mShowLeftCardWidth).toFloat())
        )
        mCurrentItemOffset = 0
        mLastPos = pos

        mRecyclerView!!.dispatchOnPageSelected(mLastPos)
        //onScrolledChangedCallback();
        mRecyclerView!!.post { onScrolledChangedCallback() }
    }

    fun setFirstItemPos(firstItemPos: Int) {
        mFirstItemPos = firstItemPos
    }

    private fun onScrolledChangedCallback() {
        if (mOnePageWidth == 0) {
            return
        }
        val currentItemPos = currentItem
        val offset = mCurrentItemOffset - (currentItemPos - mLastPos) * mOnePageWidth
        val percent =
            Math.max(Math.abs(offset) * 1.0 / mOnePageWidth, 0.0001).toFloat()

        var leftView: View? = null
        val currentView: View?
        var rightView: View? = null
        if (currentItemPos > 0) {
            leftView = mRecyclerView!!.layoutManager!!.findViewByPosition(currentItemPos - 1)
        }
        currentView = mRecyclerView!!.layoutManager!!.findViewByPosition(currentItemPos)
        if (currentItemPos < mRecyclerView!!.adapter!!.itemCount - 1) {
            rightView = mRecyclerView!!.layoutManager!!.findViewByPosition(currentItemPos + 1)
        }
        if (leftView != null) {
            leftView.scaleY = (1 - mScale) * percent + mScale
        }
        if (currentView != null) {
            currentView.scaleY = (mScale - 1) * percent + 1
        }
        if (rightView != null) {
            rightView.scaleY = (1 - mScale) * percent + mScale
        }
    }



    var currentItem: Int
        get() =
            mRecyclerView!!.layoutManager!!.getPosition(
                mLinearSnapHelper.findSnapView(
                    mRecyclerView!!.layoutManager
                )!!
            )
        set(item) {
            setCurrentItem(item, false)
        }

    fun setScale(scale: Float) {
        mScale = scale
    }

    fun setPagePadding(pagePadding: Int) {
        mPagePadding = pagePadding
    }

    fun setShowLeftCardWidth(showLeftCardWidth: Int) {
        mShowLeftCardWidth = showLeftCardWidth
    }


    private class CardLinearSnapHelper : LinearSnapHelper() {
        var mNoNeedToScroll = false
        var finalSnapDistance = intArrayOf(0, 0)
        override fun calculateDistanceToFinalSnap(
            layoutManager: RecyclerView.LayoutManager,
            targetView: View
        ): IntArray {
            //Log.e("TAG", "calculateDistanceToFinalSnap");
            if (mNoNeedToScroll) {
                finalSnapDistance[0] = 0
                finalSnapDistance[1] = 0
            } else {
                finalSnapDistance = super.calculateDistanceToFinalSnap(layoutManager, targetView)!!
            }
            return finalSnapDistance
        }
    }
}

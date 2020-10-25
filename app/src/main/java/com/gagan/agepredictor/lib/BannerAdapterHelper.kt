package com.gagan.agepredictor.lib


import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView


class BannerAdapterHelper {
    fun onCreateViewHolder(parent: ViewGroup, itemView: View) {
        val lp: RecyclerView.LayoutParams = itemView.layoutParams as RecyclerView.LayoutParams
        lp.width = parent.width - ScreenUtil.dip2px(
            itemView.context,
            (2 * (sPagePadding + sShowLeftCardWidth)).toFloat()
        )
        itemView.layoutParams = lp
    }

    fun onBindViewHolder(itemView: View, position: Int, itemCount: Int) {
        val padding: Int = ScreenUtil.dip2px(itemView.context, sPagePadding.toFloat())
        itemView.setPadding(padding, 0, padding, 0)
        val leftMarin = if (position == 0) padding + ScreenUtil.dip2px(
            itemView.context,
            sShowLeftCardWidth.toFloat()
        ) else 0
        val rightMarin = if (position == itemCount - 1) padding + ScreenUtil.dip2px(
            itemView.context,
            sShowLeftCardWidth.toFloat()
        ) else 0
        setViewMargin(itemView, leftMarin, 0, rightMarin, 0)
    }

    private fun setViewMargin(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        val lp = view.layoutParams as MarginLayoutParams
        if (lp.leftMargin != left || lp.topMargin != top || lp.rightMargin != right || lp.bottomMargin != bottom) {
            lp.setMargins(left, top, right, bottom)
            view.layoutParams = lp
        }
    }

    fun setPagePadding(pagePadding: Int) {
        sPagePadding = pagePadding
    }

    fun setShowLeftCardWidth(showLeftCardWidth: Int) {
        sShowLeftCardWidth = showLeftCardWidth
    }

    companion object {
        var sPagePadding = 15
        var sShowLeftCardWidth = 15
    }
}
package com.gagan.agepredictor.utils


import androidx.fragment.app.FragmentActivity
import com.gagan.agepredictor.MainActivity

class NavigationHelper {
    companion object {
        const val MAIN_FRAGMENT_TAG = "main_fragment_tag"



        fun showTitleAndBackButtonInFragment(activity: FragmentActivity, title: String) {
            activity.let {
                it.title = title
                (it as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
        }


    }
}
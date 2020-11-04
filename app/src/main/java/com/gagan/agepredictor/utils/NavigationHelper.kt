package com.gagan.agepredictor.utils


import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.gagan.agepredictor.MainActivity
import com.gagan.agepredictor.R
import com.gagan.agepredictor.fragments.MainFragment

class NavigationHelper {
    companion object {
        const val MAIN_FRAGMENT_TAG = "main_fragment_tag"

        fun gotoMainFragment(fragmentManager: FragmentManager) {
            val fragment = MainFragment()
            fragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, fragment, MAIN_FRAGMENT_TAG)
                commit()
            }
        }

        fun showTitleAndBackButtonInFragment(activity: FragmentActivity, title: String) {
            activity.let {
                it.title = title
                (it as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
        }

        fun closeFragment(fragmentManager: FragmentManager) {
            fragmentManager.popBackStackImmediate(
                MAIN_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
    }
}
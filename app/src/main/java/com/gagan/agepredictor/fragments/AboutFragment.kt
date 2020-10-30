package com.gagan.agepredictor.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gagan.agepredictor.R
import com.gagan.agepredictor.utils.ShareUtils.openUrlInBrowser
import com.gagan.agepredictor.databinding.FragmentAboutBinding

class AboutFragment: Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        val inflatedView = binding.root


        val  privacyPolicyLink= binding.privacyPolicyLink
        privacyPolicyLink.setOnClickListener {
            openUrlInBrowser(
                requireContext(),
                resources.getString(R.string.privacy_policy_url)
            )
        }
        binding.playstoreLink.setOnClickListener {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + requireContext().packageName)
                    )
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + requireContext().packageName)
                    )
                )
            }
        }

        return inflatedView

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

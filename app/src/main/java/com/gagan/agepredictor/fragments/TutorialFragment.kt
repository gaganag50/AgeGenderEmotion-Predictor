package com.gagan.agepredictor.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gagan.agepredictor.adapters.CardAdapter
import com.gagan.agepredictor.MainActivity.Companion.data
import com.gagan.agepredictor.databinding.FragmentTutorialBinding
import com.gagan.agepredictor.lib.BannerScaleHelper


class TutorialFragment : Fragment(), CardAdapter.onPhotoSelectedListener {
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var _binding: FragmentTutorialBinding? = null

    private val binding get() = _binding!!


    interface onItemSelectedListener {
        fun onItemSelected(position: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? onItemSelectedListener
        if (listener == null) {
            throw ClassCastException("$context must implement OnArticleSelectedListener")
        }
    }
    var listener: onItemSelectedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)

        // HERE LAMBDA WASN'T WORKING
        // GIVING ERROR TO CHANGE THE COMPLIER LANGUAGE SETTING TO 1.4 EVEN THOUGH THE VERSION WAS ALREADY 1.4.10
        viewAdapter = CardAdapter(data,this)

        binding.recyclerView.apply {

            layoutManager = viewManager

            adapter = viewAdapter

        }
        val mBannerScaleHelper = BannerScaleHelper()
        mBannerScaleHelper.setFirstItemPos(0)
        mBannerScaleHelper.attachToRecyclerView(binding.recyclerView)


    }

    override fun onPhotoSelected(position:Int) {
        listener?.onItemSelected(position)
    }



}
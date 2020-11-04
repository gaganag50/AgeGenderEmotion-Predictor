package com.gagan.agepredictor.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gagan.agepredictor.MainActivity
import com.gagan.agepredictor.MainActivity.Companion.data
import com.gagan.agepredictor.adapters.CardAdapter
import com.gagan.agepredictor.databinding.FragmentMainBinding
import com.yarolegovich.discretescrollview.DSVOrientation
import com.yarolegovich.discretescrollview.transform.ScaleTransformer


class MainFragment : Fragment(), CardAdapter.OnPhotoSelectedListener {


    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    interface OnItemSelectedListener {
        fun onItemSelected(position: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnItemSelectedListener
        if (listener == null) {
            throw ClassCastException("$context must implement OnArticleSelectedListener")
        }
    }
    private var listener: OnItemSelectedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        activity?.title = "AgePredictor"
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        return binding.root
    }
    

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // HERE LAMBDA WASN'T WORKING
        // GIVING ERROR TO CHANGE THE COMPLIER LANGUAGE SETTING TO 1.4 EVEN THOUGH THE VERSION WAS ALREADY 1.4.10

        binding.itemPicker.setOrientation(DSVOrientation.HORIZONTAL)
        binding.itemPicker.setItemTransformer(
            ScaleTransformer.Builder().setMinScale(0.8f).build()
        )
        binding.itemPicker.adapter = CardAdapter(data,this)

    }

    override fun onPhotoSelected(position:Int) {
        listener?.onItemSelected(position)

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
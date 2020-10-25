package com.gagan.agepredictor

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gagan.agepredictor.MainActivity.Companion.data
import com.gagan.agepredictor.databinding.FragmentTutorialBinding


class MasterFragment : Fragment(),MyAdapter.onPhotoSelectedListener {
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
//    private val model: SharedViewModel by activityViewModels()

    private var _binding: FragmentTutorialBinding? = null

    private val binding get() = _binding!!

    // Container Activity must implement this interface
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
        viewManager = LinearLayoutManager(view.context)

        // HERE LAMBDA WASN'T WORKING
        // GIVING ERROR TO CHANGE THE COMPLIER LANGUAGE SETTING TO 1.4 EVEN THOUGH THE VERSION WAS ALREADY 1.4.10
        viewAdapter = MyAdapter(data,this )









        binding.myRecyclerView.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter/*(data)*/

        }


    }

    override fun onPhotoSelected(position:Int) {
        listener?.onItemSelected(position = position)
    }

//    override fun onPhotoSelected(position: Int) {
//        listener?.onItemSelected(position)
//    }

}
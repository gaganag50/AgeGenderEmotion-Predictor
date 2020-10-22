package com.gagan.agepredictor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gagan.agepredictor.MainActivity.Companion.TAG
import com.gagan.agepredictor.MainActivity.Companion.data
import com.gagan.agepredictor.databinding.FragmentTutorialBinding


class TutorialFragment : Fragment() {
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var _binding: FragmentTutorialBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTutorialBinding.inflate(inflater, container, false)
        val view = binding.root

        viewManager = LinearLayoutManager(view.context)

        val mOnClickListener = { position: Int ->
            fragmentManager?.beginTransaction()
            val newFragment = DetailsFragment()
            val bundle = Bundle()
            bundle.putInt("position", position)
            newFragment.arguments = bundle



            val transaction = fragmentManager?.beginTransaction()
            transaction?.replace(R.id.fragment_container, newFragment)
            transaction?.addToBackStack(null)
            transaction?.commit()

            Log.d(TAG, "onCreateView: $position")
        }
        viewAdapter = MyAdapter(data, mOnClickListener)









        binding.myRecyclerView.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter/*(data)*/

        }
        return view
    }

}
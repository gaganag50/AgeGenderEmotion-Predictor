package com.gagan.agepredictor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.gagan.agepredictor.R
import com.gagan.agepredictor.databinding.ItemViewCardBinding


class CardAdapter(private val list: List<Int>, private val onClickListener: OnPhotoSelectedListener) :
    RecyclerView.Adapter<CardAdapter.ViewHolder>() {
    private var _binding: ItemViewCardBinding? = null
    private val binding get() = _binding!!

    interface OnPhotoSelectedListener{
        fun onPhotoSelected(position: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        _binding = ItemViewCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val itemView = binding.root
        return ViewHolder(itemView, onClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mImageView.setImageResource(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(
        itemView: View,
        private val onPhotoSelectedListener: OnPhotoSelectedListener
    ) : RecyclerView.ViewHolder(itemView),View.OnClickListener {
        val mImageView: ImageView = itemView.findViewById<View>(R.id.imageView) as ImageView
        init {
            itemView.setOnClickListener(this)
        }
        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onPhotoSelectedListener.onPhotoSelected(position)
            }
        }
    }



}

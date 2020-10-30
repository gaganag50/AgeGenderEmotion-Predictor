package com.gagan.agepredictor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.gagan.agepredictor.R
import com.gagan.agepredictor.databinding.ItemViewCardBinding
import com.gagan.agepredictor.lib.BannerAdapterHelper


class CardAdapter(val list: List<Int>,val onClickListener: onPhotoSelectedListener) :
    RecyclerView.Adapter<CardAdapter.Lodu>() {
    private var _binding: ItemViewCardBinding? = null
    private val binding get() = _binding!!

    private val mBannerAdapterHelper = BannerAdapterHelper()
    interface onPhotoSelectedListener{
        fun onPhotoSelected(position: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Lodu {
        _binding = ItemViewCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val itemView = binding.root
        mBannerAdapterHelper.onCreateViewHolder(parent, itemView)
        return Lodu(itemView,onClickListener)
    }

    override fun onBindViewHolder(holder: Lodu, position: Int) {
        mBannerAdapterHelper.onBindViewHolder(holder.itemView, position, itemCount)
        holder.mImageView.setImageResource(list[position % list.size])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class Lodu(itemView: View, val onPhotoSelectedListener: onPhotoSelectedListener) : RecyclerView.ViewHolder(itemView),View.OnClickListener {
        val mImageView: ImageView = itemView.findViewById<View>(R.id.imageView) as ImageView
        init {
            itemView.setOnClickListener(this)
        }
        override fun onClick(p0: View?) {
            onPhotoSelectedListener.onPhotoSelected(adapterPosition)
        }
    }



}

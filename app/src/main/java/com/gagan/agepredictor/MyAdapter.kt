package com.gagan.agepredictor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView


class MyAdapter(val data:List<Int>,val onClickListener: onPhotoSelectedListener) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    interface onPhotoSelectedListener{
        fun onPhotoSelected(position: Int)
    }
    override fun onCreateViewHolder(p: ViewGroup, v: Int): MyViewHolder {
        val itemLayoutView: View = LayoutInflater.from(p.context)
            .inflate(R.layout.fragment_item, p, false)

        return MyViewHolder(itemLayoutView,onClickListener)
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.imgViewIcon.setImageResource(data[position])
    }
    override fun getItemCount() =  data.size

    inner class MyViewHolder(itemView: View, val onPhotoSelectedListener: onPhotoSelectedListener) : RecyclerView.ViewHolder(itemView),View.OnClickListener {

        var imgViewIcon: ImageView = itemView.findViewById(R.id.image)
        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onPhotoSelectedListener.onPhotoSelected(adapterPosition)
        }
    }








}
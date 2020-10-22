package com.gagan.agepredictor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView


class MyAdapter(val data:List<Int>, val onClickListener: (Int) -> Int) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(p: ViewGroup, v: Int): MyViewHolder {
        val itemLayoutView: View = LayoutInflater.from(p.context)
            .inflate(R.layout.fragment_item, p, false)

        return MyViewHolder(itemLayoutView,onClickListener)
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.imgViewIcon.setImageResource(data[position])
    }
    override fun getItemCount() =  data.size

    inner class MyViewHolder(itemView: View, onClickListener: (Int) -> Int) : RecyclerView.ViewHolder(itemView) {

        var imgViewIcon: ImageView = itemView.findViewById(R.id.image)
        init {
            itemView.setOnClickListener{
                onClickListener(adapterPosition)
            }
        }
    }








}
package com.gagan.agepredictor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DetailsAdapter(val list: MutableList<ItemDetected>) :
    RecyclerView.Adapter<DetailsAdapter.Lodu>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Lodu {
        val itemView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.detail_list_item, parent, false)
        return Lodu(itemView)
    }

    override fun onBindViewHolder(holder: Lodu, position: Int) {
        holder.detail_age.text = list[position].ageBucket
        holder.detail_gender.text = list[position].gender
        holder.detail_emotion.text = list[position].emotion
    }

    override fun getItemCount(): Int {
        return list.size
    }
    class Lodu(itemView: View) : RecyclerView.ViewHolder(itemView){
        val detail_age: TextView = itemView.findViewById(R.id.detail_age)
        val detail_gender: TextView = itemView.findViewById(R.id.detail_gender)
        val detail_emotion: TextView = itemView.findViewById(R.id.detail_emotion)
    }

}

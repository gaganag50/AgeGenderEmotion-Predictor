package com.gagan.agepredictor

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gagan.agepredictor.MainActivity.Companion.TAG


class DetailsAdapter(private val informationExtracted: List<ItemDetected>,val onClickListener: onBlurFaceListener) :
    RecyclerView.Adapter<DetailsAdapter.DetailViewHolder>() {

    interface onBlurFaceListener{
        fun onBlurFace(position: Int)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {


        val individualFaceInformation = informationExtracted[position]


        holder.faceImage.setImageBitmap(individualFaceInformation.face)
        holder.title.text = (position + 1).toString()
        Log.d(
            TAG,
            "bindVH: ${individualFaceInformation.ageBucket} ${individualFaceInformation.gender} ${individualFaceInformation.emotion}"
        )
        holder.detailAge.text = "AGE: ${individualFaceInformation.ageBucket}"
        holder.detailGender.text = "GENDER: ${individualFaceInformation.gender}"
        holder.detailEmotion.text = "EMOTION: ${individualFaceInformation.emotion}"



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_card_content, parent, false)

        return DetailViewHolder(view,onClickListener)
    }

    override fun getItemCount() = informationExtracted.size


    class DetailViewHolder(view: View,val onBlurFaceListener: onBlurFaceListener) : RecyclerView.ViewHolder(view),View.OnClickListener {
        val faceImage = itemView.findViewById<ImageView>(R.id.individual_face)!!
        val title = itemView.findViewById<TextView>(R.id.face_title)!!
        val detailAge: TextView = itemView.findViewById(R.id.detail_age)!!
        val detailGender: TextView = itemView.findViewById(R.id.detail_gender)!!
        val detailEmotion: TextView = itemView.findViewById(R.id.detail_emotion)!!
        val blurFace = itemView.findViewById<TextView>(R.id.blur_face)!!
        init {
            blurFace.setOnClickListener(this)
        }
        override fun onClick(p0: View?) {
            onBlurFaceListener.onBlurFace(adapterPosition)
        }
    }


}


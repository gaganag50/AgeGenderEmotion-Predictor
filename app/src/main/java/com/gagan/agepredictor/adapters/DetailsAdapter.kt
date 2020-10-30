package com.gagan.agepredictor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gagan.agepredictor.appdata.ItemDetected
import com.gagan.agepredictor.MainActivity.Companion.emojis
import com.gagan.agepredictor.R
import com.gagan.agepredictor.databinding.ItemCardContentBinding


class DetailsAdapter(private val informationExtracted: List<ItemDetected>, val onClickListener: onBlurFaceListener) :
    RecyclerView.Adapter<DetailsAdapter.DetailViewHolder>() {
    private var _binding: ItemCardContentBinding? = null
    private val binding get() = _binding!!

    interface onBlurFaceListener{
        fun onBlurFace(position: Int)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {


        val individualFaceInformation = informationExtracted[position]


        holder.faceImage.setImageBitmap(individualFaceInformation.face)
        holder.title.text = (position + 1).toString()
        holder.detailAge.text = "AGE: ${individualFaceInformation.ageBucket}"
        holder.detailGender.text = "GENDER: ${individualFaceInformation.gender}"
        holder.detailEmotion.text = "EMOTION: ${individualFaceInformation.emotion}"
        holder.detailEmoji.setImageResource(emojis[individualFaceInformation.emotion]!!)


    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {

        _binding= ItemCardContentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        val view = binding.root
        return DetailViewHolder(view,onClickListener)
    }

    override fun getItemCount() = informationExtracted.size


    class DetailViewHolder(view: View,val onBlurFaceListener: onBlurFaceListener) : RecyclerView.ViewHolder(view),View.OnClickListener {
        val faceImage = itemView.findViewById<ImageView>(R.id.individual_face)!!
        val title = itemView.findViewById<TextView>(R.id.face_title)!!
        val detailAge: TextView = itemView.findViewById(R.id.detail_age)!!
        val detailGender: TextView = itemView.findViewById(R.id.detail_gender)!!
        val detailEmotion: TextView = itemView.findViewById(R.id.detail_emotion)!!
        val detailEmoji:ImageView=  itemView.findViewById(R.id.detail_emoji)!!
        val blurFace = itemView.findViewById<TextView>(R.id.blur_face)!!
        init {
            blurFace.setOnClickListener(this)
        }
        override fun onClick(p0: View?) {
            onBlurFaceListener.onBlurFace(adapterPosition)
        }
    }


}


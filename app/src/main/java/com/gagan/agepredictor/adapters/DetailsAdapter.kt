package com.gagan.agepredictor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.recyclerview.widget.RecyclerView
import com.gagan.agepredictor.appdata.ItemDetected
import com.gagan.agepredictor.MainActivity.Companion.emojis
import com.gagan.agepredictor.R
import com.gagan.agepredictor.databinding.ItemCardContentBinding


class DetailsAdapter(private val informationExtracted: List<ItemDetected>, private val onClickListener: OnBlurFaceListener) :
    RecyclerView.Adapter<DetailsAdapter.DetailViewHolder>() {
    private var _binding: ItemCardContentBinding? = null
    private val binding get() = _binding!!

    interface OnBlurFaceListener{
        fun onBlurFace(position: Int)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {


        val individualFaceInformation = informationExtracted[position]


        holder.faceImage.setImageBitmap(individualFaceInformation.face)
        holder.title.text = (position + 1).toString()
        val age = buildSpannedString {
            bold { append("AGE: ") }
            append(individualFaceInformation.ageBucket)
        }

        holder.detailAge.text = age
        val gender = buildSpannedString {
            bold { append("GENDER: ") }
            append(individualFaceInformation.gender)
        }

        holder.detailGender.text = gender

        val emotion = buildSpannedString {
            bold { append("EMOTION: ") }
            append(individualFaceInformation.emotion)
        }
        holder.detailEmotion.text = emotion
        val emotionEmoji = emojis.getValue(individualFaceInformation.emotion)
        holder.detailEmoji.setImageResource(emotionEmoji)


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


    class DetailViewHolder(view: View, private val onBlurFaceListener: OnBlurFaceListener) : RecyclerView.ViewHolder(view),View.OnClickListener {
        val faceImage = itemView.findViewById<ImageView>(R.id.individual_face)!!
        val title = itemView.findViewById<TextView>(R.id.face_title)!!
        val detailAge: TextView = itemView.findViewById(R.id.detail_age)!!
        val detailGender: TextView = itemView.findViewById(R.id.detail_gender)!!
        val detailEmotion: TextView = itemView.findViewById(R.id.detail_emotion)!!
        val detailEmoji:ImageView=  itemView.findViewById(R.id.detail_emoji)!!
        private val blurFace = itemView.findViewById<TextView>(R.id.blur_face)!!
        init {
            blurFace.setOnClickListener(this)
        }
        override fun onClick(p0: View?) {
            onBlurFaceListener.onBlurFace(adapterPosition)
        }
    }


}


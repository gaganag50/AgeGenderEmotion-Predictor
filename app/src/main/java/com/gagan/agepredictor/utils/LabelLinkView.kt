package com.gagan.agepredictor.utils


import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.gagan.agepredictor.R


class LabelLinkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val leadingImageView: AppCompatImageView
    private val labelTextView: AppCompatTextView
    private val trailingImageView: AppCompatImageView

    private var leadingIcon: Drawable? = null
        set(value) {
            if (value == null) {
                leadingImageView.visibility = View.GONE
            } else {
                leadingImageView.setImageDrawable(value)
                leadingImageView.visibility = View.VISIBLE
            }
            field = value
        }

    private var label: String = ""
        set(value) {
            labelTextView.text = value
            field = value
        }

    private var linkUrl: String = ""

    private val onLinkClickedListener = OnClickListener {
        if (linkUrl.isBlank()) return@OnClickListener

        launchUrl(linkUrl)
    }

    init {
        clipToPadding = false
        orientation = HORIZONTAL

        val view = View.inflate(context, R.layout.label_view_layout, this)
        leadingImageView = view.findViewById(R.id.label_leading_image_view)
        labelTextView = view.findViewById(R.id.label_text_view)
        trailingImageView = view.findViewById(R.id.label_trailing_image_view)
        trailingImageView.setOnClickListener(onLinkClickedListener)

        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LabelLinkView,
            defStyleAttr,
            defStyleRes
        )
        leadingIcon = a.getDrawable(R.styleable.LabelLinkView_leadingIcon)
        label = a.getString(R.styleable.LabelLinkView_android_text) ?: label
        linkUrl = a.getString(R.styleable.LabelLinkView_linkUrl) ?: linkUrl
        a.recycle()
    }

    private fun launchUrl(urlString: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlString)))
    }
}
package com.gagan.agepredictor.fragments

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gagan.agepredictor.Classifier
import com.gagan.agepredictor.appdata.InfoExtracted
import com.gagan.agepredictor.appdata.ItemDetected
import com.gagan.agepredictor.MainActivity.Companion.data
import com.gagan.agepredictor.adapters.DetailsAdapter
import com.gagan.agepredictor.databinding.FragmentDetailsBinding
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


class DetailsFragment : Fragment(), DetailsAdapter.onBlurFaceListener {
    private lateinit var viewManager: RecyclerView.LayoutManager

    private val itemDetectedList: MutableList<ItemDetected> = mutableListOf()
    private var infoExtractedList: List<InfoExtracted> = listOf()
    private var viewAdapter: DetailsAdapter? = null

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var mSelectedImage: Bitmap
    private lateinit var frame: Mat
    private val classifier: Classifier by activityViewModels()

    @Throws(IOException::class)
    fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun emotionModelInitialization(): ByteBuffer {
        val assetManager = requireContext().assets
        return loadModelFile(assetManager, "mnist.tflite")
    }

    private fun ageModelInitialization(): ByteBuffer {
        val assetManager = requireContext().assets
        return loadModelFile(assetManager, "age_gender.tflite")
    }
    private fun genderModelInitialization(): ByteBuffer {
        val assetManager = requireContext().assets
        return loadModelFile(assetManager, "gender.tflite")
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        classifier.isProcessing.observe(viewLifecycleOwner, {
            when {
                it -> binding.progressBar.visibility = View.VISIBLE
                else -> binding.progressBar.visibility = View.GONE
            }
        })
        binding.findFaces.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {

//                val ageGenderModelInitilaziton = ageGenderModelInitilaziton()
                val emotionModelInitialization = emotionModelInitialization()
                val ageModelInitialization = ageModelInitialization()
                val genderModelInitialization = genderModelInitialization()
//                Log.d(TAG, "onViewCreated: ageModelInitialization: ${ageModelInitialization}")
//                Log.d(TAG, "onViewCreated: genderModelInitialization: ${genderModelInitialization}")
//                Log.d(TAG, "onViewCreated: emotionModelInitialization: ${emotionModelInitialization}")
                classifier.runFaceContourDetection(
                    mSelectedImage,
                    frame,
                    ageModelInitialization,
                    genderModelInitialization,
                    emotionModelInitialization
                )

            }
        }



        classifier.infoRegardingFaces.observe(viewLifecycleOwner, { boundsList ->
            itemDetectedList.clear()
            infoExtractedList = boundsList
            showToast("${boundsList.size} faces detected")
            for (items in boundsList) {
                val bounds = items.rect
                val left = bounds.left
                val top = bounds.top
                val right = bounds.right
                val bottom = bounds.bottom

                val rectCrop = Rect(
                    Point(left.toDouble(), top.toDouble()),
                    Point(right.toDouble(), bottom.toDouble())
                )
                val croppedFace = Mat(frame, rectCrop)
                val faceBitmap = Bitmap.createBitmap(
                    croppedFace.width(),
                    croppedFace.height(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(croppedFace, faceBitmap)
                Imgproc.rectangle(
                    frame,
                    Point(left.toDouble(), top.toDouble()),
                    Point(right.toDouble(), bottom.toDouble()),
                    Scalar(0.0, 255.0, 0.0),
                    3
                )


                val age = items.ageBucket
                val gender = items.gender
                val emotion = items.emotion


                itemDetectedList.add(ItemDetected(faceBitmap, age, gender, emotion))


            }

            Utils.matToBitmap(frame, mSelectedImage)

            binding.imageView.setImageBitmap(mSelectedImage)
            viewAdapter?.notifyDataSetChanged()
        })

        viewManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        viewAdapter = DetailsAdapter(itemDetectedList, this)

        binding.recyclerView.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

    }

//    private fun setTextInTextView(list: MutableList<ItemDetected>) {
//        var displayText: String = String()
//        var displayList: MutableList<String> = mutableListOf()
//
//        for (faceDetails in list) {
//            val age = faceDetails.ageBucket
//            val gender = faceDetails.gender
//            val emo = faceDetails.emotion
//            var faceString = String()
//            if (age != null) {
//                faceString += "\nAGE: $age"
//            }
//            if (gender != null) {
//                faceString += "\nGENDER: $gender"
//            }
//            if (emo != null) {
//                faceString += "\nEMOTION: $emo"
//            }
//
//            displayList.add(faceString)
//        }
////        if(displayList.isNotEmpty()) {
////            displayList = displayList.drop(1) as MutableList<String>
////        }
////        for (item in displayList) {
////            displayText += item
////        }
////        binding.details.text = displayText
//    }

    private fun showToast(message: String) =
        Toast.makeText(activity?.applicationContext, message, Toast.LENGTH_SHORT).show()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        val view = binding.root


        val filePath = arguments?.getString("filePath")


        if (filePath == null) {
            val position = arguments?.getInt("position")
            if (position == null) {
                showToast("UnRecognized command")
                activity?.onBackPressed()
            } else {
//                Log.d(TAG, "onCreateView: filePath is  NULL")
                mSelectedImage = BitmapFactory.decodeResource(
                    context?.resources,
                    data[position]
                )

            }
        } else {
            mSelectedImage = BitmapFactory.decodeFile(filePath)
        }


        binding.imageView.setImageBitmap(mSelectedImage)
        frame = Mat()
        Utils.bitmapToMat(mSelectedImage, frame)
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)


        return view
    }

    override fun onDestroy() {
        classifier.close()
        super.onDestroy()
    }

    private fun getPath(file: String, context: Context): String {
        val assetManager: AssetManager = context.assets
        val inputStream: BufferedInputStream?
        try {
            // Read data from assets.
            inputStream = BufferedInputStream(assetManager.open(file))
            val data = ByteArray(inputStream.available())
            inputStream.read(data)
            inputStream.close()
            // Create copy file in storage.
            val outFile = File(context.filesDir, file)
            val os = FileOutputStream(outFile)
            os.write(data)
            os.close()
            // Return a path to file which may be read in common way.
            return outFile.absolutePath
        } catch (ex: IOException) {
//            Log.i(TAG, "Failed to upload a file")
        }
        return ""
    }

    override fun onBlurFace(position: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            classifier.anonymizeFaceSimple(frame, position)
        }
        Utils.matToBitmap(frame, mSelectedImage)
        binding.imageView.setImageBitmap(mSelectedImage)
    }

}

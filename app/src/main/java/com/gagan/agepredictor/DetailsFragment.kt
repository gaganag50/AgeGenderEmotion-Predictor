package com.gagan.agepredictor

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.gagan.agepredictor.MainActivity.Companion.TAG
import com.gagan.agepredictor.MainActivity.Companion.data
import com.gagan.agepredictor.databinding.DetailsFragmentBinding
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Core.FONT_HERSHEY_SIMPLEX
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


class DetailsFragment : Fragment() {
    private val informationExtracted: MutableList<ItemDetected> = mutableListOf()
    private var viewAdapter: DetailsAdapter? = null

    private var _binding: DetailsFragmentBinding? = null
    private val binding get() = _binding!!
    lateinit var mSelectedImage: Bitmap
    lateinit var frame: Mat
    private val classifier: Classifier by activityViewModels()


    private lateinit var ageNet: Net
    private lateinit var genderNet: Net

    fun draw_label(
        image: Mat, point: Point, label: String, font: Int = FONT_HERSHEY_SIMPLEX,
        font_scale: Double = 0.8, thickness: Int = 1
    ) {
        val baseLine = IntArray(1)


        val size = Imgproc.getTextSize(label, font, font_scale, thickness, baseLine)
        val (x, y) = Pair(point.x, point.y)
        Imgproc.rectangle(
            image,
            Point(x, y - size.height),
            Point(x + size.width, y + baseLine[0]),
            Scalar(255.0, 0.0, 0.0),
            -1
        )

        Imgproc.putText(
            image,
            label,
            point,
            font,
            font_scale,
            Scalar(255.0, 255.0, 255.0), thickness, Imgproc.LINE_AA
        )
    }

    @Throws(IOException::class)
    fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun emotionModelInitialization(): ByteBuffer {
        val assetManager = requireContext().assets
        val model = loadModelFile(assetManager, "mnist.tflite")
        return model
    }

    fun ageGenderModelInitilaziton(): Pair<Net, Net> {
        val protoAge: String = getPath("age_deploy.prototxt", requireContext())
        val weightsAge: String = getPath("age_net.caffemodel", requireContext())
        ageNet = Dnn.readNetFromCaffe(protoAge, weightsAge)
        Log.d(TAG, "onCameraViewStarted: ageNet $ageNet")


        val protoGender: String = getPath("gender_deploy.prototxt", requireContext())
        val weightsGender: String = getPath("gender_net.caffemodel", requireContext())
        genderNet = Dnn.readNetFromCaffe(protoGender, weightsGender)
        Log.d(TAG, "onCameraViewStarted: genderNet $genderNet")

        return Pair(ageNet, genderNet)

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        binding.findFaces.setOnClickListener {
            val ageGenderModelInitilaziton = ageGenderModelInitilaziton()
            val emotionModelInitialization = emotionModelInitialization()
            classifier.runFaceContourDetection(
                mSelectedImage,
                frame,
                ageGenderModelInitilaziton,
                emotionModelInitialization
            )
        }



        classifier.infoRegardingFaces.observe(viewLifecycleOwner, { boundsList ->
            informationExtracted.clear()
            showToast("${boundsList.size} faces detected")
            for ((index, items) in boundsList.withIndex()) {
                val bounds = items.rect!!
                val left = bounds.left
                val top = bounds.top
                val right = bounds.right
                val bottom = bounds.bottom



                Imgproc.rectangle(
                    frame,
                    Point(left.toDouble(), top.toDouble()),
                    Point(right.toDouble(), bottom.toDouble()),
                    Scalar(0.0, 255.0, 0.0),
                    3
                )

                draw_label(frame, Point(left.toDouble(), top.toDouble()), (index + 1).toString())
                val age = items.ageBucket
                val gender = items.gender
                val emotion = items.emotion
                informationExtracted.add(ItemDetected(null, age, gender, emotion))


            }

            val displayBitmap = mSelectedImage
            Utils.matToBitmap(frame, displayBitmap)
            binding.imageView.setImageBitmap(displayBitmap)
            viewAdapter?.notifyDataSetChanged()
        })


        viewAdapter = DetailsAdapter(informationExtracted)

        binding.details.apply {


            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
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
        _binding = DetailsFragmentBinding.inflate(inflater, container, false)
        val view = binding.root



        val filePath = arguments?.getString("filePath")


        if (filePath == null) {
            val position = arguments?.getInt("position")
            if (position == null) {
                showToast("UnRecognized command")
                activity?.onBackPressed()
            } else {
                Log.d(TAG, "onCreateView: filePath is  NULL")
                mSelectedImage = BitmapFactory.decodeResource(
                    context?.resources,
                    data[position])

            }
        } else{
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
            Log.i(TAG, "Failed to upload a file")
        }
        return ""
    }

}

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
    private val list: MutableList<ItemDetected> = mutableListOf()
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



        val size = Imgproc.getTextSize(label, font, font_scale, thickness,baseLine)
        val (x, y) = Pair(point.x, point.y)
        Imgproc.rectangle(
            image,
            Point(x, y - size.height),
            Point(x + size.width, y+baseLine[0]),
            Scalar(255.0, 0.0, 0.0),
            -1
        )

        Imgproc.putText(
            image,
            label,
            point,
            font,
            font_scale,
            Scalar(255.0,255.0,255.0), thickness, Imgproc.LINE_AA
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


//        executorService.execute {
//            initializeInterpreter()
//
//        }


        binding.buttonDetectFaces.setOnClickListener {
            classifier.runFaceContourDetection(mSelectedImage, frame)
        }
        binding.buttonAgeGender.setOnClickListener {
            val ageGenderModelInitilaziton = ageGenderModelInitilaziton()
            classifier.detectAgeGender(ageGenderModelInitilaziton)
        }
        binding.emotion.setOnClickListener {
            val emotionModelInitialization = emotionModelInitialization()
            classifier.detectEmotion(emotionModelInitialization)
        }
        binding.blurFaces.setOnClickListener {

        }
//        classifier.faceDetectionProcessing.observe(viewLifecycleOwner, {
//            if (it) {
//                Log.d(TAG, "onViewCreated: complete")
//            } else {
//                Log.d(TAG, "onViewCreated: NOT complete")
//            }
//
//        })


        classifier.faceDetected.observe(viewLifecycleOwner, { boundsList ->
            for ((index, bounds) in boundsList.withIndex()) {
                val left = bounds.left
                val top = bounds.top
                val right = bounds.right
                val bottom = bounds.bottom



                Imgproc.rectangle(
                    frame,
                    Point(left.toDouble(), top.toDouble()),
                    Point(right.toDouble(), bottom.toDouble()),
                    Scalar(0.0, 255.0, 0.0), 3
                )

                draw_label(frame,Point(left.toDouble(), top.toDouble()),(index + 1).toString())



                binding.details.text = "${boundsList.size} faces detected"
                list.clear()
                for (i in 0 until boundsList.size) {
                    val item = ItemDetected(null, null, null)
                    list.add(item)
                }

            }
            val displayBitmap = mSelectedImage
            Utils.matToBitmap(frame, displayBitmap)
            binding.imageView.setImageBitmap(displayBitmap)
        })

        classifier.age.observe(viewLifecycleOwner, { detectedage ->
            for ((index, age) in detectedage.withIndex()) {
                Log.d(TAG, "onViewCreated: age = ${age.first} confidence = ${age.second}")
                list[index].ageBucket = age.first
            }

            setTextInTextView(list)
        })
        classifier.gender.observe(viewLifecycleOwner, {
            Log.d(TAG, "onViewCreated: ${it.size}")
            for ((index, gender) in it.withIndex()) {
                Log.d(TAG, "onViewCreated: gender = ${gender.first} conf = ${gender.second}")
                list[index].gender = gender.first
            }
            setTextInTextView(list)
        })
        classifier.emotion.observe(viewLifecycleOwner, {
            for ((index, emo) in it.withIndex()) {
                Log.d(TAG, "onViewCreated: emo = ${emo.first} conf = ${emo.second}")
                list[index].emotion = emo.first
            }
            setTextInTextView(list)
        })
    }

    private fun setTextInTextView(list: MutableList<ItemDetected>) {
        var displayText: String = String()
        var displayList: MutableList<String> = mutableListOf()

        for (faceDetails in list) {
            val age = faceDetails.ageBucket
            val gender = faceDetails.gender
            val emo = faceDetails.emotion
            var faceString = String()
            if (age != null) {
                faceString += "\nAGE: $age"
            }
            if (gender != null) {
                faceString += "\nGENDER: $gender"
            }
            if (emo != null) {
                faceString += "\nEMOTION: $emo"
            }

            displayList.add(faceString)
        }
//        if(displayList.isNotEmpty()) {
//            displayList = displayList.drop(1) as MutableList<String>
//        }
        for (item in displayList) {
            displayText += item
        }
        binding.details.text = displayText
    }

    private fun showToast(message: String) =
        Toast.makeText(activity?.applicationContext, message, Toast.LENGTH_SHORT).show()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DetailsFragmentBinding.inflate(inflater, container, false)
        val view = binding.root
        val position = arguments?.getInt("position")
        if (position == null) {
            showToast("UnRecognized command")
            activity?.onBackPressed()
        } else {
            mSelectedImage = BitmapFactory.decodeResource(
                context?.resources,
                data[position]
            )
//            Log.d(
//                TAG,
//                "onCreateView: ${mSelectedImage.width} ${mSelectedImage.height} ${mSelectedImage.colorSpace}"
//            )
            binding.imageView.setImageBitmap(mSelectedImage)
            frame = Mat()
            Utils.bitmapToMat(mSelectedImage, frame)
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)
        }


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
            Log.i(MainActivity.TAG, "Failed to upload a file")
        }
        return ""
    }

}

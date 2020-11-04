package com.gagan.agepredictor.fragments

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gagan.agepredictor.AgePredictionApplication.Companion.TAG
import com.gagan.agepredictor.Classifier
import com.gagan.agepredictor.ImageUtils
import com.gagan.agepredictor.MainActivity
import com.gagan.agepredictor.MainActivity.Companion.data
import com.gagan.agepredictor.R
import com.gagan.agepredictor.adapters.DetailsAdapter
import com.gagan.agepredictor.appdata.InfoExtracted
import com.gagan.agepredictor.appdata.ItemDetected
import com.gagan.agepredictor.databinding.FragmentDetailsBinding
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*


class DetailsFragment : Fragment(), DetailsAdapter.OnBlurFaceListener {
    private lateinit var viewManager: RecyclerView.LayoutManager

    private val itemDetectedList: MutableList<ItemDetected> = mutableListOf()
    private var infoExtractedList: List<InfoExtracted> = listOf()
    private var viewAdapter: DetailsAdapter? = null

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var mSelectedImage: Bitmap
    private lateinit var frame: Mat
    private val classifier: Classifier = Classifier()

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

                val emotionModelInitialization = emotionModelInitialization()
                val ageModelInitialization = ageModelInitialization()
                val genderModelInitialization = genderModelInitialization()

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
            Log.d(TAG, "onViewCreated: $boundsList")
            if(boundsList.isNullOrEmpty()){
                Toasty.success(requireContext(),"No face detected",Toast.LENGTH_SHORT,true).show()
            }else {
                Toasty.success(requireContext(),"${boundsList.size} faces detected",Toast.LENGTH_SHORT,true).show()
            }
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




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
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
                Toasty.error(requireContext(),"UnRecognized command",Toast.LENGTH_SHORT,true).show()
                activity?.onBackPressed()
            } else {
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
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        classifier.close()
        super.onDestroy()
    }

    override fun onBlurFace(position: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            classifier.anonymizeFaceSimple(frame, position)
        }
        Utils.matToBitmap(frame, mSelectedImage)
        binding.imageView.setImageBitmap(mSelectedImage)
    }
    private fun saveCartoon(): String {

        val cartoonBitmap = mSelectedImage
        val file = File(
            MainActivity.getOutputDirectory(requireContext()),
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + "_cartoon.jpg")

        ImageUtils.saveBitmap(cartoonBitmap, file)
        context?.let {
            Toasty.success(it, "saved to " + file.absolutePath.toString(), Toast.LENGTH_SHORT,true)
                .show()
        }

        return file.absolutePath

    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_details, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> saveCartoon()
        }
        return super.onOptionsItemSelected(item)
    }
    companion object{
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    }

}

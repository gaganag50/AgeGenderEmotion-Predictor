package com.gagan.agepredictor.fragments

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gagan.agepredictor.Classifier
import com.gagan.agepredictor.MainActivity
import com.gagan.agepredictor.MainActivity.Companion.TAG
import com.gagan.agepredictor.MainActivity.Companion.data
import com.gagan.agepredictor.R
import com.gagan.agepredictor.adapters.DetailsAdapter
import com.gagan.agepredictor.appdata.InfoExtracted
import com.gagan.agepredictor.appdata.ItemDetected
import com.gagan.agepredictor.databinding.FragmentDetailsBinding
import com.gagan.agepredictor.ml.WhiteboxCartoonGanDr
import com.gagan.agepredictor.utils.ImageUtils
import com.gagan.agepredictor.utils.NavigationHelper
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*


class DetailsFragment : Fragment(), DetailsAdapter.OnBlurFaceListener {


    private var infoExtractedList: List<InfoExtracted> = listOf()
    lateinit var model: WhiteboxCartoonGanDr
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

    private fun updateUI() {
        classifier.isProcessing.observe(viewLifecycleOwner, {
            when {
                it -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.findFaces.isClickable = false
                    binding.cartoonize.isClickable = false
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.findFaces.isClickable = true
                    binding.cartoonize.isClickable = true
                }
            }
        })
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val emotionModelInitialization = emotionModelInitialization()
        val ageModelInitialization = ageModelInitialization()
        val genderModelInitialization = genderModelInitialization()

        model = WhiteboxCartoonGanDr.newInstance(requireContext())
        val viewAdapter = DetailsAdapter(this)



        binding.recyclerView.apply {
            layoutManager =  LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
            adapter = viewAdapter
        }

        updateUI()

        binding.cartoonize.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
                val (outputBitmap, inferenceTime) = classifier.cartoonizeImageAsync(
                    mSelectedImage,
                    model
                ).await()
                mSelectedImage = Bitmap.createScaledBitmap(
                    outputBitmap,
                    mSelectedImage.width,
                    mSelectedImage.height, true
                )
                binding.imageView.setImageBitmap(mSelectedImage)
                Toasty.success(
                    requireContext(),
                    "Cartoonized in $inferenceTime ms",
                    Toast.LENGTH_SHORT,
                    true
                ).show()

                viewAdapter.submitList(listOf())
            }
        }
        binding.findFaces.setOnClickListener {
            frame = Mat()
            Utils.bitmapToMat(mSelectedImage, frame)
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)

            classifier.runFaceContourDetection(
                mSelectedImage,
                frame,
                ageModelInitialization,
                genderModelInitialization,
                emotionModelInitialization,

                )
        }



        classifier.infoRegardingFaces.observe(viewLifecycleOwner, { boundsList ->

            infoExtractedList = boundsList
            if (boundsList.isNullOrEmpty()) {
                Toasty.success(requireContext(), "No face detected", Toast.LENGTH_SHORT, true)
                    .show()
                viewAdapter.submitList(listOf())
            } else {
                Toasty.success(
                    requireContext(),
                    "${boundsList.size} faces detected",
                    Toast.LENGTH_SHORT,
                    true
                ).show()
            }
            
            val itemDetectedList: MutableList<ItemDetected> = mutableListOf()
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
//            Log.d(TAG, "onViewCreated: ${itemDetectedList.size}")
            viewAdapter.submitList(itemDetectedList)
        })


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
        activity?.let { NavigationHelper.showTitleAndBackButtonInFragment(it, "Details") }
        val filePath = arguments?.getString("filePath")
        if (filePath == null) {
            val position = arguments?.getInt("position")
            if (position == null) {
                Toasty.error(requireContext(), "UnRecognized command", Toast.LENGTH_SHORT, true)
                    .show()
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



        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        classifier.close()
        model.close()
        super.onDestroy()
    }

    override fun onBlurFace(position: Int) {
        viewLifecycleOwner.lifecycleScope.launch {

            classifier.anonymizeFaceSimple(frame, position)
            Utils.matToBitmap(frame, mSelectedImage)
            binding.imageView.setImageBitmap(mSelectedImage)
        }


    }

    private fun saveBitmap(): String {

        val cartoonBitmap = mSelectedImage
        val file = File(
            MainActivity.getOutputDirectory(requireContext()),
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + "_cartoon.jpg"
        )

        ImageUtils.saveBitmap(cartoonBitmap, file)
        context?.let {
            Toasty.success(it, "saved to " + file.absolutePath.toString(), Toast.LENGTH_SHORT, true)
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
            android.R.id.home -> {
                activity?.onBackPressed()

            }
            R.id.action_save -> saveBitmap()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    }

}
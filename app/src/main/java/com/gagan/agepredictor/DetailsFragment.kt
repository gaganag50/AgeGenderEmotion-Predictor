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
import com.gagan.agepredictor.MainActivity.Companion.TAG
import com.gagan.agepredictor.MainActivity.Companion.data
import com.gagan.agepredictor.databinding.DetailsFragmentBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


class DetailsFragment : Fragment() {
    private var _binding: DetailsFragmentBinding? = null
    private val binding get() = _binding!!

    private var interpreter: Interpreter? = null
    var isInitialized = false
        private set


    private var inputImageWidth: Int = 0 // will be inferred from TF Lite model.
    private var inputImageHeight: Int = 0 // will be inferred from TF Lite model.
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model.
    lateinit var mSelectedImage: Bitmap


    private lateinit var ageNet: Net
    private lateinit var genderNet: Net
    val frame = Mat()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFace.setOnClickListener {
            runFaceContourDetection()
        }
        initialize()
        initializeInterpreter()
    }

    override fun onDestroy() {
        interpreter?.close()
        super.onDestroy()
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in pixels) {
            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)

            // Convert RGB to grayscale and normalize pixel value to [0..1].
            val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
            byteBuffer.putFloat(normalizedPixelValue)
        }

        return byteBuffer
    }

    fun initialize() {
        val protoAge: String = getPath("age_deploy.prototxt", context!!)
        val weightsAge: String = getPath("age_net.caffemodel", context!!)
        ageNet = Dnn.readNetFromCaffe(protoAge, weightsAge)
        Log.d(TAG, "onCameraViewStarted: ageNet $ageNet")


        val protoGender: String = getPath("gender_deploy.prototxt", context!!)
        val weightsGender: String = getPath("gender_net.caffemodel", context!!)
        genderNet = Dnn.readNetFromCaffe(protoGender, weightsGender)
        Log.d(TAG, "onCameraViewStarted: genderNet $genderNet")


    }

    private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    private fun initializeInterpreter() {

        val assetManager = context?.applicationContext?.assets
        val model = assetManager?.let { loadModelFile(it, "mnist.tflite") }

        val interpreter = Interpreter(model!!)
        // Read input shape from model file.
        val inputShape = interpreter.getInputTensor(0).shape()
        Log.d(TAG, "initializeInterpreter: inputShape ${inputShape.size}")
        inputShape.forEach { Log.d(TAG, "initializeInterpreter: inputShape ${it}") }
        // 48*48 gray scale
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE

        // Finish interpreter initialization.
        this.interpreter = interpreter

        isInitialized = true
        Log.d(TAG, "Initialized TFLite interpreter.")
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




        val position = arguments!!.getInt("position")

        mSelectedImage = BitmapFactory.decodeResource(
            context!!.resources,
            data[position]
        )
        Utils.bitmapToMat(mSelectedImage, frame)
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)

        return view
    }


    private fun runFaceContourDetection() {
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()


        val image = InputImage.fromBitmap(mSelectedImage, 0)
        val detector = FaceDetection.getClient(highAccuracyOpts)
        detector.process(image)
            .addOnSuccessListener { faces ->
                processFaceContourDetectionResult(faces)
            }
            .addOnFailureListener { e ->
                e.message?.let { showToast(it) }
            }
    }


    private fun processing(bitmap: Bitmap): Pair<String,Float> {
        Log.d(
            TAG,
            "classify: bitmap $bitmap inputImageWidth ${inputImageWidth} inputImageHeight ${inputImageHeight}"
        )
        val resizedImage = Bitmap.createScaledBitmap(
            bitmap,
            inputImageWidth,
            inputImageHeight,
            true
        )
        Log.d(TAG, "classify: bitmap.colorSpace ${bitmap.colorSpace}")
        val byteBuffer = convertBitmapToByteBuffer(resizedImage)

        // Define an array to store the model output.
        val output = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT) }

        // Run inference with the input data.
        interpreter?.run(byteBuffer, output)

        // Post-processing: find the digit that has the highest probability
        // and return it a human-readable string.
        val result = output[0]

        val maxIndex = result.indices.maxBy { result[it] } ?: -1
        return Pair(listOf[maxIndex],result[maxIndex])

    }

    private fun processFaceContourDetectionResult(faces: List<Face>) {
        if (faces.isEmpty()) {
            showToast("No face found")
            return
        }

        for (face in faces) {
            val bounds = face.boundingBox
            val left = bounds.left
            val top = bounds.top
            val right = bounds.right
            val bottom = bounds.bottom
            Imgproc.rectangle(
                frame,
                Point(left.toDouble(), top.toDouble()),
                Point(right.toDouble(), bottom.toDouble()),
                Scalar(0.0, 255.0, 0.0)
            )

            val rectCrop = Rect(
                Point(left.toDouble(), top.toDouble()),
                Point(right.toDouble(), bottom.toDouble())
            )
            val croppedFace = Mat(frame, rectCrop)


            val faceBlob = Dnn.blobFromImage(
                croppedFace, 1.0,
                Size(227.0, 227.0),
                Scalar(78.4263377603, 87.7689143744, 114.895847746), false
            )

            genderNet.setInput(faceBlob)
            val genderPreds: Mat = genderNet.forward()
            var index = 0
            var max = 0.0
            for (j in 0 until genderPreds.cols()) {
                val d = genderPreds.get(0, j)[0]
                if (max < d) {
                    max = d
                    index = j
                }
            }
            val gender = genderList[index]
            val genderConf = max
            Log.d(TAG, "onCameraViewStarted: genderConf ${genderConf}")


            ageNet.setInput(faceBlob)
            val preds: Mat = ageNet.forward()
            val c = preds.cols()
            index = 0
            max = 0.0
            for (j in 0 until c) {
                val d = preds.get(0, j)[0]
                if (max < d) {
                    max = d
                    index = j
                }
            }


            val age = AGE_BUCKETS[index]
            val ageConfidence = max
            Log.d(TAG, "onCameraViewStarted: ageConfidence ${ageConfidence}")














            if (!isInitialized) {
                showToast("TF Lite Interpreter is not initialized yet.")
            } else {
                val b: Bitmap = Bitmap.createBitmap(
                    croppedFace.width(),
                    croppedFace.height(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(croppedFace, b)
                val emotionResult = processing(b)
                val label: String =  emotionResult.first+ "," + gender + "," + age
                Log.d(TAG, "processFaceContourDetectionResult: label ${emotionResult.second}")

                val baseLine = IntArray(1)
                val labelSize: Size =
                    Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine)
                // Draw background for label.
                Imgproc.rectangle(
                    frame, Point(left.toDouble(), top - labelSize.height),
                    Point(left + labelSize.width, (top + baseLine[0]).toDouble()),
                    Scalar(255.0, 255.0, 255.0), -1
                )
                // Write class name and confidence.
                Imgproc.putText(
                    frame, label, Point(left.toDouble(), top.toDouble()),
                    Core.FONT_HERSHEY_SIMPLEX, 0.5, Scalar(0.0, 0.0, 0.0)
                )
                Utils.matToBitmap(frame,mSelectedImage)
                binding.imageView.setImageBitmap(mSelectedImage)
            }
        }

    }

    companion object {
        private const val FLOAT_TYPE_SIZE = 4
        private const val PIXEL_SIZE = 1
        private const val OUTPUT_CLASSES_COUNT = 6

        val AGE_BUCKETS = listOf<String>(
            "(0-2)", "(4-6)", "(8-12)", "(15-20)", "(25-32)",
            "(38-43)", "(48-53)", "(60-100)"
        )
        val listOf = listOf("Angry", "Fear", "Happy", "Neutral", "Sad", "Surprise")
        val genderList = listOf("M", "F")
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
}

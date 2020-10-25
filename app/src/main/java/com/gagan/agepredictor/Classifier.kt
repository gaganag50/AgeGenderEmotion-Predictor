package com.gagan.agepredictor

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.gagan.agepredictor.ext.maxProb
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class Classifier : ViewModel() {


    val infoRegardingFaces = SingleLiveEvent<List<InfoExtracted>>()
    val isProcessing = SingleLiveEvent<Boolean>()
    val boundingBoxes: MutableList<android.graphics.Rect> = mutableListOf()
    private var interpreter: Interpreter? = null

    private var isInitialized = false
        private set

    private var inputImageWidth: Int = 0 // will be inferred from TF Lite model.
    private var inputImageHeight: Int = 0 // will be inferred from TF Lite model.
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model.


    private fun processDNN(
        net: Net,
        faceBlob: Mat,
        allowedValues: List<String>
    ): Pair<String, Double> {
        net.setInput(faceBlob)
        val predictions: Mat = net.forward()
        val (index, max) = predictions.maxProb()
        return Pair(allowedValues[index], max)
    }

    fun runFaceContourDetection(
        mSelectedImage: Bitmap, frame: Mat, ageGenderModelInitialization: Pair<Net, Net>,
        model: ByteBuffer
    ) {
        isProcessing.value = true
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()


        val image = InputImage.fromBitmap(mSelectedImage, 0)
        val detector = FaceDetection.getClient(highAccuracyOpts)
        detector.process(image)
            .addOnSuccessListener { faces ->
                processFaceContourDetectionResult(frame, faces, ageGenderModelInitialization, model)
            }
            .addOnFailureListener { e ->
                e.message?.let { Log.d(TAG, "runFaceContourDetection: ${it}") }
            }
    }


    private fun processFaceContourDetectionResult(
        frame: Mat,
        faces: List<Face>,
        ageGenderModelInitialization: Pair<Net, Net>,
        model: ByteBuffer
    ) {
        if (faces.isEmpty()) {
            Log.d(TAG, "processFaceContourDetectionResult: No face found")
            return
        }
        val (ageNet, genderNet) = ageGenderModelInitialization
        val infoList: MutableList<InfoExtracted> = mutableListOf()

        var age: String
        var gender: String
        var emotion: String
        boundingBoxes.clear()
        for (face in faces) {

            //age,gender,emotion
            val bounds = face.boundingBox
            boundingBoxes.add(bounds)
            val left = bounds.left
            val top = bounds.top
            val right = bounds.right
            val bottom = bounds.bottom


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


            val detectedGender = processDNN(genderNet, faceBlob, genderList)
            gender = detectedGender.first
            val genderConfidence = detectedGender.second


            val detectedAge = processDNN(ageNet, faceBlob, ageBucketList)
            age = detectedAge.first
            val ageConfidence = detectedAge.second

            val detectedEmotion = detectEmotion(model, croppedFace)
            emotion = detectedEmotion.first
            val emotionConfidence = detectedEmotion.second

            infoList.add(InfoExtracted(bounds, age, gender, emotion))

        }
        infoRegardingFaces.value = infoList
        isProcessing.value = false

    }

    fun anonymizeFaceSimple(frame: Mat, position: Int, factor: Double = 3.0) {
        isProcessing.value = true
        val rect = boundingBoxes.get(position)
        val (h, w) = Pair(rect.height(), rect.width())
        var kW = (w / factor).roundToInt()
        var kH = (h / factor).roundToInt()
        if (kW % 2 == 0) {
            kW -= 1
        }
        if (kH % 2 == 0) {
            kH -= 1
        }



        val left = rect.left
        val top = rect.top
        val right = rect.right
        val bottom = rect.bottom

        val roi = Rect(
            Point(left.toDouble(), top.toDouble()),
            Point(right.toDouble(), bottom.toDouble())
        )



        Imgproc.GaussianBlur(
            Mat(frame, roi),
            Mat(frame, roi),
            Size(kW.toDouble(), kH.toDouble()),
            0.0
        )
        isProcessing.value = false

    }


    private fun processing(bitmap: Bitmap): Pair<String, Float> {
        Log.d(
            MainActivity.TAG,
            "classify: bitmap $bitmap inputImageWidth ${inputImageWidth} inputImageHeight ${inputImageHeight}"
        )

        val resizedImage = Bitmap.createScaledBitmap(
            bitmap,
            inputImageWidth,
            inputImageHeight,
            true
        )
        val byteBuffer = convertBitmapToByteBuffer(resizedImage)

        val output = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT) }

        interpreter?.run(byteBuffer, output)

        val result = output[0]
        val maxIndex = result.indices.maxBy { result[it] } ?: -1
        return Pair(emotionList[maxIndex], result[maxIndex])

    }


    private fun initializeInterpreter(model: ByteBuffer) {
        val interpreter = Interpreter(model)
        val inputShape = interpreter.getInputTensor(0).shape()
        Log.d(TAG, "initializeInterpreter: inputShape ${inputShape.size}")
        inputShape.forEach { Log.d(TAG, "initializeInterpreter: inputShape ${it}") }
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE

        this.interpreter = interpreter

        isInitialized = true
        Log.d(TAG, "Initialized TFLite interpreter.")
    }


    private fun detectEmotion(model: ByteBuffer, croppedFace: Mat): Pair<String, Float> {
        initializeInterpreter(model)

        check(isInitialized) { "TF Lite Interpreter is not initialized yet." }


        val croppedFaceBitmap: Bitmap = Bitmap.createBitmap(
            croppedFace.width(),
            croppedFace.height(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(croppedFace, croppedFaceBitmap)
        return processing(croppedFaceBitmap)
    }


    fun close() {
        interpreter?.close()
        Log.d(TAG, "Closed TFLite interpreter.")
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

    companion object {
        const val TAG = "DigitClassifier"

        const val FLOAT_TYPE_SIZE = 4
        const val PIXEL_SIZE = 1

        private const val OUTPUT_CLASSES_COUNT = 6


        val ageBucketList = listOf(
            "(0-2)", "(4-6)", "(8-12)", "(15-20)", "(25-32)",
            "(38-43)", "(48-53)", "(60-100)"
        )
        val emotionList = listOf("Angry", "Fear", "Happy", "Neutral", "Sad", "Surprise")
        val genderList = listOf("M", "F")


    }
}

package com.gagan.agepredictor

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.hadilq.liveevent.LiveEvent
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

class Classifier : ViewModel() {


    val boundsList: MutableList<android.graphics.Rect> = mutableListOf()


    val faceDetected = SingleLiveEvent<MutableList<android.graphics.Rect>>()
    val gender = SingleLiveEvent<MutableList<Pair<Char, Double>>>()
    val age = SingleLiveEvent<MutableList<Pair<String, Double>>>()
    val emotion = SingleLiveEvent<MutableList<Pair<String, Float>>>()


//    val faceDetectionProcessing = MutableLiveData<Boolean>(false)


    private val faceList: MutableList<Mat> = mutableListOf()
    private val detectedGenderList: MutableList<Pair<Char, Double>> = mutableListOf()
    private val detectedAgeList: MutableList<Pair<String, Double>> = mutableListOf()
    private val detectedEmotionList: MutableList<Pair<String, Float>> = mutableListOf()
    private var interpreter: Interpreter? = null
    var isInitialized = false
        private set

    private var inputImageWidth: Int = 0 // will be inferred from TF Lite model.
    private var inputImageHeight: Int = 0 // will be inferred from TF Lite model.
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model.


    fun runFaceContourDetection(mSelectedImage: Bitmap, frame: Mat) {
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()


        val image = InputImage.fromBitmap(mSelectedImage, 0)
        val detector = FaceDetection.getClient(highAccuracyOpts)
        detector.process(image)
            .addOnSuccessListener { faces ->
                processFaceContourDetectionResult(frame, faces)
            }
            .addOnFailureListener { e ->
                e.message?.let { Log.d(TAG, "runFaceContourDetection: ${it}") }
            }
    }

    fun processFaceContourDetectionResult(frame: Mat, faces: List<Face>) {
        if (faces.isEmpty()) {
            Log.d(TAG, "processFaceContourDetectionResult: No face found")
            return
        }

        boundsList.clear()

        faceList.clear()
        for (face in faces) {
            val bounds = face.boundingBox
            boundsList.add(bounds)
            val left = bounds.left
            val top = bounds.top
            val right = bounds.right
            val bottom = bounds.bottom


            val rectCrop = Rect(
                Point(left.toDouble(), top.toDouble()),
                Point(right.toDouble(), bottom.toDouble())
            )
            val croppedFace = Mat(frame, rectCrop)
            faceList.add(croppedFace)


        }
        faceDetected.value = boundsList

    }


    fun detectAgeGender(ageGenderModelInitilaziton: Pair<Net, Net>) {
        val (ageNet, genderNet) = ageGenderModelInitilaziton
        Log.d(TAG, "detectAgeGender: faceList.size ${faceList.size}")
        detectedGenderList.clear()
        detectedAgeList.clear()
        for (croppedFace in faceList) {
            Log.d(
                TAG,
                "detectAgeGender: croppedFace ${croppedFace.width()} ${croppedFace.height()}"
            )
            val faceBlob = Dnn.blobFromImage(
                croppedFace, 1.0,
                Size(227.0, 227.0),
                Scalar(78.4263377603, 87.7689143744, 114.895847746), false
            )
            Log.d(
                TAG,
                "detectAgeGender: faceBlob ${faceBlob.width()} ${faceBlob.height()} ${faceBlob.size()}"
            )
            genderNet.setInput(faceBlob)
            Log.d(TAG, "detectAgeGender: $ageNet $genderNet")
            Log.d(TAG, "detectAgeGender: ${ageNet.layerNames} ${genderNet.layerNames}")
            Log.d(TAG, "detectAgeGender: ${ageNet.empty()} ${genderNet.empty()}")
//            OpenCV(3.4.3) Error: Assertion failed (inputs[0]->size[1] % blobs[0].size[1] == 0)
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
            Log.d(MainActivity.TAG, "onCameraViewStarted: genderConf ${genderConf}")


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


            val age = ageBucketList[index]
            val ageConfidence = max

            Log.d(MainActivity.TAG, "onCameraViewStarted: ageConfidence ${ageConfidence}")
            detectedAgeList.add(Pair(age, ageConfidence))
            detectedGenderList.add(Pair(gender, genderConf))
        }
        Log.d(TAG, "detectAgeGender: detectedGenderList.size ${detectedGenderList.size}")
        gender.value = detectedGenderList
        Log.d(TAG, "detectAgeGender: detectedGenderList.size ${detectedAgeList.size}")
        age.value = detectedAgeList

    }

    fun processing(bitmap: Bitmap): Pair<String, Float> {
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


    fun initializeInterpreter(model: ByteBuffer) {


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


    fun detectEmotion(model: ByteBuffer) {
        initializeInterpreter(model)
        detectedEmotionList.clear()
        check(isInitialized) { "TF Lite Interpreter is not initialized yet." }
        for (croppedFace in faceList) {


            val croppedFaceBitmap: Bitmap = Bitmap.createBitmap(
                croppedFace.width(),
                croppedFace.height(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(croppedFace, croppedFaceBitmap)
            val emotionResult = processing(croppedFaceBitmap)
            detectedEmotionList.add(emotionResult)
        }

        emotion.value = detectedEmotionList
    }


//    fun classifyAsync(bitmap: Bitmap): Task<String> {
//        val task = TaskCompletionSource<String>()
//        executorService.execute {
//            val result = classify(bitmap)
//            task.setResult(result)
//        }
//        return task.task
//    }

    fun close() {

        interpreter?.close()
        Log.d(TAG, "Closed TFLite interpreter.")

    }

    fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {

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
        val genderList = listOf('M', 'F')


    }
}

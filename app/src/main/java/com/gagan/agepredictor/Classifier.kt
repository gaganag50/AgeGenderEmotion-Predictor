package com.gagan.agepredictor

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.gagan.agepredictor.AgePredictionApplication.Companion.TAG
import com.gagan.agepredictor.appdata.InfoExtracted
import com.gagan.agepredictor.ml.WhiteboxCartoonGanDr
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.*
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class Classifier {


    val infoRegardingFaces = LiveEvent<List<InfoExtracted>>()
    val isProcessing = LiveEvent<Boolean>()
    private val boundingBoxes: MutableList<android.graphics.Rect> = mutableListOf()
    private var interpreterEmotion: Interpreter? = null
    private var interpreterAge: Interpreter? = null
    private var interpreterGender: Interpreter? = null

    private var isEmotionInitialized = false
    private var isAgeInitialized = false
    private var isGenderInitialized = false

    private var inputEmotionImageWidth: Int = 0
    private var inputEmotionImageHeight: Int = 0
    private var modelEmotionInputSize: Int = 0


    private var inputAgeGenderImageWidth: Int = 0
    private var inputAgeGenderImageHeight: Int = 0
    private var modelAgeGenderInputSize: Int = 0


    fun runFaceContourDetection(
        mSelectedImage: Bitmap, frame: Mat, ageModelInitialization: ByteBuffer,
        genderModelInitialization: ByteBuffer,
        emotionModelInitialization: ByteBuffer

    ) {
        isProcessing.postValue(true)
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()


        val image = InputImage.fromBitmap(mSelectedImage, 0)
        val detector = FaceDetection.getClient(highAccuracyOpts)
        detector.process(image)
            .addOnSuccessListener { faces ->
                processFaceContourDetectionResult(
                    frame,
                    faces,
                    ageModelInitialization,
                    genderModelInitialization,
                    emotionModelInitialization
                )
            }
            .addOnFailureListener { e ->
                e.message?.let {
                    Log.d(TAG, "runFaceContourDetection: $it")
                }
            }
    }


    private fun processFaceContourDetectionResult(
        frame: Mat,
        faces: List<Face>,
        ageModelInitialization: ByteBuffer,
        genderModelInitialization: ByteBuffer,
        model: ByteBuffer
    ) {
        val infoList: MutableList<InfoExtracted> = mutableListOf()
        if (faces.isNotEmpty()) {


            var age: String
            var gender: String
            var emotion: String
            boundingBoxes.clear()
            for (face in faces) {
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
                val detectedAge = detectAge(ageModelInitialization, croppedFace)
                val detectedGender = detectGender(genderModelInitialization, croppedFace)
                age = detectedAge.toString()
                gender = detectedGender
                val detectedEmotion = detectEmotion(model, croppedFace)
                emotion = detectedEmotion.first
//            val emotionConfidence = detectedEmotion.second
                infoList.add(InfoExtracted(bounds, age, gender, emotion))
            }

        }
        infoRegardingFaces.value = infoList
        isProcessing.postValue(false)
    }

    suspend fun anonymizeFaceSimple(frame: Mat, position: Int, factor: Double = 3.0) {
        withContext(Dispatchers.IO) {
            isProcessing.postValue(true)
            val rect = boundingBoxes[position]
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
            isProcessing.postValue(false)


        }


    }


    private fun emotionProcessing(bitmap: Bitmap): Pair<String, Float> {
        val resizedImage = Bitmap.createScaledBitmap(
            bitmap,
            inputEmotionImageWidth,
            inputEmotionImageHeight,
            true
        )
        val byteBuffer = convertBitmapToByteBuffer(
            resizedImage,
            modelEmotionInputSize,
            inputEmotionImageWidth,
            inputEmotionImageHeight, true
        )
        val output = Array(1) { FloatArray(EMOTION_OUTPUT_CLASSES_COUNT) }
        interpreterEmotion?.run(byteBuffer, output)
        val result = output[0]
        val maxIndex = result.indices.maxByOrNull { result[it] } ?: -1
        return Pair(emotionList[maxIndex], result[maxIndex])
    }

    private fun ageProcessing(bitmap: Bitmap): Int {
        val resizedImage = Bitmap.createScaledBitmap(
            bitmap,
            this.inputAgeGenderImageWidth,
            inputAgeGenderImageHeight,
            true
        )
        val byteBuffer = convertBitmapToByteBuffer(
            resizedImage,
            modelAgeGenderInputSize,
            this.inputAgeGenderImageWidth,
            inputAgeGenderImageHeight, false
        )
        val output =
            Array(1) { FloatArray(AGE_GENDER_OUTPUT_CLASSES_COUNT) }
        interpreterAge?.run(byteBuffer, output)
        val result = output[0]
        val ages = FloatArray(101) { (it + 1).toFloat() }
        var age = 0f
        for (i in 0 until 101) {
            age += ages[i] * result[i]
        }
        return age.toInt()
    }

    private fun genderProcessing(bitmap: Bitmap): String {
        val resizedImage = Bitmap.createScaledBitmap(
            bitmap,
            this.inputAgeGenderImageWidth,
            inputAgeGenderImageHeight,
            true
        )
        val byteBuffer = convertBitmapToByteBuffer(
            resizedImage,
            modelAgeGenderInputSize,
            inputAgeGenderImageWidth,
            inputAgeGenderImageHeight, false
        )
        val output =
            Array(1) { FloatArray(2) }
        interpreterGender?.run(byteBuffer, output)
        val result = output[0]

        return if (result[0] < 0.5) {
            "M"
        } else {
            "F"
        }
    }


    private fun initializeEmotionInterpreter(model: ByteBuffer) {
        val interpreter = Interpreter(model)
        val inputShape = interpreter.getInputTensor(0).shape()
        inputEmotionImageWidth = inputShape[1]
        inputEmotionImageHeight = inputShape[2]
        modelEmotionInputSize =
            FLOAT_TYPE_SIZE * inputEmotionImageWidth * inputEmotionImageHeight * EMOTION_PIXEL_SIZE
        this.interpreterEmotion = interpreter
        isEmotionInitialized = true
    }

    private fun initializeAgeInterpreter(ageModelInitialization: ByteBuffer) {
        val interpreter = Interpreter(ageModelInitialization)
        val inputShape = interpreter.getInputTensor(0).shape()
        this.inputAgeGenderImageWidth = inputShape[1]
        inputAgeGenderImageHeight = inputShape[2]
        modelAgeGenderInputSize =
            FLOAT_TYPE_SIZE * this.inputAgeGenderImageWidth * inputAgeGenderImageHeight * AGE_GENDER_PIXEL_SIZE
        this.interpreterAge = interpreter
        isAgeInitialized = true
    }

    private fun initializeGenderInterpreter(genderModelInitialization: ByteBuffer) {
        val interpreter = Interpreter(genderModelInitialization)

        val inputShape = interpreter.getInputTensor(0).shape()

        this.inputAgeGenderImageWidth = inputShape[1]
        inputAgeGenderImageHeight = inputShape[2]
        modelAgeGenderInputSize =
            FLOAT_TYPE_SIZE * this.inputAgeGenderImageWidth * inputAgeGenderImageHeight * AGE_GENDER_PIXEL_SIZE
        this.interpreterGender = interpreter
        isGenderInitialized = true
    }


    private fun detectAge(
        ageModelInitialization: ByteBuffer,
        croppedFace: Mat
    ): Int {
        initializeAgeInterpreter(ageModelInitialization)
        check(isAgeInitialized) { "TF Lite Interpreter is not initialized yet." }
        val croppedFaceBitmap: Bitmap = Bitmap.createBitmap(
            croppedFace.width(),
            croppedFace.height(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(croppedFace, croppedFaceBitmap)
        return ageProcessing(croppedFaceBitmap)
    }

    private fun detectGender(
        genderModelInitialization: ByteBuffer,
        croppedFace: Mat
    ): String {
        initializeGenderInterpreter(genderModelInitialization)
        check(isGenderInitialized) { "TF Lite Interpreter is not initialized yet." }
        val croppedFaceBitmap: Bitmap = Bitmap.createBitmap(
            croppedFace.width(),
            croppedFace.height(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(croppedFace, croppedFaceBitmap)
        return genderProcessing(croppedFaceBitmap)
    }


    private fun detectEmotion(model: ByteBuffer, croppedFace: Mat): Pair<String, Float> {
        initializeEmotionInterpreter(model)
        check(isEmotionInitialized) { "TF Lite Interpreter is not initialized yet." }
        val croppedFaceBitmap: Bitmap = Bitmap.createBitmap(
            croppedFace.width(),
            croppedFace.height(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(croppedFace, croppedFaceBitmap)
        return emotionProcessing(croppedFaceBitmap)
    }


    fun close() {
        interpreterEmotion?.close()
    }

    private fun convertBitmapToByteBuffer(
        bitmap: Bitmap,
        modelInputSize: Int,
        inputImageWidth: Int,
        inputImageHeight: Int,
        isGray: Boolean
    ): ByteBuffer {


        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (pixelValue in pixels) {
            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)
            if (isGray) {
                val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
                byteBuffer.putFloat(normalizedPixelValue)
            } else {
                byteBuffer.putFloat(r.toFloat())
                byteBuffer.putFloat(g.toFloat())
                byteBuffer.putFloat(b.toFloat())
            }
        }

        return byteBuffer
    }

    private fun inferenceWithDrModel(
        sourceImage: TensorImage,
        model: WhiteboxCartoonGanDr
    ): TensorImage {
        val outputs = model.process(sourceImage)
        val cartoonizedImage = outputs.cartoonizedImageAsTensorImage
        model.close()
        return cartoonizedImage
    }


    private val coroutineScope = CoroutineScope(
        Dispatchers.Main
    )

    fun cartoonizeImage(
        bitmap: Bitmap,
        model: WhiteboxCartoonGanDr
    ): Deferred<Pair<Bitmap, Long>> =
        coroutineScope.async(Dispatchers.IO) {
            isProcessing.postValue(true)
            val startTime = SystemClock.uptimeMillis()
            val sourceImage = TensorImage.fromBitmap(bitmap)

            val cartoonizedImage: TensorImage = inferenceWithDrModel(sourceImage, model)
            val inferenceTime = SystemClock.uptimeMillis() - startTime
            val cartoonizedImageBitmap = cartoonizedImage.bitmap
            isProcessing.postValue(false)
            return@async Pair(cartoonizedImageBitmap, inferenceTime)
        }


    companion object {

        const val FLOAT_TYPE_SIZE = 4
        const val EMOTION_PIXEL_SIZE = 1
        const val AGE_GENDER_PIXEL_SIZE = 3

        private const val EMOTION_OUTPUT_CLASSES_COUNT = 6
        private const val AGE_GENDER_OUTPUT_CLASSES_COUNT = 101


        val emotionList = listOf("Angry", "Fear", "Happy", "Neutral", "Sad", "Surprise")


    }
}
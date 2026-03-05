package com.kidshield.kid_shield.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class FaceRecognitionEngine(private val context: Context) {

    companion object {
        private const val TAG = "FaceRecEngine"
        private const val MODEL_FILE = "mobilefacenet.tflite"
        private const val INPUT_SIZE = 112
        private const val EMBEDDING_SIZE = 192
        private const val SIMILARITY_THRESHOLD = 0.65f
    }

    private var interpreter: Interpreter? = null
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.1f)
        .build()
    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "MobileFaceNet model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load MobileFaceNet model", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Detect faces in a bitmap and return the bounding boxes.
     */
    fun detectFaces(bitmap: Bitmap, callback: (List<Rect>) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val rects = faces.map { it.boundingBox }
                callback(rects)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
                callback(emptyList())
            }
    }

    /**
     * Extract a 128-dimensional face embedding from a cropped face bitmap.
     */
    fun extractEmbedding(faceBitmap: Bitmap): FloatArray? {
        val interpreter = this.interpreter ?: run {
            Log.e(TAG, "Interpreter not initialized")
            return null
        }

        try {
            // Resize to 112x112
            val resized = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true)

            // Prepare input buffer: [1, 112, 112, 3] float32
            val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())

            val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
            resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

            for (pixel in pixels) {
                // Normalize to [-1, 1]
                val r = ((pixel shr 16 and 0xFF) / 127.5f) - 1.0f
                val g = ((pixel shr 8 and 0xFF) / 127.5f) - 1.0f
                val b = ((pixel and 0xFF) / 127.5f) - 1.0f
                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            }

            // Output: [1, 128]
            val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
            interpreter.run(inputBuffer, output)

            // L2 normalize the embedding
            val embedding = output[0]
            val norm = sqrt(embedding.map { it * it }.sum())
            if (norm > 0) {
                for (i in embedding.indices) {
                    embedding[i] /= norm
                }
            }

            return embedding
        } catch (e: Exception) {
            Log.e(TAG, "Embedding extraction failed", e)
            return null
        }
    }

    /**
     * Detect face in bitmap, crop it, and extract embedding. Returns null if no face found.
     */
    fun processFrame(bitmap: Bitmap, callback: (FloatArray?) -> Unit) {
        detectFaces(bitmap) { rects ->
            if (rects.isEmpty()) {
                callback(null)
                return@detectFaces
            }

            val faceRect = rects[0] // Use the first (largest) face
            val safeBounds = Rect(
                faceRect.left.coerceAtLeast(0),
                faceRect.top.coerceAtLeast(0),
                faceRect.right.coerceAtMost(bitmap.width),
                faceRect.bottom.coerceAtMost(bitmap.height)
            )

            if (safeBounds.width() <= 0 || safeBounds.height() <= 0) {
                callback(null)
                return@detectFaces
            }

            val faceCrop = Bitmap.createBitmap(
                bitmap,
                safeBounds.left,
                safeBounds.top,
                safeBounds.width(),
                safeBounds.height()
            )

            val embedding = extractEmbedding(faceCrop)
            callback(embedding)
        }
    }

    /**
     * Compare two face embeddings using cosine similarity.
     * Returns a value between -1 and 1, where > threshold means same person.
     */
    fun compareFaces(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != EMBEDDING_SIZE || embedding2.size != EMBEDDING_SIZE) return -1f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in 0 until EMBEDDING_SIZE) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }

        val denominator = sqrt(norm1) * sqrt(norm2)
        return if (denominator > 0) dotProduct / denominator else -1f
    }

    /**
     * Check if an embedding matches any of the registered parent embeddings.
     * Returns the index of the matched parent, or -1 if no match.
     */
    fun matchAgainstRegistered(
        embedding: FloatArray,
        registeredEmbeddings: List<FloatArray>
    ): Pair<Int, Float> {
        var bestIndex = -1
        var bestSimilarity = -1f

        for ((index, registered) in registeredEmbeddings.withIndex()) {
            val similarity = compareFaces(embedding, registered)
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestIndex = index
            }
        }

        return if (bestSimilarity >= SIMILARITY_THRESHOLD) {
            Pair(bestIndex, bestSimilarity)
        } else {
            Pair(-1, bestSimilarity)
        }
    }

    fun getThreshold(): Float = SIMILARITY_THRESHOLD

    fun close() {
        interpreter?.close()
        faceDetector.close()
    }
}

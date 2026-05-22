package com.example.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.Exception
import kotlin.coroutines.resume

class OcrManager(private val context: Context) {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun loadBitmapFromUrl(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Use standard Coil singleton imageLoader instead of creating new instances to conserve memory and threads
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // ML Kit OCR requires a software-decodable bitmap
                .build()
            
            when (val result = loader.execute(request)) {
                is SuccessResult -> {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        drawable.bitmap
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Preprocesses bitmap by converting to grayscale and increasing contrast.
     * This greatly increases accuracy of text recognitions in manga pages.
     */
    fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpGrayscale)
        val paint = Paint()
        
        // 1. Convert to grayscale (saturation = 0)
        val grayscaleMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }
        
        // 2. Increase contrast and slightly tweak brightness
        val contrastFactor = 1.4f
        val brightnessOffset = -15f
        val contrastMatrix = floatArrayOf(
            contrastFactor, 0f, 0f, 0f, brightnessOffset,
            0f, contrastFactor, 0f, 0f, brightnessOffset,
            0f, 0f, contrastFactor, 0f, brightnessOffset,
            0f, 0f, 0f, 1f, 0f
        )
        
        val finalMatrix = ColorMatrix().apply {
            postConcat(grayscaleMatrix)
            postConcat(ColorMatrix(contrastMatrix))
        }
        
        paint.colorFilter = ColorMatrixColorFilter(finalMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return bmpGrayscale
    }

    /**
     * Cleans OCR-ed English text by trimming spaces, eliminating weird layouts noise,
     * and merging hyphenated lines into a natural continuous flows.
     */
    fun cleanExtractedText(text: String): String {
        if (text.isBlank()) return ""
        return text.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ") { line ->
                if (line.endsWith("-")) {
                    line.dropLast(1)
                } else {
                    line + " "
                }
            }
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            try {
                // Apply visual enhancement preprocessing
                val preprocessed = preprocessBitmap(bitmap)
                val image = InputImage.fromBitmap(preprocessed, 0)
                
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val cleanText = cleanExtractedText(visionText.text)
                        continuation.resume(cleanText)
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        // Fallback to original bitmap if preprocessing fails
                        try {
                            val originalImage = InputImage.fromBitmap(bitmap, 0)
                            recognizer.process(originalImage)
                                .addOnSuccessListener { fallbackText ->
                                    continuation.resume(cleanExtractedText(fallbackText.text))
                                }
                                .addOnFailureListener { fallbackEx ->
                                    fallbackEx.printStackTrace()
                                    continuation.resume("")
                                }
                        } catch (ex: Exception) {
                            continuation.resume("")
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume("")
            }
        }
    }

    fun getDemoTextForPage(url: String): String? {
        return when {
            url.contains("photo-1607604276583") -> "Saitama is walking down the street thinking. 'The grocery store discount starts at exactly 6 PM, but it is already 5:45! I must hurry!'"
            url.contains("photo-1541562232579") -> "He sees Genos blocking his path. 'Master, I have scanned all supermarket flyers. The best deal on cabbages is indeed at the East Wing Store!'"
            url.contains("photo-1578632767115") -> "'Genos! Move aside, there is no time to waste on cabbages, the beef half-price clearance is what truly matters!'"
            url.contains("photo-1509198397868") -> "Genos looks determined. 'I will activate incinerate mode to cook the beef directly at the supermarket aisle for you, Master!'"
            url.contains("photo-1520333789090") -> "Saitama screams: 'No, Genos! The thrill of active bargaining in the grocery store is the essence of my daily life!'"
            else -> null
        }
    }
}

package com.example.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
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
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun loadBitmapFromUrl(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
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

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val cleanText = visionText.textBlocks.joinToString("\n") { block ->
                            block.text.replace("\n", " ")
                        }
                        continuation.resume(cleanText)
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        continuation.resume("") // Return empty on failure instead of crashing
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

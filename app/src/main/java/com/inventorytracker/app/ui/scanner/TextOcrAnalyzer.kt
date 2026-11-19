package com.inventorytracker.app.ui.scanner

import android.os.SystemClock
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalGetImage::class)
class TextOcrAnalyzer(
    private val onText: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )
    private val busy = AtomicBoolean(false)
    private var lastRun = 0L

    override fun analyze(proxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRun < 650 ||
            !busy.compareAndSet(false, true)) {
            proxy.close()
            return
        }
        lastRun = now
        val image = proxy.image
        if (image == null) {
            busy.set(false)
            proxy.close()
            return
        }
        try {
            recognizer.process(
                InputImage.fromMediaImage(
                    image, proxy.imageInfo.rotationDegrees
                )
            ).addOnSuccessListener {
                val value = it.text.trim()
                if (value.isNotEmpty()) onText(value)
            }.addOnCompleteListener {
                proxy.close()
                busy.set(false)
            }
        } catch (_: Exception) {
            proxy.close()
            busy.set(false)
        }
    }

    fun close() = recognizer.close()
}

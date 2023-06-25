package com.elfefe.notescanner.controller

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalZeroShutterLag::class)
suspend fun Context. createVideoCaptureUseCase(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
): ImageCapture {
    val preview = Preview.Builder()
        .build()
        .apply { setSurfaceProvider(previewView.surfaceProvider) }

    val imageCapture = ImageCapture.Builder()
        .setTargetRotation(previewView.display.rotation)
        .setCaptureMode(CAPTURE_MODE_ZERO_SHUTTER_LAG)
        .setResolutionSelector(ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(ResolutionStrategy(
                Size(1920, 1080),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
            ))
            .build())
        .build()

    val cameraProvider = getCameraProvider()
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        preview,
        imageCapture
    )

    return imageCapture
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener(
            { continuation.resume(future.get()) },
            executorMain
        )
    }
}

fun Bitmap.rotate(angle: Int): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle.toFloat())
    return Bitmap.createBitmap(
        this, 0, 0, width, height, matrix,
        true
    )
}

fun Context.capture(configuration: Configuration, recognizer: TextRecognizer, onCaptured: (File?) -> Unit, onRead: (List<String>) -> Unit) = object : ImageCapture.OnImageCapturedCallback() {
    override fun onCaptureSuccess(imageProxy: ImageProxy) {
        super.onCaptureSuccess(imageProxy)

        var file: File?
        val rotation = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 90 else 0

        imageProxy.toBitmap().rotate(rotation).run {
            file = File(
                    filesDir,
            "${
                LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd-MM-yyyy_AAAAAAAA")
                )}.png"
            )
            if (!compress(
                Bitmap.CompressFormat.PNG, 100,
                FileOutputStream(file)
            )) file = null

            recognizer.process(InputImage.fromBitmap(this, rotation))
                .addOnSuccessListener {
                    onRead(it.textBlocks.map { it.text })
                }
                .addOnFailureListener {
                    println("Failed")
                }
        }
        imageProxy.close()

        onCaptured(file)
    }

    override fun onError(exception: ImageCaptureException) {
        super.onError(exception)

        onCaptured(null)
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
fun Context.analysis(configuration: Configuration, recognizer: TextRecognizer, onRead: (List<String>) -> Unit) =
    ImageAnalysis.Analyzer { imageProxy ->
        imageProxy.image?.let { image ->
            val rotation =
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 90
                else 0
            recognizer.process(InputImage.fromMediaImage(image, rotation))
                .addOnSuccessListener {
                    onRead(it.textBlocks.map { it.text })
                }
                .addOnFailureListener {
                    println("Failed")
                }
        }
        imageProxy.close()
    }
package com.elfefe.notescanner.ui.view

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Space
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.elfefe.notescanner.presenter.MainActivity
import com.elfefe.notescanner.R
import com.elfefe.notescanner.controller.capture
import com.elfefe.notescanner.controller.createVideoCaptureUseCase
import com.elfefe.notescanner.controller.extractData
import com.elfefe.notescanner.presenter.Application
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.ktor.util.InternalAPI
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import java.util.concurrent.Executors


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainActivity.Main() {
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
        )
    )

    LaunchedEffect(Unit) {
        while (!permissionState.allPermissionsGranted)
            permissionState.launchMultiplePermissionRequest()
    }

    AnimatedVisibility(
        visible = permissionState.allPermissionsGranted,
        modifier = Modifier.fillMaxSize()
    ) {
        Greeting()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainActivity.Greeting() {
    val pagerState = rememberPagerState { 2 }

    HorizontalPager(state = pagerState) {
        if (it == 0) Camera()
        else Cards()
    }
}

@Composable
fun MainActivity.Camera() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()

    val previewView: PreviewView = remember { PreviewView(context) }
    var imageCapture: ImageCapture? = null

    var isCapturing by remember { mutableStateOf(false) }
    var capturedImage: Bitmap? by remember { mutableStateOf(null) }

    var capturingFlash by remember { mutableFloatStateOf(0f) }

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    LaunchedEffect(previewView) {
        val capture = context.createVideoCaptureUseCase(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView
        )
        imageCapture = capture
    }

    LaunchedEffect(capturedImage) {
        if (capturedImage != null)
            animate(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = tween(1000, 0, FastOutLinearInEasing)
            ) { value, _ ->
                capturingFlash = value
            }
    }

    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (camera, captureButton, outputText) = createRefs()
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .drawWithContent {
                    drawContent()
                }
                .constrainAs(camera) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(1f, 1f, 1f, capturingFlash))
        ) {}

        IconButton(
            onClick = {
                isCapturing = true
                imageCapture?.takePicture(
                    Executors.newSingleThreadExecutor(),
                    context.capture(configuration, recognizer, {
                        isCapturing = false
                        it?.let { file ->
                            extractData(scope, file) {
                                try {
                                    addExtractPics(
                                        file.name,
                                        Gson().fromJson(
                                            it,
                                            object : TypeToken<List<String>>() {}.type
                                        )
                                    )
                                } catch (e: Exception) {
                                    println(it)
                                    println(e)
                                }
                            }
                            capturedImage = BitmapFactory.decodeFile(file.absolutePath)
                        }
                    }, {})
                )
            },
            enabled = !isCapturing,
            modifier = Modifier
                .background(
                    color = Color.Black,
                    shape = CircleShape
                )
                .constrainAs(captureButton) {
                    bottom.linkTo(parent.bottom, 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_camera_24),
                contentDescription = null,
                tint = if (isCapturing) Color.Magenta else Color.White
            )
        }
    }
}

@Composable
fun MainActivity.Cards() {
    var extractedPics by remember { mutableStateOf(listOf<Pair<String, List<String>>>()) }

    notifiedExtractPics { map ->
        println(map)
        extractedPics = map.map { Pair(it.key, it.value) }
    }

    LazyColumn(
        modifier = Modifier
            .background(Color.LightGray)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(extractedPics) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(.9f)
                    .height(256.dp),
                colors = CardDefaults.cardColors(Color.White, Color.Black),
                elevation = CardDefaults.cardElevation(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.Transparent)
                        .fillMaxSize(),
                     verticalAlignment = Alignment.CenterVertically
                ) {
                    println(it)
                    Image(
                        bitmap = BitmapFactory.decodeFile(
                            File(Application.instance.filesDir, it.first).absolutePath
                        ).asImageBitmap(),
                        modifier = Modifier
                            .fillMaxWidth(.4f),
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(it.second) { text ->
                            Text(text = text)
                        }
                    }
                }
            }
        }
    }
}
package com.elfefe.notescanner.ui.composable

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.elfefe.notescanner.R
import com.elfefe.notescanner.controller.applicationScope
import com.elfefe.notescanner.controller.capture
import com.elfefe.notescanner.controller.createVideoCaptureUseCase
import com.elfefe.notescanner.controller.extractData
import com.elfefe.notescanner.controller.getImage
import com.elfefe.notescanner.controller.ocrDatabase
import com.elfefe.notescanner.model.OcrData
import com.elfefe.notescanner.presenter.MainActivity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        val (camera, captureButton) = createRefs()
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
                            val name = file.name.removeSuffix(".png")
                            updateDatabase(name, "[]")
                            extractData(applicationScope, file) { text ->
                                println(text)
                                updateDatabase(name, text)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainActivity.Cards() {
    val scope = rememberCoroutineScope()

    val extractedPics = remember { mutableStateListOf(*ocrDatabase.toTypedArray()) }

    val clipBoard = LocalClipboardManager.current

    notifiedExtractPics = {
        extractedPics.clear()
        extractedPics.addAll(ocrDatabase)
    }

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(extractedPics) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
                    .animateItemPlacement(animationSpec = tween(100, 0, FastOutLinearInEasing)),
                colors = CardDefaults.elevatedCardColors(
                    MaterialTheme.colorScheme.onBackground,
                    MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(8.dp),
            ) {
                println(it)
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.Transparent)
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = BitmapFactory.decodeFile(
                            getImage(it.imageName).absolutePath
                        ).asImageBitmap(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .fillMaxHeight()
                            .wrapContentWidth(),
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable {
                                    if (it.texts.isNotEmpty()) {
                                        clipBoard.setText(
                                            AnnotatedString(it.texts.joinToString("\n"))
                                        )
                                        Toast
                                            .makeText(
                                                application,
                                                "Copied to clipboard",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                },
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (it.texts.isEmpty()) item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(64.dp)
                                    )
                                }
                            }
                            else items(it.texts) { text ->
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    updateDatabase(it.name, "[]")
                                    extractData(applicationScope, getImage(it.imageName)) { text ->
                                        updateDatabase(it.name, text)
                                    }
                                }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface
                                )
                            }

                            IconButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        it.delete()
                                        notifiedExtractPics()
                                    }
                                }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }

                            IconButton(
                                onClick = {

                                }) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}